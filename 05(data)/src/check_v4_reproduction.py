"""
Determinism check: at origin 2023 the rolling-origin run uses the same split as
the v4 single holdout. Models with FIXED hyperparameters must reproduce v4 to
machine precision; models whose hyperparameters were re-searched per origin are
expected to differ. Verifying this distinguishes a genuine pipeline bug from an
expected search difference.

Also repairs the stale `origin_2023_reproduces_v4` flag in the summary JSON.
"""
import json
import sys
import pandas as pd

OUT = "outputs"
po = pd.read_csv(f"{OUT}/rolling_origin_per_origin_v5.csv")
v4 = pd.read_csv(f"{OUT}/benchmark_final_v4.csv")

V4NAME = {
    "XGBRanker (pairwise)": "XGBoost Ranker (Tuned, rank obj)",
    "Random Forest": "Random Forest Regressor",
    "XGBoost Regressor": "XGBoost Regressor (Tuned)",
    "Two-Stage Hurdle": "Two-Stage Hurdle (XGB Clf+Reg)",
    "Naive persistence": "Naive Historical Growth Baseline",
    "LightGBM": "LightGBM Regressor",
    "Gradient Boosting": "Gradient Boosting (sklearn)",
    "Ridge Regression": "Ridge Regression",
    "Heuristic weighted": "Heuristic Weighted Score",
}
FIXED = {"Random Forest", "LightGBM", "Gradient Boosting",
         "Ridge Regression", "Naive persistence", "Heuristic weighted"}
TUNED = {"XGBRanker (pairwise)", "XGBoost Regressor", "Two-Stage Hurdle"}

o23 = po[po.origin == 2023].set_index("model")["spearman_rho"]
v4r = v4.set_index("Model")["Spearman_rho"]

print("=" * 70)
print("ORIGIN 2023 vs v4 SINGLE HOLDOUT (same split)")
print("=" * 70)
fails = []
for short, full in V4NAME.items():
    a = float(o23[short]); b = float(v4r[full])
    same = abs(a - b) < 1e-9
    kind = "fixed hp" if short in FIXED else "re-tuned"
    mark = "identical" if same else f"differs by {a - b:+.4f}"
    print(f"  {short:<22} {kind}  rolling={a:.7f}  v4={b:.7f}  -> {mark}")
    if short in FIXED and not same:
        fails.append(f"{short} has fixed hyperparameters but did not reproduce v4 "
                     f"({a:.7f} vs {b:.7f}) -- indicates a pipeline bug")
    if short in TUNED and same:
        print(f"      (note: {short} matched exactly despite independent search)")

print("-" * 70)
if fails:
    print(f"{len(fails)} PROBLEM(S):")
    for f in fails:
        print("  -", f)
    sys.exit(1)
print("PASS: every fixed-hyperparameter model reproduces v4 exactly;")
print("      differences are confined to the independently re-tuned models.")

# ---- repair the stale flag if an older run left it behind -----------------
p = f"{OUT}/rolling_origin_summary_v5.json"
s = json.load(open(p))
if "origin_2023_reproduces_v4" in s:
    del s["origin_2023_reproduces_v4"]
    s["origin_2023_vs_v4"] = {
        "same_split": True,
        "hyperparameter_search_rerun_independently": True,
        "exact_reproduction_verified_for": sorted(FIXED),
        "close_but_not_identical_for": sorted(TUNED),
        "note": "tuned models differ because the per-origin search uses its own "
                "RNG stream and a smaller budget than v4",
    }
    json.dump(s, open(p, "w"), indent=2, default=float)
    print(f"\nrepaired stale flag in {p}")
else:
    print(f"\n{p} already carries the corrected field")
