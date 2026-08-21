"""
CBEC-AI Export-Destination Ranking — v4 Rigorous Pipeline
=========================================================
Implements, in order:
  (2) Leakage audit vs `year` (Pearson, Spearman, mutual information) + a
      within-(hs6,year)-group variance test. A feature is flagged LEAKING if it
      is BOTH strongly year-associated AND ~constant within ranking groups
      (so it can only help by encoding the temporal regime).
  (3) Feature selection: drop gdp_per_capita_usd (hurts ranking + synthetic),
      drop leaking features (entry_metrics_imputed, feature_year_num); keep the
      statistically-validated engineered features.
  (4) Time-aware expanding-window CV (train years<=N, validate N+1); 2023 held out.
  (5) XGBoost hyperparameter search over the time-aware CV for BOTH an
      XGBRegressor and an XGBRanker (rank:pairwise & rank:ndcg, grouped by
      product+year), scored on ranking (Spearman rho).
  (6) Retrain all other models (RF, LightGBM, GradientBoosting, Ridge, Naive,
      Heuristic, Hurdle) on the SAME cleaned features + SAME splits.
  (7) Significance tests vs naive baseline: paired Wilcoxon (rho, top3) +
      McNemar (top1); 5-seed ablation with paired t-tests. Seeds {42,101,2024,777,999}.
  (8) Honest results table (Spearman, Top-3, Top-1, NDCG@5/10, RMSE, R2, p-values).
  (9) Leakage red-flag check: any model beating naive with large delta AND very
      low p is investigated (feature-importance year-proxy scan) before trusting.
 (10) All outputs saved as *_v4 artifacts; v3 files are never overwritten.

Run from the 05(data) directory:  python src/train_models_v4.py
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
from sklearn.metrics import accuracy_score, roc_auc_score, average_precision_score
from sklearn.feature_selection import mutual_info_regression
from sklearn.linear_model import Ridge
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
import xgboost as xgb
import lightgbm as lgb

warnings.filterwarnings("ignore")

# ---------------------------------------------------------------------------
# Reproducibility
# ---------------------------------------------------------------------------
GLOBAL_SEED = 42
SEEDS_ABLATION = [42, 101, 2024, 777, 999]
os.environ["PYTHONHASHSEED"] = str(GLOBAL_SEED)
random.seed(GLOBAL_SEED)
np.random.seed(GLOBAL_SEED)

DATA_PATH = os.path.join("filtered_for_model", "master_cbec_train_dataset.csv")
OUT = "outputs"
os.makedirs(OUT, exist_ok=True)
TARGET = "target_log_export_value"
RAW_TARGET = "target_future_export_value_usd"

# ---------------------------------------------------------------------------
# Ranking metric helpers (identical methodology to v3 for comparability)
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
    """Per-product (grouped by hs6) ranking eval, averaged — matches v3."""
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
    nz = diff[diff != 0]
    if len(nz) < 5:
        return 1.0
    try:
        _, p = wilcoxon(nz, alternative="two-sided")
        return float(p)
    except Exception:
        return 1.0


# ---------------------------------------------------------------------------
# Load data & define feature groups
# ---------------------------------------------------------------------------
print("=" * 80)
print(f"CBEC-AI v4 PIPELINE  (global seed={GLOBAL_SEED})")
print("=" * 80)

df = pd.read_csv(DATA_PATH)
print(f"Loaded {df.shape[0]} rows x {df.shape[1]} cols")

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

all_candidate_features = group_a + group_b + group_c + group_d + group_f + group_eng + group_time

# ===========================================================================
# STEP 2 : LEAKAGE AUDIT vs YEAR
# ===========================================================================
print("\n" + "=" * 80)
print("STEP 2: LEAKAGE AUDIT vs YEAR (Pearson / Spearman / Mutual Information)")
print("=" * 80)

year = df["year"].values
audit_rows = []
for feat in all_candidate_features:
    x = df[feat].fillna(0.0).astype(float)
    # correlation with year
    try:
        pear = float(np.corrcoef(x, year)[0, 1]) if x.std() > 0 else 0.0
    except Exception:
        pear = 0.0
    sp = float(spearmanr(x, year).correlation) if x.std() > 0 else 0.0
    sp = 0.0 if np.isnan(sp) else sp
    # mutual information between feature and year (feature as predictor of year)
    try:
        mi = float(mutual_info_regression(x.values.reshape(-1, 1), year,
                                          random_state=GLOBAL_SEED)[0])
    except Exception:
        mi = 0.0
    # within-(hs6,year) group variance ratio: ~0 => constant within ranking group
    grp_var = df.groupby(["hs6", "year"])[feat].transform(lambda s: s.var(ddof=0))
    overall_var = x.var(ddof=0)
    within_ratio = float(np.nanmean(grp_var) / overall_var) if overall_var > 1e-12 else 0.0
    # deterministic-function-of-year check: does (year) fully determine feature?
    n_unique_per_year = df.groupby("year")[feat].nunique()
    det_by_year = bool((n_unique_per_year <= 1).all())  # one value per year => pure year fn
    audit_rows.append({
        "feature": feat,
        "pearson_year": round(pear, 4),
        "spearman_year": round(sp, 4),
        "mutual_info_year": round(mi, 4),
        "within_group_var_ratio": round(within_ratio, 4),
        "deterministic_by_year": det_by_year,
    })

audit_df = pd.DataFrame(audit_rows).sort_values("mutual_info_year", ascending=False).reset_index(drop=True)

# Decision rule: LEAK if strongly year-associated AND near-constant within ranking group.
def decide(r):
    year_assoc = (abs(r["pearson_year"]) >= 0.30) or (abs(r["spearman_year"]) >= 0.30) or (r["mutual_info_year"] >= 0.15)
    near_const_in_group = r["within_group_var_ratio"] <= 0.05
    if r["feature"] == "gdp_per_capita_usd":
        return "DROP (instruction: hurts ranking; also synthetic year-trend)"
    if year_assoc and near_const_in_group:
        return "DROP (year-proxy leak: year-associated AND ~constant within ranking group)"
    if year_assoc and not near_const_in_group:
        return "KEEP-FLAG (year-trending but cross-sectional signal within group)"
    return "KEEP"

audit_df["decision"] = audit_df.apply(decide, axis=1)
audit_df.to_csv(os.path.join(OUT, "leakage_audit_v4.csv"), index=False)

print(audit_df.to_string(index=False))
leaking = audit_df[audit_df["decision"].str.startswith("DROP")]["feature"].tolist()
print(f"\nFeatures flagged for DROP: {leaking}")

# ===========================================================================
# STEP 3 : FEATURE SELECTION
# ===========================================================================
DROP_FEATURES = set(["entry_metrics_imputed", "feature_year_num", "gdp_per_capita_usd"])
# Assert our leakage rule actually caught the known leaks (sanity)
for must in ["entry_metrics_imputed", "feature_year_num"]:
    assert must in leaking, f"Expected {must} to be flagged as leaking by the audit rule!"

FEATURES = [f for f in all_candidate_features if f not in DROP_FEATURES]
KEEP_ENG = ["demand_per_capita", "india_export_intensity", "competition_intensity", "market_growth_x_fta"]
assert all(f in FEATURES for f in KEEP_ENG), "Validated engineered features must be kept!"

print("\n" + "=" * 80)
print("STEP 3: FEATURE SELECTION")
print("=" * 80)
print(f"Dropped ({len(DROP_FEATURES)}): {sorted(DROP_FEATURES)}")
print(f"Kept ({len(FEATURES)}): {FEATURES}")

with open(os.path.join(OUT, "selected_features_v4.json"), "w") as f:
    json.dump({"kept_features": FEATURES, "dropped_features": sorted(DROP_FEATURES),
               "validated_engineered_kept": KEEP_ENG,
               "drop_rationale": {
                   "entry_metrics_imputed": "1[year>2020]; deterministic year step-function; year-proxy leak.",
                   "feature_year_num": "raw year; constant within (hs6,year) ranking group; out-of-range at test.",
                   "gdp_per_capita_usd": "synthetic year-trend; ablation showed it hurts ranking."}}, f, indent=2)

# ===========================================================================
# STEP 4 : TIME-AWARE EXPANDING-WINDOW CV
# ===========================================================================
# Labeled years with a valid target: 2017..2023 (target at T+1 = 2018..2024).
# Final untouched test = 2023 (predicts 2024). CV uses only years <= 2022.
CV_FOLDS = [
    (list(range(2017, 2019)), 2019),
    (list(range(2017, 2020)), 2020),
    (list(range(2017, 2021)), 2021),
    (list(range(2017, 2022)), 2022),
]
TEST_YEAR = 2023
FINAL_TRAIN_YEARS = list(range(2017, 2023))  # 2017-2022 for final fit

df_labeled = df[df[TARGET].notna()].copy()
df_test = df[df["year"] == TEST_YEAR].copy()

def qid_sorted(frame):
    """Return frame sorted so (hs6,year) groups are contiguous, group sizes, and
    integer relevance grades (0..n-1 within each group by ascending target).
    XGBRanker requires non-negative INTEGER relevance labels, not continuous."""
    f = frame.sort_values(["year", "hs6"]).copy()
    grp = f.groupby(["year", "hs6"]).size().values
    rel = (f.groupby(["year", "hs6"])[TARGET]
             .rank(method="first").astype(int) - 1).values
    return f, grp, rel

# ===========================================================================
# STEP 5 : XGBOOST HYPERPARAMETER SEARCH (Regressor + Ranker) over time-aware CV
# ===========================================================================
print("\n" + "=" * 80)
print("STEP 5: XGBOOST TUNING OVER TIME-AWARE EXPANDING-WINDOW CV")
print("=" * 80)

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

def sample_params(rng):
    return {k: rng.choice(v) for k, v in param_grid.items()}

N_SEARCH = 40
rng = np.random.RandomState(GLOBAL_SEED)

def cv_score_regressor(params):
    scores = []
    for train_years, val_year in CV_FOLDS:
        tr = df_labeled[df_labeled["year"].isin(train_years)]
        va = df[df["year"] == val_year].copy()
        model = xgb.XGBRegressor(
            objective="reg:squarederror", eval_metric="rmse",
            random_state=GLOBAL_SEED, n_jobs=-1,
            max_depth=int(params["max_depth"]), learning_rate=float(params["learning_rate"]),
            n_estimators=int(params["n_estimators"]), subsample=float(params["subsample"]),
            colsample_bytree=float(params["colsample_bytree"]),
            min_child_weight=int(params["min_child_weight"]),
            reg_alpha=float(params["reg_alpha"]), reg_lambda=float(params["reg_lambda"]))
        model.fit(tr[FEATURES].fillna(0.0), tr[TARGET])
        va["p"] = model.predict(va[FEATURES].fillna(0.0))
        s, _ = evaluate_rankings(va, "p")
        scores.append(s["spearman_rho"])
    return float(np.mean(scores)), scores

def cv_score_ranker(params, objective):
    scores = []
    for train_years, val_year in CV_FOLDS:
        tr = df_labeled[df_labeled["year"].isin(train_years)]
        tr_s, grp, rel = qid_sorted(tr)
        va = df[df["year"] == val_year].copy()
        model = xgb.XGBRanker(
            objective=objective, random_state=GLOBAL_SEED, n_jobs=-1,
            max_depth=int(params["max_depth"]), learning_rate=float(params["learning_rate"]),
            n_estimators=int(params["n_estimators"]), subsample=float(params["subsample"]),
            colsample_bytree=float(params["colsample_bytree"]),
            min_child_weight=int(params["min_child_weight"]),
            reg_alpha=float(params["reg_alpha"]), reg_lambda=float(params["reg_lambda"]))
        # relevance must be non-negative INTEGER grades (within-group rank of target)
        model.fit(tr_s[FEATURES].fillna(0.0), rel, group=grp)
        va["p"] = model.predict(va[FEATURES].fillna(0.0))
        s, _ = evaluate_rankings(va, "p")
        scores.append(s["spearman_rho"])
    return float(np.mean(scores)), scores

# --- search: regressor ---
cv_records = []
best_reg = {"cv": -1, "params": None}
for i in range(N_SEARCH):
    p = sample_params(rng)
    cv, folds = cv_score_regressor(p)
    cv_records.append({"model": "XGBRegressor", "cv_spearman": cv, **{k: p[k] for k in param_grid}})
    if cv > best_reg["cv"]:
        best_reg = {"cv": cv, "params": p, "folds": folds}
print(f"Best XGBRegressor CV Spearman = {best_reg['cv']:.4f}")

# --- search: ranker (pairwise & ndcg) ---
best_rank = {"cv": -1, "params": None, "objective": None}
rng2 = np.random.RandomState(GLOBAL_SEED + 1)
for obj in ["rank:pairwise", "rank:ndcg"]:
    for i in range(N_SEARCH // 2):
        p = sample_params(rng2)
        cv, folds = cv_score_ranker(p, obj)
        cv_records.append({"model": f"XGBRanker[{obj}]", "cv_spearman": cv, **{k: p[k] for k in param_grid}})
        if cv > best_rank["cv"]:
            best_rank = {"cv": cv, "params": p, "objective": obj, "folds": folds}
print(f"Best XGBRanker CV Spearman = {best_rank['cv']:.4f} (objective={best_rank['objective']})")

pd.DataFrame(cv_records).sort_values("cv_spearman", ascending=False).to_csv(
    os.path.join(OUT, "cv_results_v4.csv"), index=False)
with open(os.path.join(OUT, "best_hyperparameters_v4.json"), "w") as f:
    json.dump({
        "xgb_regressor": {"cv_spearman": best_reg["cv"], "params": {k: (int(v) if isinstance(v, (np.integer,)) else float(v)) for k, v in best_reg["params"].items()}},
        "xgb_ranker": {"cv_spearman": best_rank["cv"], "objective": best_rank["objective"], "params": {k: (int(v) if isinstance(v, (np.integer,)) else float(v)) for k, v in best_rank["params"].items()}},
    }, f, indent=2, default=float)

# ===========================================================================
# STEP 6 : RETRAIN ALL MODELS on SAME cleaned features + SAME splits
#          (final train = 2017-2022, test = 2023)
# ===========================================================================
print("\n" + "=" * 80)
print("STEP 6: RETRAIN ALL MODELS (train 2017-2022, test 2023) ON CLEANED FEATURES")
print("=" * 80)

df_tr = df_labeled[df_labeled["year"].isin(FINAL_TRAIN_YEARS)].copy()
Xtr = df_tr[FEATURES].fillna(0.0); ytr = df_tr[TARGET]
Xte = df_test[FEATURES].fillna(0.0); yte = df_test[TARGET]

# 1) Tuned XGBRegressor
rp = best_reg["params"]
m_xgbreg = xgb.XGBRegressor(objective="reg:squarederror", eval_metric="rmse",
    random_state=GLOBAL_SEED, n_jobs=-1,
    max_depth=int(rp["max_depth"]), learning_rate=float(rp["learning_rate"]),
    n_estimators=int(rp["n_estimators"]), subsample=float(rp["subsample"]),
    colsample_bytree=float(rp["colsample_bytree"]), min_child_weight=int(rp["min_child_weight"]),
    reg_alpha=float(rp["reg_alpha"]), reg_lambda=float(rp["reg_lambda"]))
m_xgbreg.fit(Xtr, ytr)
df_test["pred_xgb_reg"] = m_xgbreg.predict(Xte)

# 2) Tuned XGBRanker
kp = best_rank["params"]
df_tr_s, grp_tr, rel_tr = qid_sorted(df_tr)
m_xgbrank = xgb.XGBRanker(objective=best_rank["objective"], random_state=GLOBAL_SEED, n_jobs=-1,
    max_depth=int(kp["max_depth"]), learning_rate=float(kp["learning_rate"]),
    n_estimators=int(kp["n_estimators"]), subsample=float(kp["subsample"]),
    colsample_bytree=float(kp["colsample_bytree"]), min_child_weight=int(kp["min_child_weight"]),
    reg_alpha=float(kp["reg_alpha"]), reg_lambda=float(kp["reg_lambda"]))
m_xgbrank.fit(df_tr_s[FEATURES].fillna(0.0), rel_tr, group=grp_tr)
df_test["pred_xgb_rank"] = m_xgbrank.predict(Xte)

# 3) Random Forest
m_rf = RandomForestRegressor(n_estimators=300, max_depth=8, min_samples_leaf=3,
                             random_state=GLOBAL_SEED, n_jobs=-1)
m_rf.fit(Xtr, ytr)
df_test["pred_rf"] = m_rf.predict(Xte)

# 4) LightGBM
m_lgb = lgb.LGBMRegressor(objective="regression", n_estimators=400, learning_rate=0.05,
                          max_depth=6, num_leaves=31, min_child_samples=15,
                          subsample=0.8, colsample_bytree=0.8, random_state=GLOBAL_SEED,
                          deterministic=True, verbose=-1)
m_lgb.fit(Xtr, ytr)
df_test["pred_lightgbm"] = m_lgb.predict(Xte)

# 5) Gradient Boosting (sklearn)
m_gbr = GradientBoostingRegressor(n_estimators=200, learning_rate=0.05, max_depth=4,
                                  random_state=GLOBAL_SEED)
m_gbr.fit(Xtr, ytr)
df_test["pred_gbr"] = m_gbr.predict(Xte)

# 6) Ridge
m_ridge = Ridge(alpha=1.0, random_state=GLOBAL_SEED)
m_ridge.fit(Xtr, ytr)
df_test["pred_ridge"] = m_ridge.predict(Xte)

# 7) Naive historical-growth baseline (persistence): export_t * (1 + growth_1y)
pred_naive_raw = df_test["india_export_value_usd"] * (1.0 + df_test["india_export_growth_1y"].clip(-0.9, 5.0))
df_test["pred_naive"] = np.log1p(np.maximum(0, pred_naive_raw))

# 8) Heuristic weighted score
ne = df_test.groupby("hs6")["india_export_value_usd"].transform(lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
ng = df_test.groupby("hs6")["destination_import_growth_1y"].transform(lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
ns = df_test.groupby("hs6")["india_market_share_pct"].transform(lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
nd = 1.0 - (df_test["distance_km"] / 12000.0)
df_test["pred_heuristic"] = (0.40 * ne + 0.25 * ng + 0.20 * ns + 0.15 * nd) * 20.0

# 9) Two-stage hurdle (XGB classifier P(export>0) * XGB regressor on nonzero)
df_tr["is_nz"] = (df_tr[RAW_TARGET] > 0).astype(int)
clf = xgb.XGBClassifier(objective="binary:logistic", eval_metric="logloss",
    random_state=GLOBAL_SEED, n_jobs=-1, max_depth=4, learning_rate=0.05,
    n_estimators=300, subsample=0.85, colsample_bytree=0.85, min_child_weight=3)
clf.fit(Xtr, df_tr["is_nz"])
prob_nz = clf.predict_proba(Xte)[:, 1]
nz_mask = df_tr[RAW_TARGET] > 0
m_s2 = xgb.XGBRegressor(objective="reg:squarederror", random_state=GLOBAL_SEED, n_jobs=-1,
    max_depth=int(rp["max_depth"]), learning_rate=float(rp["learning_rate"]),
    n_estimators=int(rp["n_estimators"]), subsample=float(rp["subsample"]),
    colsample_bytree=float(rp["colsample_bytree"]), min_child_weight=int(rp["min_child_weight"]),
    reg_alpha=float(rp["reg_alpha"]), reg_lambda=float(rp["reg_lambda"]))
m_s2.fit(Xtr[nz_mask.values], df_tr.loc[nz_mask, TARGET])
pred_cond = np.maximum(0, np.expm1(m_s2.predict(Xte)))
df_test["pred_hurdle"] = np.log1p(prob_nz * pred_cond)
hurdle_clf_metrics = {
    "roc_auc": float(roc_auc_score((df_test[RAW_TARGET] > 0).astype(int), prob_nz)),
    "pr_auc": float(average_precision_score((df_test[RAW_TARGET] > 0).astype(int), prob_nz)),
    "accuracy": float(accuracy_score((df_test[RAW_TARGET] > 0).astype(int), (prob_nz >= 0.5).astype(int))),
}

models = {
    "XGBoost Ranker (Tuned, rank obj)": "pred_xgb_rank",
    "XGBoost Regressor (Tuned)": "pred_xgb_reg",
    "Random Forest Regressor": "pred_rf",
    "LightGBM Regressor": "pred_lightgbm",
    "Gradient Boosting (sklearn)": "pred_gbr",
    "Two-Stage Hurdle (XGB Clf+Reg)": "pred_hurdle",
    "Naive Historical Growth Baseline": "pred_naive",
    "Ridge Regression": "pred_ridge",
    "Heuristic Weighted Score": "pred_heuristic",
}

# ===========================================================================
# STEP 7+8 : BENCHMARK TABLE + SIGNIFICANCE TESTS vs NAIVE
# ===========================================================================
print("\n" + "=" * 80)
print("STEP 7/8: BENCHMARK + SIGNIFICANCE TESTS (vs naive baseline)")
print("=" * 80)

_, naive_prod = evaluate_rankings(df_test, "pred_naive")
naive_rho = naive_prod["spearman_rho"].values
naive_top3 = naive_prod["top3_overlap"].values
naive_top1 = naive_prod["top1_hit"].values

rows = []
for name, col in models.items():
    yt = df_test[TARGET]; yp = df_test[col]
    rmse = float(np.sqrt(mean_squared_error(yt, yp)))
    mae = float(mean_absolute_error(yt, yp))
    r2 = float(r2_score(yt, yp)) if "Heuristic" not in name else np.nan
    s, prod = evaluate_rankings(df_test, col)
    if name != "Naive Historical Growth Baseline":
        p_rho = wilcoxon_p(prod["spearman_rho"].values - naive_rho)
        p_top3 = wilcoxon_p(prod["top3_overlap"].values - naive_top3)
        p_top1 = mcnemar_p(naive_top1, prod["top1_hit"].values)
        d_rho = s["spearman_rho"] - naive_prod["spearman_rho"].mean()
    else:
        p_rho = p_top3 = p_top1 = np.nan
        d_rho = 0.0
    rows.append({
        "Model": name, "Spearman_rho": s["spearman_rho"], "Top-3 Overlap": s["top3_overlap"],
        "Top-1 Hit Rate": s["top1_hit_rate"], "Kendall_tau": s["kendall_tau"],
        "NDCG@5": s["ndcg@5"], "NDCG@10": s["ndcg@10"], "RMSE": rmse, "MAE": mae, "R2": r2,
        "Delta_rho_vs_Naive": d_rho, "Wilcoxon_p(rho)": p_rho,
        "Wilcoxon_p(Top3)": p_top3, "McNemar_p(Top1)": p_top1,
    })

bench = pd.DataFrame(rows).sort_values("Spearman_rho", ascending=False).reset_index(drop=True)
bench.to_csv(os.path.join(OUT, "benchmark_comparison_v4.csv"), index=False)
print(bench[["Model", "Spearman_rho", "Top-3 Overlap", "Top-1 Hit Rate", "NDCG@5",
             "RMSE", "R2", "Delta_rho_vs_Naive", "Wilcoxon_p(rho)", "McNemar_p(Top1)"]].to_string(index=False))

# ===========================================================================
# STEP 9 : LEAKAGE RED-FLAG CHECK
# ===========================================================================
print("\n" + "=" * 80)
print("STEP 9: LEAKAGE RED-FLAG CHECK")
print("=" * 80)
red_flags = []
for _, r in bench.iterrows():
    if r["Model"] == "Naive Historical Growth Baseline":
        continue
    if pd.notna(r["Wilcoxon_p(rho)"]) and r["Delta_rho_vs_Naive"] > 0.05 and r["Wilcoxon_p(rho)"] < 0.01:
        red_flags.append(r["Model"])
if red_flags:
    print(f"RED FLAG: {red_flags} beat naive by >0.05 rho with p<0.01 — investigating feature importance for year-proxies...")
    booster = m_xgbreg.get_booster()
    gain = booster.get_score(importance_type="gain")
    top = sorted(gain.items(), key=lambda kv: kv[1], reverse=True)[:10]
    print("Top-10 XGBRegressor gains:", top)
    suspects = [f for f, _ in top if f in ("feature_year_num", "entry_metrics_imputed", "gdp_per_capita_usd")]
    print(f"Year-proxy suspects among top features: {suspects if suspects else 'NONE — no obvious leakage'}")
else:
    print("No model dramatically beats naive with very low p-values.")
    print("=> Consistent with prior honest finding: short-term trade inertia dominates; "
          "ML gains over persistence are modest and not necessarily significant. No leakage red flags.")

# ===========================================================================
# STEP 7b : 5-SEED ABLATION with paired t-tests (uses tuned XGBRegressor params)
# ===========================================================================
print("\n" + "=" * 80)
print("STEP 7b: 5-SEED ABLATION WITH PAIRED t-TESTS")
print("=" * 80)

ablation_configs = {
    "Full Model (cleaned features)": FEATURES,
    "Model A: Trade Only (B+C)": group_b + group_c,
    "Model B: Trade + Macro (B+C+gdp/pop/dist)": group_b + group_c + ["gdp_usd", "population", "distance_km", "common_border", "common_language"],
    "Model C: No Engineered": [f for f in FEATURES if f not in KEEP_ENG],
    "Drop India Export Value": [f for f in FEATURES if f != "india_export_value_usd"],
    "Drop India Market Share": [f for f in FEATURES if f != "india_market_share_pct"],
    "Drop Distance (Gravity)": [f for f in FEATURES if f != "distance_km"],
    "Drop Engineered Intensity Feats": [f for f in FEATURES if f not in ["india_export_intensity", "competition_intensity", "demand_per_capita"]],
}

def fit_eval_xgb(feat_subset, seed):
    p = best_reg["params"]
    m = xgb.XGBRegressor(objective="reg:squarederror", random_state=seed, n_jobs=-1,
        max_depth=int(p["max_depth"]), learning_rate=float(p["learning_rate"]),
        n_estimators=int(p["n_estimators"]), subsample=float(p["subsample"]),
        colsample_bytree=float(p["colsample_bytree"]), min_child_weight=int(p["min_child_weight"]),
        reg_alpha=float(p["reg_alpha"]), reg_lambda=float(p["reg_lambda"]))
    m.fit(df_tr[feat_subset].fillna(0.0), ytr)
    df_test["_tmp"] = m.predict(df_test[feat_subset].fillna(0.0))
    s, _ = evaluate_rankings(df_test, "_tmp")
    return s["spearman_rho"]

ablation_raw = {}
for name, feats in ablation_configs.items():
    ablation_raw[name] = [fit_eval_xgb(feats, sd) for sd in SEEDS_ABLATION]

full_arr = np.array(ablation_raw["Full Model (cleaned features)"])
abl_rows = []
for name, rhos in ablation_raw.items():
    arr = np.array(rhos)
    if name == "Full Model (cleaned features)":
        abl_rows.append({"Config": name, "rho_mean": arr.mean(), "rho_std": arr.std(),
                         "delta_vs_full": 0.0, "t_stat": np.nan, "p_value": np.nan,
                         "significant": "Reference"})
    else:
        t = stats.ttest_rel(arr, full_arr)
        abl_rows.append({"Config": name, "rho_mean": arr.mean(), "rho_std": arr.std(),
                         "delta_vs_full": float(arr.mean() - full_arr.mean()),
                         "t_stat": float(t.statistic), "p_value": float(t.pvalue),
                         "significant": "YES" if t.pvalue < 0.05 else "no"})

abl_df = pd.DataFrame(abl_rows)
abl_df.to_csv(os.path.join(OUT, "ablation_study_5seeds_v4.csv"), index=False)
with open(os.path.join(OUT, "ablation_per_seed_raw_v4.json"), "w") as f:
    json.dump(ablation_raw, f, indent=2)
print(abl_df.to_string(index=False))

# ===========================================================================
# STEP 10 : SAVE ARTIFACTS (feature importance, final summary, winning model)
# ===========================================================================
print("\n" + "=" * 80)
print("STEP 10: SAVE VERSIONED ARTIFACTS")
print("=" * 80)

# Feature importance (tuned XGBRegressor, gain)
gain = m_xgbreg.get_booster().get_score(importance_type="gain")
fi = pd.DataFrame(sorted(gain.items(), key=lambda kv: kv[1], reverse=True), columns=["feature", "gain"])
fi.to_csv(os.path.join(OUT, "feature_importance_v4.csv"), index=False)

# Winning model = top of benchmark by Spearman
winner_name = bench.iloc[0]["Model"]
winner_col = models[winner_name]
winner_summary, _ = evaluate_rankings(df_test, winner_col)
print(f"WINNING MODEL (by Spearman rho on 2023->2024 test): {winner_name}  rho={winner_summary['spearman_rho']:.4f}")

final_summary = {
    "winning_model": winner_name,
    "test_split": "2023 features -> 2024 realized exports (untouched holdout)",
    "cv": "expanding-window time-aware (val 2019,2020,2021,2022)",
    "winner_metrics": winner_summary,
    "benchmark_top": bench.head(3).to_dict(orient="records"),
    "dropped_features": sorted(DROP_FEATURES),
    "kept_features": FEATURES,
    "best_xgb_regressor_params": {k: (int(v) if isinstance(v, np.integer) else float(v)) for k, v in best_reg["params"].items()},
    "best_xgb_ranker": {"objective": best_rank["objective"], "cv_spearman": best_rank["cv"]},
    "hurdle_stage1": hurdle_clf_metrics,
    "red_flags": red_flags,
    "note": "Honest report: whichever model tops Spearman rho on the untouched holdout wins. "
            "No feature was engineered to inflate a specific model.",
}
with open(os.path.join(OUT, "final_metrics_summary_v4.json"), "w") as f:
    json.dump(final_summary, f, indent=2, default=float)

# Persist the tuned XGBRegressor + XGBRanker for backend serving (native JSON).
m_xgbreg.get_booster().save_model(os.path.join(OUT, "xgb_regressor_v4.json"))
m_xgbrank.get_booster().save_model(os.path.join(OUT, "xgb_ranker_v4.json"))

# Forward 2025 predictions from the winning model (for the app), if regressor-type.
df_demo = df[df["year"] == 2024].copy()
if winner_col in ("pred_xgb_reg",):
    df_demo["pred_2025_log"] = m_xgbreg.predict(df_demo[FEATURES].fillna(0.0))
elif winner_col in ("pred_xgb_rank",):
    df_demo["pred_2025_log"] = m_xgbrank.predict(df_demo[FEATURES].fillna(0.0))
else:
    df_demo["pred_2025_log"] = m_xgbreg.predict(df_demo[FEATURES].fillna(0.0))
df_demo["pred_2025_export_usd"] = np.maximum(0, np.expm1(df_demo["pred_2025_log"]))
df_demo["pred_2025_rank"] = df_demo.groupby("hs6")["pred_2025_log"].rank(ascending=False, method="min")
df_demo[["hs6", "product_category", "destination_iso3", "destination_name",
         "india_export_value_usd", "pred_2025_export_usd", "pred_2025_rank"]].sort_values(
    ["hs6", "pred_2025_rank"]).to_csv(os.path.join(OUT, "forward_predictions_2025_v4.csv"), index=False)

print("\nArtifacts written (all *_v4, originals untouched):")
for fn in ["leakage_audit_v4.csv", "selected_features_v4.json", "cv_results_v4.csv",
           "best_hyperparameters_v4.json", "benchmark_comparison_v4.csv",
           "ablation_study_5seeds_v4.csv", "ablation_per_seed_raw_v4.json",
           "feature_importance_v4.csv", "final_metrics_summary_v4.json",
           "xgb_regressor_v4.json", "xgb_ranker_v4.json", "forward_predictions_2025_v4.csv"]:
    print("  outputs/" + fn)

print("\nv4 PIPELINE COMPLETE.")
