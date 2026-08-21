"""
Rolling-origin (multi-horizon) evaluation for export-destination ranking.
=========================================================================
Extends the single-holdout v4 evaluation to four successive test origins so
that the reported improvement can be shown to hold across time rather than in
one year, and so that significance tests run on 4x the paired sample.

Design (leakage-critical):
  For each test origin T, hyperparameters are re-tuned from scratch using ONLY
  expanding-window folds whose validation years are strictly < T. Reusing the
  v4 hyperparameters for every origin would leak information from later years
  into earlier tests, so each origin gets its own search.

    origin T=2020: CV val {2019}                  -> final train 2017-2019, test 2020
    origin T=2021: CV val {2019,2020}             -> final train 2017-2020, test 2021
    origin T=2022: CV val {2019,2020,2021}        -> final train 2017-2021, test 2022
    origin T=2023: CV val {2019,2020,2021,2022}   -> final train 2017-2022, test 2023

  "Test origin T" means: features observed in year T, target = realized exports
  in T+1. Origin 2023 uses the SAME split as the v4 single holdout, but its
  hyperparameter search is re-run independently with a smaller budget, so only
  the fixed-hyperparameter models (RF, LightGBM, GradientBoosting, Ridge, naive,
  heuristic) reproduce v4 exactly; the tuned models (ranker, XGB regressor,
  hurdle) land close but not identical. That partial reproduction is itself the
  determinism check.

Metric helpers are copied VERBATIM from train_models_v4.py so that origin-2023
numbers are directly comparable to the published v4 table.

Run from the 05(data) directory:  python src/rolling_origin_eval.py
"""

import os
import json
import random
import warnings
import numpy as np
import pandas as pd
import scipy.stats as stats
from scipy.stats import spearmanr, kendalltau, wilcoxon, binomtest
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.linear_model import Ridge
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
import xgboost as xgb
import lightgbm as lgb

warnings.filterwarnings("ignore")

GLOBAL_SEED = 42
SEEDS = [42, 101, 2024, 777, 999]
os.environ["PYTHONHASHSEED"] = str(GLOBAL_SEED)
random.seed(GLOBAL_SEED)
np.random.seed(GLOBAL_SEED)

DATA_PATH = os.path.join("filtered_for_model", "master_cbec_train_dataset.csv")
OUT = "outputs"
os.makedirs(OUT, exist_ok=True)
TARGET = "target_log_export_value"
RAW_TARGET = "target_future_export_value_usd"

ORIGINS = [2020, 2021, 2022, 2023]
FIRST_VAL_YEAR = 2019

# search budget per origin (kept modest: 4 origins x 3 model families)
N_SEARCH_REG = 24
N_SEARCH_RANK = 12

# ---------------------------------------------------------------------------
# Metric helpers -- VERBATIM from train_models_v4.py
# ---------------------------------------------------------------------------

def ndcg_at_k(actual, pred, k=5):
    actual = np.asarray(actual); pred = np.asarray(pred)
    if len(actual) == 0:
        return 0.0
    k = min(k, len(actual))
    order_pred = np.argsort(pred)[::-1]
    order_ideal = np.argsort(actual)[::-1]
    gains_pred = np.maximum(0, actual[order_pred][:k])
    gains_ideal = np.maximum(0, actual[order_ideal][:k])
    disc = np.log2(np.arange(k) + 2)
    dcg = np.sum(gains_pred / disc)
    idcg = np.sum(gains_ideal / disc)
    if idcg == 0:
        return 1.0 if dcg == 0 else 0.0
    return float(dcg / idcg)


def top_k_overlap(actual, pred, k=3):
    actual = np.asarray(actual); pred = np.asarray(pred)
    k = min(k, len(actual))
    if k == 0:
        return 0.0
    ta = set(np.argsort(actual)[::-1][:k])
    tp = set(np.argsort(pred)[::-1][:k])
    return len(ta & tp) / float(k)


