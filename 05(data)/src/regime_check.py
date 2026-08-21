"""
Feasibility check ONLY: does the existing dataset support defining measurable
trade regimes at the level of test origins (2020-2023), using a criterion that
is not invented after the fact to fit a desired story?

Criterion (pre-specified, measurable, defensible):
  Regime label for origin T = sign/magnitude of the CROSS-SECTIONAL MEDIAN of
  destination_import_growth_1y across all (product, destination) rows observed
  in year T. This is a demand-side macro indicator already present as a raw
  column in master_cbec_train_dataset.csv (not engineered for this purpose),
  so it is not circular with the ranking target (target_log_export_value is
  INDIA's future export value; destination_import_growth_1y is the
  DESTINATION's own realized import growth in year T, a different quantity).

This script only reports whether origins split into more than one regime and
by how much. It does NOT run any new model and does NOT report a ranking
result. If origins do not separate into distinct regimes, that is reported
honestly and the paper must say "insufficient regime separation" rather than
force a narrative.
"""
import pandas as pd
import numpy as np

df = pd.read_csv("filtered_for_model/master_cbec_train_dataset.csv")

rows = []
for T in [2020, 2021, 2022, 2023]:
    sub = df[df["year"] == T]
    g = sub["destination_import_growth_1y"].dropna()
    rows.append({
        "origin": T,
        "n_rows": len(sub),
        "median_growth": float(g.median()),
        "mean_growth": float(g.mean()),
        "std_growth": float(g.std()),
        "pct_negative": float((g < 0).mean()),
        "import_volatility_mean": float(sub["import_volatility"].mean()),
    })

out = pd.DataFrame(rows)
print(out.to_string(index=False))
out.to_csv("outputs/regime_feasibility_check.csv", index=False)
print("\nwrote outputs/regime_feasibility_check.csv")
print("\nSpread across origins (max-min):")
print(f"  median_growth spread: {out.median_growth.max() - out.median_growth.min():.4f}")
print(f"  volatility spread:    {out.import_volatility_mean.max() - out.import_volatility_mean.min():.4f}")
