"""
Derive the reportable claims from the rolling-origin run, including which
single-origin conclusions survive and which do not.
"""
import json
import numpy as np
import pandas as pd

OUT = "outputs"
po = pd.read_csv(f"{OUT}/rolling_origin_per_origin_v5.csv")
h2h = pd.read_csv(f"{OUT}/rolling_origin_head2head_v5.csv")
pooled = pd.read_csv(f"{OUT}/rolling_origin_pooled_v5.csv")
summ = json.load(open(f"{OUT}/rolling_origin_summary_v5.json"))

RK = "XGBRanker (pairwise)"
NV = "Naive persistence"
ORIGINS = sorted(po["origin"].unique())

print("=" * 76)
print("PER-ORIGIN delta rho vs persistence, and its STABILITY")
print("=" * 76)
rows = []
for m in po["model"].unique():
    if m == NV:
        continue
    d = []
    for T in ORIGINS:
        r = po[(po.model == m) & (po.origin == T)]["spearman_rho"].iloc[0]
        n = po[(po.model == NV) & (po.origin == T)]["spearman_rho"].iloc[0]
        d.append(r - n)
    d = np.array(d)
    rows.append({
        "model": m,
        **{f"d{T}": round(float(x), 4) for T, x in zip(ORIGINS, d)},
        "delta_mean": float(d.mean()),
        "delta_std": float(d.std(ddof=0)),
        "delta_min": float(d.min()),
        "n_origins_positive": int((d > 0).sum()),
        "stability_ratio": float(d.mean() / d.std(ddof=0)) if d.std(ddof=0) > 0 else np.inf,
    })
st = pd.DataFrame(rows).sort_values("delta_mean", ascending=False)
print(st.to_string(index=False))
st.to_csv(f"{OUT}/rolling_origin_delta_stability_v5.csv", index=False)

print()
print("=" * 76)
print("WHICH SINGLE-ORIGIN CONCLUSIONS SURVIVE?")
print("=" * 76)
n_tests = len(h2h)
bonf = 0.05 / n_tests
print(f"head-to-head tests = {n_tests}, Bonferroni threshold = {bonf:.5f}\n")
for _, r in h2h.iterrows():
    nominal = "yes" if r.wilcoxon_p < 0.05 else "NO "
    corrected = "yes" if r.wilcoxon_p < bonf else "NO "
    print(f"  ranker vs {r.opponent:<22} d={r.mean_delta_rho:+.4f} "
          f"win={r.win_rate:.3f} p={r.wilcoxon_p:.3g}  "
          f"sig@.05={nominal} sig@bonf={corrected}")

print()
print("=" * 76)
print("TOP-1 ACROSS ORIGINS (metric-dependence check)")
print("=" * 76)
t1 = po.pivot_table(index="model", columns="origin", values="top1_hit_rate")
t1["mean"] = t1.mean(axis=1)
print(t1.sort_values("mean", ascending=False).round(4).to_string())

print()
print("=" * 76)
print("OBJECTIVE COMPARISON: pairwise vs listwise NDCG, per origin (CV)")
print("=" * 76)
oc = pd.DataFrame(summ["objective_comparison"])
print(oc.round(4).to_string(index=False))
print(f"pairwise beat ndcg in all origins: {summ['pairwise_beats_ndcg_in_all_origins']}")

print()
print("=" * 76)
print("KEY NUMBERS FOR THE PAPER")
print("=" * 76)
rk = pooled[pooled.model == RK].iloc[0]
nv = pooled[pooled.model == NV].iloc[0]
rkst = st[st.model == RK].iloc[0]
rfst = st[st.model == "Random Forest"].iloc[0]

facts = {
    "n_origins": len(ORIGINS),
    "origins": [int(x) for x in ORIGINS],
    "n_paired_comparisons": int(rk.n_pairs),
    "ranker_rho_origin_mean": rk.rho_origin_mean,
    "ranker_rho_origin_std": rk.rho_origin_std,
    "ranker_rho_min": rk.rho_origin_min,
    "ranker_rho_max": rk.rho_origin_max,
    "naive_rho_origin_mean": nv.rho_origin_mean,
    "naive_rho_origin_std": nv.rho_origin_std,
    "naive_rho_min": nv.rho_origin_min,
    "naive_rho_max": nv.rho_origin_max,
    "ranker_delta_mean": rkst.delta_mean,
    "ranker_delta_std": rkst.delta_std,
    "ranker_delta_min": rkst.delta_min,
    "ranker_origins_positive": int(rkst.n_origins_positive),
    "rf_delta_mean": rfst.delta_mean,
    "rf_delta_std": rfst.delta_std,
    "rf_delta_min": rfst.delta_min,
    "rf_origins_positive": int(rfst.n_origins_positive),
    "ranker_pooled_wilcoxon_vs_naive": rk.wilcoxon_p_pooled,
    "ranker_win_rate_vs_naive": rk.product_win_rate_vs_naive,
    "ranker_top1_origin_mean": rk.top1_origin_mean,
    "rf_top1_origin_mean": float(pooled[pooled.model == "Random Forest"].iloc[0].top1_origin_mean),
    "naive_top1_origin_mean": nv.top1_origin_mean,
    "heuristic_rho_origin_mean": float(pooled[pooled.model == "Heuristic weighted"].iloc[0].rho_origin_mean),
    "heuristic_delta_pooled": float(pooled[pooled.model == "Heuristic weighted"].iloc[0].delta_rho_pooled_mean),
    "heuristic_p_pooled": float(pooled[pooled.model == "Heuristic weighted"].iloc[0].wilcoxon_p_pooled),
    "heuristic_win_rate": float(pooled[pooled.model == "Heuristic weighted"].iloc[0].product_win_rate_vs_naive),
    "lightgbm_p_pooled": float(pooled[pooled.model == "LightGBM"].iloc[0].wilcoxon_p_pooled),
    "lightgbm_origins_positive": int(pooled[pooled.model == "LightGBM"].iloc[0].n_origins_beating_naive),
    "bonferroni_h2h_threshold": bonf,
    "h2h_vs_rf_p": float(h2h[h2h.opponent == "Random Forest"].iloc[0].wilcoxon_p),
    "h2h_vs_rf_delta": float(h2h[h2h.opponent == "Random Forest"].iloc[0].mean_delta_rho),
    "h2h_vs_rf_winrate": float(h2h[h2h.opponent == "Random Forest"].iloc[0].win_rate),
    "h2h_vs_hurdle_p": float(h2h[h2h.opponent == "Two-Stage Hurdle"].iloc[0].wilcoxon_p),
    "h2h_vs_xgbreg_p": float(h2h[h2h.opponent == "XGBoost Regressor"].iloc[0].wilcoxon_p),
    "h2h_vs_xgbreg_delta": float(h2h[h2h.opponent == "XGBoost Regressor"].iloc[0].mean_delta_rho),
    "n_models_beating_naive_all_origins": int((pooled.n_origins_beating_naive == len(ORIGINS)).sum()),
    "models_beating_naive_all_origins": pooled[pooled.n_origins_beating_naive == len(ORIGINS)]["model"].tolist(),
}
for k, v in facts.items():
    print(f"  {k:<38} {v}")

with open(f"{OUT}/rolling_origin_paper_facts_v5.json", "w") as f:
    json.dump(facts, f, indent=2, default=float)
print(f"\nwrote {OUT}/rolling_origin_delta_stability_v5.csv")
print(f"wrote {OUT}/rolling_origin_paper_facts_v5.json")