def top_1_hit(actual, pred):
    actual = np.asarray(actual); pred = np.asarray(pred)
    if len(actual) == 0:
        return 0.0
    return 1.0 if np.argmax(actual) == np.argmax(pred) else 0.0


def evaluate_rankings(df_subset, pred_col, target_col=TARGET):
    recs = []
    for hs, g in df_subset.groupby("hs6"):
        if len(g) < 2:
            continue
        yt = g[target_col].values
        yp = g[pred_col].values
        if np.std(yt) > 1e-6 and np.std(yp) > 1e-6:
            rho, _ = spearmanr(yt, yp)
            tau, _ = kendalltau(yt, yp)
            rho = 0.0 if np.isnan(rho) else rho
            tau = 0.0 if np.isnan(tau) else tau
        else:
            rho, tau = 0.0, 0.0
        recs.append({
            "hs6": hs, "spearman_rho": rho, "kendall_tau": tau,
            "ndcg@5": ndcg_at_k(yt, yp, 5), "ndcg@10": ndcg_at_k(yt, yp, 10),
            "top1_hit": top_1_hit(yt, yp), "top3_overlap": top_k_overlap(yt, yp, 3),
        })
    dfp = pd.DataFrame(recs)
    summary = {
        "spearman_rho": float(dfp["spearman_rho"].mean()),
        "kendall_tau": float(dfp["kendall_tau"].mean()),
        "top3_overlap": float(dfp["top3_overlap"].mean()),
        "top1_hit_rate": float(dfp["top1_hit"].mean()),
        "ndcg@5": float(dfp["ndcg@5"].mean()),
        "ndcg@10": float(dfp["ndcg@10"].mean()),
    }
    return summary, dfp


def mcnemar_p(hits_base, hits_model):
    hb = np.asarray(hits_base); hm = np.asarray(hits_model)
    b = int(np.sum((hb == 1) & (hm == 0)))
    c = int(np.sum((hb == 0) & (hm == 1)))
    if b + c == 0:
        return 1.0
    return float(binomtest(min(b, c), b + c, 0.5, alternative="two-sided").pvalue)


def wilcoxon_p(diff):
    diff = np.asarray(diff, dtype=float)
    nz = diff[diff != 0]
    if len(nz) < 5:
        return 1.0
    try:
        _, p = wilcoxon(nz, alternative="two-sided")
        return float(p)
    except Exception:
        return 1.0


# ---------------------------------------------------------------------------
# Load data, rebuild the exact v4 feature set
# ---------------------------------------------------------------------------
print("=" * 78)
print("ROLLING-ORIGIN EVALUATION")
print("=" * 78)

df = pd.read_csv(DATA_PATH)
for col in ["hs2", "hs4", "hs6", "product_category", "destination_iso3"]:
    df[f"{col}_enc"] = df[col].astype("category").cat.codes
df["feature_year_num"] = df["year"] - 2017

group_a = ["hs2_enc", "hs4_enc", "hs6_enc", "product_category_enc"]
group_b = ["destination_total_imports_usd", "destination_import_growth_1y",
           "destination_import_growth_3y", "num_supplying_countries",
           "hhi_concentration", "import_volatility", "top_competitor_share_pct"]
group_c = ["india_export_value_usd", "india_export_growth_1y", "india_export_growth_3y",
           "india_market_share_pct", "india_market_share_change_trailing",
           "india_export_rank_among_suppliers"]
group_d = ["fta_wto_agreement", "market_growth_x_fta"]
group_f = ["gdp_usd", "gdp_per_capita_usd", "population", "distance_km",
           "common_border", "common_language"]
group_eng = ["demand_per_capita", "india_export_intensity", "competition_intensity",
             "entry_cost_pct_gdpcap", "entry_procedures_count", "entry_time_days",
             "entry_metrics_imputed"]
group_time = ["feature_year_num"]

