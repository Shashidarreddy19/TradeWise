"""
Generate the backend serving table from the WINNING v4 models.

Uses the most recent feature year (2024) to produce forward-looking predictions:
  - XGBRanker (winner on ranking)  -> ranking score + within-product rank + 0..100 opportunity score
  - XGBRegressor (calibrated value) -> predicted next-year export value in USD

Ranker scores are ordinal (not USD); the regressor supplies the calibrated USD.
Output CSV is written to the backend resources so Java can serve by lookup:
  backend/src/main/resources/ml/ml_export_rankings_v4.csv
Columns: hs6, destination_iso3, destination_name, model_rank, opportunity_score,
         ranker_score, predicted_export_usd, model_name
Scope: 55 HS6 x 10 model countries. Anything outside -> DATA_UNAVAILABLE in the API.
"""
import os, json, numpy as np, pandas as pd
import xgboost as xgb

OUT = "outputs"
FEATURE_YEAR = 2024  # latest features -> forward (2025) prediction

df = pd.read_csv(os.path.join("filtered_for_model", "master_cbec_train_dataset.csv"))
for col in ["hs2", "hs4", "hs6", "product_category", "destination_iso3"]:
    df[f"{col}_enc"] = df[col].astype("category").cat.codes

with open(os.path.join(OUT, "selected_features_v4.json")) as f:
    FEATURES = json.load(f)["kept_features"]

ranker = xgb.Booster(); ranker.load_model(os.path.join(OUT, "xgb_ranker_v4.json"))
regr = xgb.Booster(); regr.load_model(os.path.join(OUT, "xgb_regressor_v4.json"))

sub = df[df["year"] == FEATURE_YEAR].copy()
X = xgb.DMatrix(sub[FEATURES].fillna(0.0))
sub["ranker_score"] = ranker.predict(X)
sub["reg_log"] = regr.predict(X)
sub["predicted_export_usd"] = np.maximum(0.0, np.expm1(sub["reg_log"]))

# within-product (hs6) ranking + 0..100 opportunity score from the RANKER
sub["model_rank"] = sub.groupby("hs6")["ranker_score"].rank(ascending=False, method="min").astype(int)
def scale(s):
    lo, hi = s.min(), s.max()
    return (s - lo) / (hi - lo) * 100.0 if hi > lo else pd.Series(50.0, index=s.index)
sub["opportunity_score"] = sub.groupby("hs6")["ranker_score"].transform(scale).round(1)

out = sub[["hs6", "destination_iso3", "destination_name", "model_rank",
           "opportunity_score", "ranker_score", "predicted_export_usd"]].copy()
out["ranker_score"] = out["ranker_score"].round(5)
out["predicted_export_usd"] = out["predicted_export_usd"].round(2)
out["model_name"] = "XGBRanker(rank:pairwise) v4 [rank]; XGBRegressor v4 [value]"
out = out.sort_values(["hs6", "model_rank"]).reset_index(drop=True)

# Backend resource CSV: drop destination_name (contains commas) — Java maps by ISO3.
# All remaining fields are comma-free so naive CSV parsing in Java is safe.
backend_cols = ["hs6", "destination_iso3", "model_rank", "opportunity_score",
                "ranker_score", "predicted_export_usd", "model_name"]
dest = os.path.join("..", "backend", "src", "main", "resources", "ml")
os.makedirs(dest, exist_ok=True)
out_path = os.path.join(dest, "ml_export_rankings_v4.csv")
out[backend_cols].to_csv(out_path, index=False)
# full copy (with names) in outputs for the paper
out.to_csv(os.path.join(OUT, "ml_export_rankings_v4.csv"), index=False)

print(f"Wrote {len(out)} rows to {out_path}")
print(f"Products (HS6): {out['hs6'].nunique()} | Countries: {out['destination_iso3'].nunique()}")
print("\nSample (HS6 610910 cotton t-shirts):")
print(out[out['hs6'] == 610910][["destination_iso3","destination_name","model_rank","opportunity_score","predicted_export_usd"]].to_string(index=False))
