"""Produce one authoritative, honest v4 results table + summary update."""
import os, json, numpy as np, pandas as pd

OUT = "outputs"
bench = pd.read_csv(os.path.join(OUT, "benchmark_comparison_v4.csv"))
stab = pd.read_csv(os.path.join(OUT, "seed_stability_v4.csv"))

# RMSE/R2 are not meaningful for ranking-only scores (Ranker, Heuristic):
# their outputs are ordinal scores, not calibrated log-USD.
ranking_only = ["XGBoost Ranker (Tuned, rank obj)", "Heuristic Weighted Score"]
for col in ["RMSE", "MAE", "R2"]:
    bench.loc[bench["Model"].isin(ranking_only), col] = np.nan

# Attach 5-seed mean +/- std (rho) where we computed it.
stab_map = {
    "XGBoost Ranker (Tuned, rank obj)": "Ranker",
    "XGBoost Regressor (Tuned)": "Regressor",
    "Random Forest Regressor": "RandomForest",
    "LightGBM Regressor": "LightGBM",
    "Naive Historical Growth Baseline": "Naive Baseline",
}
sd_lookup = {r["Model"]: r for _, r in stab.iterrows()}
def rho5(m):
    key = stab_map.get(m)
    if key is None: return np.nan, np.nan
    row = sd_lookup.get(key)
    if row is None: return np.nan, np.nan
    return float(row["rho_mean"]), float(row["rho_std"])
bench["rho_5seed_mean"] = bench["Model"].map(lambda m: rho5(m)[0])
bench["rho_5seed_std"] = bench["Model"].map(lambda m: rho5(m)[1])

bench = bench.sort_values("Spearman_rho", ascending=False).reset_index(drop=True)
cols = ["Model", "Spearman_rho", "rho_5seed_mean", "rho_5seed_std", "Top-3 Overlap",
        "Top-1 Hit Rate", "NDCG@5", "NDCG@10", "RMSE", "R2",
        "Delta_rho_vs_Naive", "Wilcoxon_p(rho)", "McNemar_p(Top1)"]
bench[cols].to_csv(os.path.join(OUT, "benchmark_final_v4.csv"), index=False)

pd.set_option("display.width", 200); pd.set_option("display.max_columns", 20)
print("=" * 110)
print("TABLE — CBEC-AI v4 FINAL HONEST BENCHMARK (test: 2023 features -> 2024 exports; RMSE/R2 = N/A for ranking-only models)")
print("=" * 110)
show = bench[cols].copy()
for c in ["Spearman_rho","rho_5seed_mean","rho_5seed_std","Top-3 Overlap","Top-1 Hit Rate","NDCG@5","NDCG@10","RMSE","R2","Delta_rho_vs_Naive"]:
    show[c] = show[c].map(lambda x: f"{x:.4f}" if pd.notna(x) else "N/A")
for c in ["Wilcoxon_p(rho)","McNemar_p(Top1)"]:
    show[c] = show[c].map(lambda x: f"{x:.2e}" if pd.notna(x) else "—")
print(show.to_string(index=False))

# Update final summary with the honest multi-seed verdict.
with open(os.path.join(OUT, "final_metrics_summary_v4.json")) as f:
    summary = json.load(f)
with open(os.path.join(OUT, "redflag_investigation_v4.json")) as f:
    inv = json.load(f)

summary["winner_by_metric"] = {
    "best_ranking_spearman_top3_ndcg": "XGBoost Ranker (Tuned, rank:pairwise) — rho 5-seed 0.8956 +/- 0.0033, stable, leak-free; sig. vs regressor p=0.0020 and vs RF p=0.0023",
    "best_top1_single_market_pick": "Random Forest Regressor — Top-1 0.804 (5-seed)",
    "best_calibrated_value_rmse_r2": "XGBoost Regressor (Tuned) / LightGBM — Ranker RMSE/R2 are N/A (uncalibrated scores)",
}
summary["seed_stability_5"] = {k: {"rho_mean": float(v["rho_mean"]), "rho_std": float(v["rho_std"])}
                               for k, v in sd_lookup.items()}
summary["redflag_investigation"] = inv
summary["headline_finding"] = (
    "With leakage removed (entry_metrics_imputed, feature_year_num, gdp_per_capita dropped) and a "
    "time-aware expanding-window CV, a directly-optimized XGBRanker attains the best ranking quality "
    "(Spearman 0.896, Top-3 0.884), a stable and statistically significant gain over both the tuned "
    "regressor and the naive persistence baseline. Regression models remain preferable when a "
    "calibrated export-value estimate is required. No leakage red flags survived investigation."
)
with open(os.path.join(OUT, "final_metrics_summary_v4.json"), "w") as f:
    json.dump(summary, f, indent=2, default=float)
print("\nWrote outputs/benchmark_final_v4.csv and updated outputs/final_metrics_summary_v4.json")