all_candidates = group_a + group_b + group_c + group_d + group_f + group_eng + group_time
DROP = {"entry_metrics_imputed", "feature_year_num", "gdp_per_capita_usd"}
FEATURES = [f for f in all_candidates if f not in DROP]
assert len(FEATURES) == 30, f"expected 30 features, got {len(FEATURES)}"

with open(os.path.join(OUT, "selected_features_v4.json")) as f:
    v4feats = json.load(f)["kept_features"]
assert set(FEATURES) == set(v4feats), "feature set diverged from v4"
print(f"features: {len(FEATURES)} (identical to v4)")

df_labeled = df[df[TARGET].notna()].copy()
print(f"labeled rows: {len(df_labeled)}  years {int(df_labeled.year.min())}-{int(df_labeled.year.max())}")

param_grid = {
    "max_depth": [3, 4, 5, 6],
    "learning_rate": [0.02, 0.04, 0.06, 0.1],
    "n_estimators": [200, 350, 500],
    "subsample": [0.7, 0.85, 1.0],
    "colsample_bytree": [0.7, 0.85, 1.0],
    "min_child_weight": [1, 3, 5],
    "reg_alpha": [0.0, 0.1, 0.5],
    "reg_lambda": [0.5, 1.0, 2.0],
}


def qid_sorted(frame):
    f = frame.sort_values(["year", "hs6"]).copy()
    grp = f.groupby(["year", "hs6"]).size().values
    rel = (f.groupby(["year", "hs6"])[TARGET].rank(method="first").astype(int) - 1).values
    return f, grp, rel


def mk_reg(p, seed):
    return xgb.XGBRegressor(
        objective="reg:squarederror", eval_metric="rmse", random_state=seed, n_jobs=-1,
        max_depth=int(p["max_depth"]), learning_rate=float(p["learning_rate"]),
        n_estimators=int(p["n_estimators"]), subsample=float(p["subsample"]),
        colsample_bytree=float(p["colsample_bytree"]),
        min_child_weight=int(p["min_child_weight"]),
        reg_alpha=float(p["reg_alpha"]), reg_lambda=float(p["reg_lambda"]))


def mk_rank(p, seed, objective):
    return xgb.XGBRanker(
        objective=objective, random_state=seed, n_jobs=-1,
        max_depth=int(p["max_depth"]), learning_rate=float(p["learning_rate"]),
        n_estimators=int(p["n_estimators"]), subsample=float(p["subsample"]),
        colsample_bytree=float(p["colsample_bytree"]),
        min_child_weight=int(p["min_child_weight"]),
        reg_alpha=float(p["reg_alpha"]), reg_lambda=float(p["reg_lambda"]))


MODEL_ORDER = [
    "XGBRanker (pairwise)", "Random Forest", "XGBoost Regressor",
    "Two-Stage Hurdle", "Naive persistence", "LightGBM",
    "Gradient Boosting", "Ridge Regression", "Heuristic weighted",
]
STOCHASTIC = {"XGBRanker (pairwise)", "Random Forest", "XGBoost Regressor",
              "LightGBM", "Gradient Boosting"}

per_origin_rows = []
per_product_rows = []
chosen_hparams = {}
cv_objective_cmp = []

for T in ORIGINS:
    val_years = list(range(FIRST_VAL_YEAR, T))
    train_years = list(range(2017, T))
    folds = [(list(range(2017, v)), v) for v in val_years]
    print("\n" + "-" * 78)
    print(f"ORIGIN {T}  (features {T} -> exports {T + 1})")
    print(f"  final train years : {train_years[0]}-{train_years[-1]}")
    print(f"  CV validation yrs : {val_years}  ({len(folds)} fold(s))")

    te = df_labeled[df_labeled["year"] == T].copy()
    tr = df_labeled[df_labeled["year"].isin(train_years)].copy()
    assert len(te) > 0 and len(tr) > 0
    print(f"  train rows {len(tr)}   test rows {len(te)}")

    # ---- per-origin hyperparameter search (folds strictly < T) ----
    def cv_reg(p):
        sc = []
        for tys, vy in folds:
            a = df_labeled[df_labeled["year"].isin(tys)]
            b = df_labeled[df_labeled["year"] == vy].copy()
            m = mk_reg(p, GLOBAL_SEED)
            m.fit(a[FEATURES].fillna(0.0), a[TARGET])
            b["p"] = m.predict(b[FEATURES].fillna(0.0))
            sc.append(evaluate_rankings(b, "p")[0]["spearman_rho"])
        return float(np.mean(sc))

    def cv_rank(p, obj):
        sc = []
        for tys, vy in folds:
            a = df_labeled[df_labeled["year"].isin(tys)]
            a_s, g, r = qid_sorted(a)
            b = df_labeled[df_labeled["year"] == vy].copy()
            m = mk_rank(p, GLOBAL_SEED, obj)
            m.fit(a_s[FEATURES].fillna(0.0), r, group=g)
            b["p"] = m.predict(b[FEATURES].fillna(0.0))
            sc.append(evaluate_rankings(b, "p")[0]["spearman_rho"])
        return float(np.mean(sc))

    rng = np.random.RandomState(GLOBAL_SEED + T)
    best_reg = {"cv": -np.inf, "params": None}
    for _ in range(N_SEARCH_REG):
        p = {k: rng.choice(v) for k, v in param_grid.items()}
        c = cv_reg(p)
        if c > best_reg["cv"]:
            best_reg = {"cv": c, "params": p}

    rng2 = np.random.RandomState(GLOBAL_SEED + T + 1)
    best_pw = {"cv": -np.inf, "params": None}
    best_nd = {"cv": -np.inf, "params": None}
    for _ in range(N_SEARCH_RANK):
        p = {k: rng2.choice(v) for k, v in param_grid.items()}
        c = cv_rank(p, "rank:pairwise")
        if c > best_pw["cv"]:
            best_pw = {"cv": c, "params": p}
    for _ in range(N_SEARCH_RANK):
        p = {k: rng2.choice(v) for k, v in param_grid.items()}
        c = cv_rank(p, "rank:ndcg")
        if c > best_nd["cv"]:
            best_nd = {"cv": c, "params": p}

    print(f"  CV rho  regressor={best_reg['cv']:.4f}  "
          f"pairwise={best_pw['cv']:.4f}  ndcg={best_nd['cv']:.4f}")
    cv_objective_cmp.append({"origin": T, "cv_pairwise": best_pw["cv"],
                             "cv_ndcg": best_nd["cv"], "cv_regressor": best_reg["cv"],
                             "pairwise_beats_ndcg": bool(best_pw["cv"] > best_nd["cv"])})
    chosen_hparams[str(T)] = {
        "n_cv_folds": len(folds), "cv_val_years": val_years,
        "regressor": {k: float(v) for k, v in best_reg["params"].items()},
        "ranker_pairwise": {k: float(v) for k, v in best_pw["params"].items()},
        "cv_rho": {"regressor": best_reg["cv"], "pairwise": best_pw["cv"],
                   "ndcg": best_nd["cv"]},
    }

    Xtr = tr[FEATURES].fillna(0.0); ytr = tr[TARGET]
    Xte = te[FEATURES].fillna(0.0)

    # ---- deterministic baselines ----
    nraw = te["india_export_value_usd"] * (1.0 + te["india_export_growth_1y"].clip(-0.9, 5.0))
    te["pred__Naive persistence"] = np.log1p(np.maximum(0, nraw))

    ne = te.groupby("hs6")["india_export_value_usd"].transform(
        lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
    ng = te.groupby("hs6")["destination_import_growth_1y"].transform(
        lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
    ns = te.groupby("hs6")["india_market_share_pct"].transform(
        lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
    nd = 1.0 - (te["distance_km"] / 12000.0)
    te["pred__Heuristic weighted"] = (0.40 * ne + 0.25 * ng + 0.20 * ns + 0.15 * nd) * 20.0

    m_ridge = Ridge(alpha=1.0, random_state=GLOBAL_SEED)
    m_ridge.fit(Xtr, ytr)
    te["pred__Ridge Regression"] = m_ridge.predict(Xte)

    # ---- stochastic models over seeds; also keep seed-0 preds for tables ----
    seed_rho = {m: [] for m in STOCHASTIC}
    seed_top1 = {m: [] for m in STOCHASTIC}
    for si, sd in enumerate(SEEDS):
        preds = {}

        m = mk_reg(best_reg["params"], sd); m.fit(Xtr, ytr)
        preds["XGBoost Regressor"] = m.predict(Xte)

        tr_s, g, r = qid_sorted(tr)
        m = mk_rank(best_pw["params"], sd, "rank:pairwise")
        m.fit(tr_s[FEATURES].fillna(0.0), r, group=g)
        preds["XGBRanker (pairwise)"] = m.predict(Xte)

        m = RandomForestRegressor(n_estimators=300, max_depth=8, min_samples_leaf=3,
                                  random_state=sd, n_jobs=-1)
        m.fit(Xtr, ytr)
        preds["Random Forest"] = m.predict(Xte)

        m = lgb.LGBMRegressor(objective="regression", n_estimators=400, learning_rate=0.05,
                              max_depth=6, num_leaves=31, min_child_samples=15,
                              subsample=0.8, colsample_bytree=0.8, random_state=sd,
                              deterministic=True, verbose=-1)
        m.fit(Xtr, ytr)
        preds["LightGBM"] = m.predict(Xte)

        m = GradientBoostingRegressor(n_estimators=200, learning_rate=0.05,
                                      max_depth=4, random_state=sd)
        m.fit(Xtr, ytr)
        preds["Gradient Boosting"] = m.predict(Xte)

        for name, pv in preds.items():
            te["_s"] = pv
            s, _ = evaluate_rankings(te, "_s")
            seed_rho[name].append(s["spearman_rho"])
            seed_top1[name].append(s["top1_hit_rate"])
            if si == 0:
                te[f"pred__{name}"] = pv
    te.drop(columns=["_s"], inplace=True, errors="ignore")

    # ---- hurdle (seed 0 only, matching v4) ----
    tr["is_nz"] = (tr[RAW_TARGET] > 0).astype(int)
    clf = xgb.XGBClassifier(objective="binary:logistic", eval_metric="logloss",
                            random_state=GLOBAL_SEED, n_jobs=-1, max_depth=4,
                            learning_rate=0.05, n_estimators=300, subsample=0.85,
                            colsample_bytree=0.85, min_child_weight=3)
    clf.fit(Xtr, tr["is_nz"])
    prob_nz = clf.predict_proba(Xte)[:, 1]
    nzm = (tr[RAW_TARGET] > 0).values
    m_s2 = mk_reg(best_reg["params"], GLOBAL_SEED)
    m_s2.fit(Xtr[nzm], tr.loc[nzm, TARGET])
    te["pred__Two-Stage Hurdle"] = np.log1p(
        prob_nz * np.maximum(0, np.expm1(m_s2.predict(Xte))))

    # ---- evaluate every model at this origin ----
    _, naive_prod = evaluate_rankings(te, "pred__Naive persistence")
    naive_prod = naive_prod.set_index("hs6")

    for name in MODEL_ORDER:
        col = f"pred__{name}"
        s, prod = evaluate_rankings(te, col)
        prod = prod.set_index("hs6")
        common = prod.index.intersection(naive_prod.index)
        d_rho = (prod.loc[common, "spearman_rho"] - naive_prod.loc[common, "spearman_rho"]).values

        if name == "Naive persistence":
            p_rho = np.nan; p_t1 = np.nan
        else:
            p_rho = wilcoxon_p(d_rho)
            p_t1 = mcnemar_p(naive_prod.loc[common, "top1_hit"].values,
                             prod.loc[common, "top1_hit"].values)

        yt = te[TARGET]; yp = te[col]
        calibrated = name not in ("XGBRanker (pairwise)", "Heuristic weighted")
        per_origin_rows.append({
            "origin": T, "test_target_year": T + 1, "model": name,
            "n_cv_folds": len(folds), "n_train_rows": len(tr), "n_test_rows": len(te),
            "spearman_rho": s["spearman_rho"], "kendall_tau": s["kendall_tau"],
            "top3_overlap": s["top3_overlap"], "top1_hit_rate": s["top1_hit_rate"],
            "ndcg@5": s["ndcg@5"], "ndcg@10": s["ndcg@10"],
            "rho_5seed_mean": float(np.mean(seed_rho[name])) if name in STOCHASTIC else s["spearman_rho"],
            "rho_5seed_std": float(np.std(seed_rho[name])) if name in STOCHASTIC else 0.0,
            "top1_5seed_mean": float(np.mean(seed_top1[name])) if name in STOCHASTIC else s["top1_hit_rate"],
            "rmse": float(np.sqrt(mean_squared_error(yt, yp))) if calibrated else np.nan,
            "mae": float(mean_absolute_error(yt, yp)) if calibrated else np.nan,
            "r2": float(r2_score(yt, yp)) if calibrated else np.nan,
            "delta_rho_vs_naive": s["spearman_rho"] - naive_prod["spearman_rho"].mean(),
            "wilcoxon_p_rho": p_rho, "mcnemar_p_top1": p_t1,
        })
        for hs in common:
            per_product_rows.append({
                "origin": T, "model": name, "hs6": hs,
                "spearman_rho": float(prod.loc[hs, "spearman_rho"]),
                "top1_hit": float(prod.loc[hs, "top1_hit"]),
                "top3_overlap": float(prod.loc[hs, "top3_overlap"]),
                "delta_rho_vs_naive": float(prod.loc[hs, "spearman_rho"]
                                            - naive_prod.loc[hs, "spearman_rho"]),
            })
        print(f"    {name:<24} rho={s['spearman_rho']:.4f}  "
              f"top3={s['top3_overlap']:.3f}  top1={s['top1_hit_rate']:.3f}"
              + ("" if np.isnan(p_rho) else f"  p={p_rho:.2e}"))

po = pd.DataFrame(per_origin_rows)
pp = pd.DataFrame(per_product_rows)
po.to_csv(os.path.join(OUT, "rolling_origin_per_origin_v5.csv"), index=False)
pp.to_csv(os.path.join(OUT, "rolling_origin_per_product_v5.csv"), index=False)

# ---------------------------------------------------------------------------
# Pooled analysis across origins  (4 origins x 55 products = 220 pairs)
# ---------------------------------------------------------------------------
print("\n" + "=" * 78)
print("POOLED ACROSS ORIGINS")
print("=" * 78)

pooled_rows = []
for name in MODEL_ORDER:
    sub = pp[pp["model"] == name]
    g = po[po["model"] == name]
    d = sub["delta_rho_vs_naive"].values
    if name == "Naive persistence":
        p_pool = np.nan; win = np.nan
    else:
        p_pool = wilcoxon_p(d)
        win = float(np.mean(d > 0))
    pooled_rows.append({
        "model": name,
        "n_pairs": int(len(sub)),
        "rho_pooled_mean": float(sub["spearman_rho"].mean()),
        "rho_origin_mean": float(g["spearman_rho"].mean()),
        "rho_origin_std": float(g["spearman_rho"].std(ddof=0)),
        "rho_origin_min": float(g["spearman_rho"].min()),
        "rho_origin_max": float(g["spearman_rho"].max()),
        "top3_origin_mean": float(g["top3_overlap"].mean()),
        "top1_origin_mean": float(g["top1_hit_rate"].mean()),
        "ndcg5_origin_mean": float(g["ndcg@5"].mean()),
        "delta_rho_pooled_mean": float(np.mean(d)),
        "delta_rho_pooled_median": float(np.median(d)),
        "product_win_rate_vs_naive": win,
        "wilcoxon_p_pooled": p_pool,
        "n_origins_beating_naive": int((g["delta_rho_vs_naive"] > 0).sum()),
    })

pooled = pd.DataFrame(pooled_rows).sort_values("rho_origin_mean", ascending=False)
pooled.to_csv(os.path.join(OUT, "rolling_origin_pooled_v5.csv"), index=False)
print(pooled[["model", "rho_origin_mean", "rho_origin_std", "rho_origin_min",
              "rho_origin_max", "delta_rho_pooled_mean", "product_win_rate_vs_naive",
              "wilcoxon_p_pooled", "n_origins_beating_naive"]].to_string(index=False))

# ---- head-to-head: ranker vs each competitor, pooled over 220 pairs -------
print("\nHEAD-TO-HEAD (pooled paired Wilcoxon on per-product rho)")
h2h = []
rk = pp[pp["model"] == "XGBRanker (pairwise)"].set_index(["origin", "hs6"])["spearman_rho"]
for name in MODEL_ORDER:
    if name == "XGBRanker (pairwise)":
        continue
    ot = pp[pp["model"] == name].set_index(["origin", "hs6"])["spearman_rho"]
    idx = rk.index.intersection(ot.index)
    d = (rk.loc[idx] - ot.loc[idx]).values
    h2h.append({"opponent": name, "n_pairs": int(len(d)),
                "mean_delta_rho": float(np.mean(d)),
                "median_delta_rho": float(np.median(d)),
                "win_rate": float(np.mean(d > 0)),
                "wilcoxon_p": wilcoxon_p(d)})
h2h_df = pd.DataFrame(h2h)
h2h_df.to_csv(os.path.join(OUT, "rolling_origin_head2head_v5.csv"), index=False)
print(h2h_df.to_string(index=False))

# ---- Bonferroni bookkeeping ----------------------------------------------
n_h2h = len(h2h_df)
bonf_h2h = 0.05 / n_h2h
h2h_df["survives_bonferroni"] = h2h_df["wilcoxon_p"] < bonf_h2h

summary = {
    "design": {
        "origins": ORIGINS,
        "meaning": "origin T = features in year T, target = realized exports in T+1",
        "hyperparameters": "re-tuned per origin using only CV folds with validation year < T",
        "search_budget_per_origin": {"regressor": N_SEARCH_REG,
                                     "ranker_pairwise": N_SEARCH_RANK,
                                     "ranker_ndcg": N_SEARCH_RANK},
        "seeds": SEEDS,
        "n_features": len(FEATURES),
    },
    "per_origin_hyperparameters": chosen_hparams,
    "objective_comparison": cv_objective_cmp,
    "pairwise_beats_ndcg_in_all_origins": bool(all(c["pairwise_beats_ndcg"]
                                                   for c in cv_objective_cmp)),
    "pooled": pooled.to_dict(orient="records"),
    "head_to_head_vs_ranker": h2h_df.to_dict(orient="records"),
    "bonferroni": {"n_head_to_head_tests": n_h2h, "threshold": bonf_h2h},
    "origin_2023_vs_v4": {
        "same_split": True,
        "hyperparameter_search_rerun_independently": True,
        "exact_reproduction_expected_for": [
            "Random Forest", "LightGBM", "Gradient Boosting",
            "Ridge Regression", "Naive persistence", "Heuristic weighted"],
        "close_but_not_identical_expected_for": [
            "XGBRanker (pairwise)", "XGBoost Regressor", "Two-Stage Hurdle"],
        "note": "tuned models differ because the per-origin search uses its own "
                "RNG stream and a smaller budget than v4",
    },
}
with open(os.path.join(OUT, "rolling_origin_summary_v5.json"), "w") as f:
    json.dump(summary, f, indent=2, default=float)

print("\n" + "=" * 78)
print("wrote outputs/rolling_origin_{per_origin,per_product,pooled,head2head}_v5.csv")
print("      outputs/rolling_origin_summary_v5.json")
print("=" * 78)