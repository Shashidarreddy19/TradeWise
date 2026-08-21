import os
import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from scipy.stats import spearmanr, kendalltau
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.linear_model import Ridge
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
import xgboost as xgb
import lightgbm as lgb
import shap

# Set random seed for reproducibility
RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

def ndcg_at_k(actual_scores, predicted_scores, k=5):
    """
    Computes NDCG@k for a single ranking list.
    actual_scores: array of true relevance / target values
    predicted_scores: array of predicted model scores
    """
    actual_scores = np.asarray(actual_scores)
    predicted_scores = np.asarray(predicted_scores)
    
    if len(actual_scores) == 0:
        return 0.0
    
    k = min(k, len(actual_scores))
    order_pred = np.argsort(predicted_scores)[::-1]
    order_ideal = np.argsort(actual_scores)[::-1]
    
    actual_sorted_by_pred = actual_scores[order_pred][:k]
    actual_sorted_ideal = actual_scores[order_ideal][:k]
    
    # Use standard 2^rel - 1 formulation for positive gain, or normalized linear gain
    # Linear gain with rank discounting is robust for continuous trade values
    gains_pred = np.maximum(0, actual_sorted_by_pred)
    gains_ideal = np.maximum(0, actual_sorted_ideal)
    
    discounts = np.log2(np.arange(len(gains_pred)) + 2)
    dcg = np.sum(gains_pred / discounts)
    idcg = np.sum(gains_ideal / discounts)
    
    if idcg == 0:
        return 1.0 if dcg == 0 else 0.0
    return float(dcg / idcg)

def top_k_overlap(actual_scores, predicted_scores, k=3):
    """
    Computes fraction/percentage of top-k actual items present in top-k predicted items.
    """
    actual_scores = np.asarray(actual_scores)
    predicted_scores = np.asarray(predicted_scores)
    k = min(k, len(actual_scores))
    if k == 0:
        return 0.0
    top_actual_idx = set(np.argsort(actual_scores)[::-1][:k])
    top_pred_idx = set(np.argsort(predicted_scores)[::-1][:k])
    overlap = len(top_actual_idx.intersection(top_pred_idx)) / float(k)
    return overlap

def top_1_hit_rate(actual_scores, predicted_scores):
    actual_scores = np.asarray(actual_scores)
    predicted_scores = np.asarray(predicted_scores)
    if len(actual_scores) == 0:
        return 0.0
    return 1.0 if np.argmax(actual_scores) == np.argmax(predicted_scores) else 0.0

def evaluate_rankings(df_subset, pred_col, target_col='target_log_export_value'):
    """
    Computes ranking metrics aggregated across all distinct HS6 products in df_subset.
    """
    spearman_list = []
    kendall_list = []
    ndcg3_list = []
    ndcg5_list = []
    ndcg10_list = []
    top1_list = []
    top3_list = []
    top5_list = []
    
    for hs, group in df_subset.groupby('hs6'):
        if len(group) < 2:
            continue
        y_true = group[target_col].values
        y_pred = group[pred_col].values
        
        # Spearman rho
        if np.std(y_true) > 1e-6 and np.std(y_pred) > 1e-6:
            rho, _ = spearmanr(y_true, y_pred)
            tau, _ = kendalltau(y_true, y_pred)
            if not np.isnan(rho):
                spearman_list.append(rho)
            if not np.isnan(tau):
                kendall_list.append(tau)
        else:
            spearman_list.append(0.0)
            kendall_list.append(0.0)
            
        ndcg3_list.append(ndcg_at_k(y_true, y_pred, k=3))
        ndcg5_list.append(ndcg_at_k(y_true, y_pred, k=5))
        ndcg10_list.append(ndcg_at_k(y_true, y_pred, k=10))
        top1_list.append(top_1_hit_rate(y_true, y_pred))
        top3_list.append(top_k_overlap(y_true, y_pred, k=3))
        top5_list.append(top_k_overlap(y_true, y_pred, k=5))
        
    return {
        'spearman_rho': float(np.mean(spearman_list)),
        'kendall_tau': float(np.mean(kendall_list)),
        'ndcg@3': float(np.mean(ndcg3_list)),
        'ndcg@5': float(np.mean(ndcg5_list)),
        'ndcg@10': float(np.mean(ndcg10_list)),
        'top1_hit_rate': float(np.mean(top1_list)),
        'top3_overlap': float(np.mean(top3_list)),
        'top5_overlap': float(np.mean(top5_list))
    }

def run_experiment_pipeline():
    print("=" * 70)
    print("STEP 2: MODEL TRAINING, EVALUATION & ABLATION STUDY")
    print("=" * 70)
    
    os.makedirs('outputs', exist_ok=True)
    os.makedirs('figures', exist_ok=True)
    
    # Load dataset
    data_path = os.path.join('filtered_for_model', 'master_cbec_train_dataset.csv')
    df = pd.read_csv(data_path)
    print(f"Loaded dataset: {df.shape[0]} rows, {df.shape[1]} columns.")
    
    # Define Feature Sets
    group_a = ['hs2_enc', 'hs4_enc', 'hs6_enc', 'product_category_enc']
    group_b = ['destination_total_imports_usd', 'destination_import_growth_1y', 'destination_import_growth_3y', 
               'num_supplying_countries', 'hhi_concentration', 'import_volatility', 'top_competitor_share_pct']
    group_c = ['india_export_value_usd', 'india_export_growth_1y', 'india_export_growth_3y',
               'india_market_share_pct', 'india_market_share_change_trailing', 'india_export_rank_among_suppliers']
    group_d = ['fta_wto_agreement', 'market_growth_x_fta'] # Group D (FTA/WTO agreement, note: WITS tariff rates marked out-of-scope for v1)
    group_f = ['gdp_usd', 'gdp_per_capita_usd', 'population', 'distance_km', 'common_border', 'common_language']
    group_eng = ['demand_per_capita', 'india_export_intensity', 'competition_intensity', 'entry_cost_pct_gdpcap', 'entry_procedures_count', 'entry_time_days', 'entry_metrics_imputed']
    
    # Encode categoricals
    for col in ['hs2', 'hs4', 'hs6', 'product_category', 'destination_iso3']:
        df[f"{col}_enc"] = df[col].astype('category').cat.codes
        
    all_features = group_a + group_b + group_c + group_d + group_f + group_eng
    print(f"Total feature count: {len(all_features)}")
    
    # Temporal Split:
    # Train: 2017 - 2021 (Predicting 2018 - 2022)
    # Val: 2022 (Predicting 2023)
    # Test: 2023 (Predicting 2024)
    # Live Demo: 2024 (Predicting 2025)
    train_mask = (df['year'] >= 2017) & (df['year'] <= 2021)
    val_mask = (df['year'] == 2022)
    test_mask = (df['year'] == 2023)
    demo_mask = (df['year'] == 2024)
    
    df_train = df[train_mask].copy()
    df_val = df[val_mask].copy()
    df_test = df[test_mask].copy()
    df_demo = df[demo_mask].copy()
    
    print(f"Train rows: {len(df_train)} | Val rows: {len(df_val)} | Test rows: {len(df_test)} | Demo rows: {len(df_demo)}")
    
    target_col = 'target_log_export_value'
    
    X_train, y_train = df_train[all_features], df_train[target_col]
    X_val, y_val = df_val[all_features], df_val[target_col]
    X_test, y_test = df_test[all_features], df_test[target_col]
    
    # Impute missing if any (safety check)
    X_train = X_train.fillna(0.0)
    X_val = X_val.fillna(0.0)
    X_test = X_test.fillna(0.0)
    
    # -------------------------------------------------------------------------
    # 1. BASELINE: Naive Historical Growth
    # -------------------------------------------------------------------------
    print("\n--- Running Baseline 1: Naive Historical Growth ---")
    # Assume T+1 = log(1 + T * (1 + trailing_1y_growth))
    pred_naive_raw = df_test['india_export_value_usd'] * (1.0 + df_test['india_export_growth_1y'].clip(lower=-0.9, upper=5.0))
    df_test['pred_naive'] = np.log1p(np.maximum(0, pred_naive_raw))
    
    # -------------------------------------------------------------------------
    # 2. BASELINE: Variant A (Heuristic Weighted Opportunity Score)
    # -------------------------------------------------------------------------
    print("--- Running Baseline 2: Variant A (Heuristic Weighted Score) ---")
    # Weighted composite: 0.35 * normalized India export + 0.25 * import growth + 0.20 * market share + 0.20 * (1/distance)
    norm_exp = df_test.groupby('hs6')['india_export_value_usd'].transform(lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
    norm_growth = df_test.groupby('hs6')['destination_import_growth_1y'].transform(lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
    norm_share = df_test.groupby('hs6')['india_market_share_pct'].transform(lambda s: (s - s.min()) / (s.max() - s.min() + 1e-5))
    norm_dist = 1.0 - (df_test['distance_km'] / 12000.0)
    df_test['pred_variant_a'] = (0.40 * norm_exp + 0.25 * norm_growth + 0.20 * norm_share + 0.15 * norm_dist) * 20.0
    
    # -------------------------------------------------------------------------
    # 3. BASELINE: Linear Regression (Ridge)
    # -------------------------------------------------------------------------
    print("--- Running Baseline 3: Ridge Linear Regression ---")
    model_ridge = Ridge(alpha=1.0, random_state=RANDOM_SEED)
    model_ridge.fit(X_train, y_train)
    df_test['pred_ridge'] = model_ridge.predict(X_test)
    
    # -------------------------------------------------------------------------
    # 4. BASELINE: Random Forest Regressor
    # -------------------------------------------------------------------------
    print("--- Running Baseline 4: Random Forest Regressor ---")
    model_rf = RandomForestRegressor(n_estimators=150, max_depth=8, min_samples_leaf=3, random_state=RANDOM_SEED, n_jobs=-1)
    model_rf.fit(X_train, y_train)
    df_test['pred_rf'] = model_rf.predict(X_test)
    
    # -------------------------------------------------------------------------
    # 5. BASELINE: Gradient Boosting Regressor (sklearn)
    # -------------------------------------------------------------------------
    print("--- Running Baseline 5: Sklearn Gradient Boosting ---")
    model_gbr = GradientBoostingRegressor(n_estimators=120, learning_rate=0.05, max_depth=5, random_state=RANDOM_SEED)
    model_gbr.fit(X_train, y_train)
    df_test['pred_gbr'] = model_gbr.predict(X_test)
    
    # -------------------------------------------------------------------------
    # 6. MODEL: Variant B (LightGBM Regressor)
    # -------------------------------------------------------------------------
    print("--- Running Variant B: LightGBM Regressor ---")
    lgb_train = lgb.Dataset(X_train, y_train)
    lgb_val = lgb.Dataset(X_val, y_val, reference=lgb_train)
    
    lgb_params = {
        'objective': 'regression',
        'metric': 'rmse',
        'boosting_type': 'gbdt',
        'learning_rate': 0.05,
        'max_depth': 6,
        'num_leaves': 31,
        'min_child_samples': 15,
        'subsample': 0.8,
        'colsample_bytree': 0.8,
        'random_state': RANDOM_SEED,
        'verbose': -1
    }
    
    evals_result_lgb = {}
    model_lgb = lgb.train(
        lgb_params,
        lgb_train,
        num_boost_round=300,
        valid_sets=[lgb_train, lgb_val],
        valid_names=['train', 'val'],
        callbacks=[
            lgb.early_stopping(stopping_rounds=30, verbose=False),
            lgb.record_evaluation(evals_result_lgb)
        ]
    )
    df_test['pred_lightgbm'] = model_lgb.predict(X_test, num_iteration=model_lgb.best_iteration)
    
    # -------------------------------------------------------------------------
    # 7. PRIMARY MODEL: XGBoost Regressor (Tuned with Val Early Stopping)
    # -------------------------------------------------------------------------
    print("--- Running Primary Model: Tuned XGBoost Regressor ---")
    dtrain = xgb.DMatrix(X_train, label=y_train)
    dval = xgb.DMatrix(X_val, label=y_val)
    dtest = xgb.DMatrix(X_test, label=y_test)
    
    xgb_params = {
        'objective': 'reg:squarederror',
        'eval_metric': 'rmse',
        'learning_rate': 0.04,
        'max_depth': 5,
        'min_child_weight': 4,
        'subsample': 0.85,
        'colsample_bytree': 0.85,
        'reg_alpha': 0.1,
        'reg_lambda': 1.0,
        'seed': RANDOM_SEED
    }
    
    evals_result_xgb = {}
    model_xgb = xgb.train(
        xgb_params,
        dtrain,
        num_boost_round=350,
        evals=[(dtrain, 'train'), (dval, 'val')],
        early_stopping_rounds=35,
        evals_result=evals_result_xgb,
        verbose_eval=False
    )
    df_test['pred_xgboost'] = model_xgb.predict(dtest)
    
    # -------------------------------------------------------------------------
    # METRIC COMPUTATIONS & COMPARISON TABLE
    # -------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print("MODEL COMPARISON & BENCHMARKING RESULTS ON TEST SET (2023 -> 2024)")
    print("=" * 70)
    
    models_dict = {
        'Naive Historical Growth': 'pred_naive',
        'Variant A (Heuristic Weighted)': 'pred_variant_a',
        'Linear Regression (Ridge)': 'pred_ridge',
        'Random Forest': 'pred_rf',
        'Gradient Boosting (sklearn)': 'pred_gbr',
        'Variant B (LightGBM)': 'pred_lightgbm',
        'Primary XGBoost (CBEC-AI v2)': 'pred_xgboost'
    }
    
    benchmark_results = []
    for model_name, pred_col in models_dict.items():
        y_true = df_test[target_col]
        y_pred = df_test[pred_col]
        
        # Regression metrics
        mae = mean_absolute_error(y_true, y_pred)
        rmse = np.sqrt(mean_squared_error(y_true, y_pred))
        r2 = r2_score(y_true, y_pred) if model_name != 'Variant A (Heuristic Weighted)' else np.nan
        
        # Back-transformed USD MAPE on active export flows (> $10k)
        active_mask = df_test['target_future_export_value_usd'] > 10000
        raw_true = df_test.loc[active_mask, 'target_future_export_value_usd']
        raw_pred = np.expm1(y_pred[active_mask]).clip(lower=0)
        mape = np.mean(np.abs((raw_true - raw_pred) / (raw_true + 1e-5))) * 100.0 if model_name != 'Variant A (Heuristic Weighted)' else np.nan
        
        # Ranking metrics
        rank_metrics = evaluate_rankings(df_test, pred_col, target_col)
        
        res = {
            'Model': model_name,
            'Spearman_rho': rank_metrics['spearman_rho'],
            'NDCG@5': rank_metrics['ndcg@5'],
            'NDCG@10': rank_metrics['ndcg@10'],
            'Top-3 Overlap': rank_metrics['top3_overlap'],
            'Top-1 Hit Rate': rank_metrics['top1_hit_rate'],
            'Kendall_tau': rank_metrics['kendall_tau'],
            'RMSE (Log)': rmse,
            'MAE (Log)': mae,
            'R2': r2
        }
        benchmark_results.append(res)
        
    df_benchmark = pd.DataFrame(benchmark_results)
    df_benchmark.to_csv('outputs/benchmark_comparison.csv', index=False)
    print(df_benchmark.to_string(index=False))
    
    # -------------------------------------------------------------------------
    # ABLATION STUDY
    # -------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print("STEP 3: ABLATION STUDY EXPERIMENTS")
    print("=" * 70)
    
    ablation_records = []
    
    # 1. Feature Group Ablations
    group_experiments = {
        'Full Model (All Features)': all_features,
        'Model A: Trade Features Only (Group B+C)': group_b + group_c,
        'Model B: Trade + Macro (Group B+C+F)': group_b + group_c + group_f,
        'Model C: Full v1 Spec (Groups A+B+C+D+F)': group_a + group_b + group_c + group_d + group_f
    }
    
    for exp_name, feat_sub in group_experiments.items():
        dtr = xgb.DMatrix(X_train[feat_sub], label=y_train)
        dte = xgb.DMatrix(X_test[feat_sub], label=y_test)
        m = xgb.train(xgb_params, dtr, num_boost_round=model_xgb.best_iteration or 150, verbose_eval=False)
        preds = m.predict(dte)
        df_test['tmp_pred'] = preds
        rm = evaluate_rankings(df_test, 'tmp_pred', target_col)
        rmse = np.sqrt(mean_squared_error(y_test, preds))
        r2 = r2_score(y_test, preds)
        
        ablation_records.append({
            'Ablation Experiment': exp_name,
            'Features Dropped / Config': 'None (Baseline)' if 'Full' in exp_name else 'Restricted Subset',
            'Spearman_rho': rm['spearman_rho'],
            'NDCG@5': rm['ndcg@5'],
            'NDCG@10': rm['ndcg@10'],
            'Top-3 Overlap': rm['top3_overlap'],
            'RMSE': rmse,
            'R2': r2
        })
        
    # 2. Key 6 Features Dropped One-At-A-Time (Image Matrix #5 requirement)
    key_6_features = [
        ('india_export_value_usd', 'India Baseline Export Value'),
        ('destination_total_imports_usd', 'Destination Market Demand'),
        ('india_market_share_pct', 'India Market Share %'),
        ('distance_km', 'Geographic Distance (Gravity)'),
        ('destination_import_growth_1y', 'Destination 1Y Import Growth'),
        ('gdp_per_capita_usd', 'Destination GDP Per Capita')
    ]
    
    for feat_col, feat_desc in key_6_features:
        feat_sub = [f for f in all_features if f != feat_col]
        dtr = xgb.DMatrix(X_train[feat_sub], label=y_train)
        dte = xgb.DMatrix(X_test[feat_sub], label=y_test)
        m = xgb.train(xgb_params, dtr, num_boost_round=model_xgb.best_iteration or 150, verbose_eval=False)
        preds = m.predict(dte)
        df_test['tmp_pred'] = preds
        rm = evaluate_rankings(df_test, 'tmp_pred', target_col)
        rmse = np.sqrt(mean_squared_error(y_test, preds))
        r2 = r2_score(y_test, preds)
        
        ablation_records.append({
            'Ablation Experiment': f"Drop {feat_desc}",
            'Features Dropped / Config': f"Dropped: {feat_col}",
            'Spearman_rho': rm['spearman_rho'],
            'NDCG@5': rm['ndcg@5'],
            'NDCG@10': rm['ndcg@10'],
            'Top-3 Overlap': rm['top3_overlap'],
            'RMSE': rmse,
            'R2': r2
        })
        
    df_ablation = pd.DataFrame(ablation_records)
    df_ablation.to_csv('outputs/ablation_study.csv', index=False)
    print(df_ablation.to_string(index=False))
    
    # -------------------------------------------------------------------------
    # STEP 4: VISUALIZATIONS & IMAGE MATRICES
    # -------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print("STEP 4: GENERATING FIGURES, LOSS CURVES & FEATURE IMPORTANCE")
    print("=" * 70)
    
    # 1. Train / Val Loss Curve (Image Matrix #7)
    train_loss = evals_result_xgb['train']['rmse']
    val_loss = evals_result_xgb['val']['rmse']
    rounds = range(1, len(train_loss) + 1)
    
    plt.figure(figsize=(8, 5), dpi=300)
    plt.plot(rounds, train_loss, label='Train RMSE', color='#1f77b4', lw=2)
    plt.plot(rounds, val_loss, label='Validation RMSE', color='#ff7f0e', lw=2)
    plt.axvline(x=model_xgb.best_iteration, color='red', linestyle='--', label=f'Best Iteration ({model_xgb.best_iteration})')
    plt.title('CBEC-AI Model Convergence: Training vs. Validation Loss (RMSE)', fontsize=13, pad=12, fontweight='bold')
    plt.xlabel('Boosting Rounds', fontsize=11)
    plt.ylabel('Loss (RMSE)', fontsize=11)
    plt.grid(True, linestyle=':', alpha=0.6)
    plt.legend(frameon=True, facecolor='white', framealpha=0.9)
    plt.tight_layout()
    plt.savefig('figures/train_val_loss_curve.png')
    plt.close()
    print("Saved figure: figures/train_val_loss_curve.png")
    
    # 2. Feature Importance (Gain & Split Importance) (Image Matrix #6)
    importance_gain = model_xgb.get_score(importance_type='gain')
    importance_weight = model_xgb.get_score(importance_type='weight')
    
    df_imp = pd.DataFrame([
        {'feature': k, 'gain': v, 'split': importance_weight.get(k, 0)}
        for k, v in importance_gain.items()
    ]).sort_values(by='gain', ascending=False)
    df_imp.to_csv('outputs/feature_importance.csv', index=False)
    
    plt.figure(figsize=(10, 6), dpi=300)
    top_15_imp = df_imp.head(15).sort_values(by='gain', ascending=True)
    bars = plt.barh(top_15_imp['feature'], top_15_imp['gain'], color='#2b5c8f', edgecolor='black', alpha=0.85)
    plt.title('Top 15 Feature Importance by Total Gain (XGBoost)', fontsize=13, pad=12, fontweight='bold')
    plt.xlabel('Feature Gain', fontsize=11)
    plt.ylabel('Feature Name', fontsize=11)
    plt.grid(axis='x', linestyle=':', alpha=0.6)
    plt.tight_layout()
    plt.savefig('figures/feature_importance_gain.png')
    plt.close()
    print("Saved figure: figures/feature_importance_gain.png")
    
    # 3. SHAP Summary Plot
    print("Computing SHAP values with TreeExplainer...")
    explainer = shap.TreeExplainer(model_xgb)
    shap_values = explainer.shap_values(X_test)
    
    plt.figure(figsize=(10, 6), dpi=300)
    shap.summary_plot(shap_values, X_test, show=False, max_display=15)
    plt.title('SHAP Feature Contribution Summary (Test Set 2023->2024)', fontsize=13, pad=12, fontweight='bold')
    plt.tight_layout()
    plt.savefig('figures/shap_summary_plot.png')
    plt.close()
    print("Saved figure: figures/shap_summary_plot.png")
    
    # -------------------------------------------------------------------------
    # STEP 5: SHAP CASE STUDIES (Local Explanations)
    # -------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print("STEP 5: EXTRACTING LOCAL SHAP EXPLANATIONS FOR KEY PRODUCTS")
    print("=" * 70)
    
    case_studies = []
    test_indices = df_test.index.tolist()
    
    # Select sample cases: Pharmaceuticals (300490), Rice (100630), Electronics (851712), Mineral Fuels (271000)
    sample_queries = [
        (300490, 'USA', 'Medicaments (Pharma)'),
        (100630, 'SAU', 'Semi/Wholly Milled Rice'),
        (851712, 'ARE', 'Smartphones / Telco'),
        (271000, 'SGP', 'Refined Petroleum'),
        (610910, 'DEU', 'Cotton T-Shirts (Apparel)')
    ]
    
    for hs_target, iso_target, desc in sample_queries:
        row_match = df_test[(df_test['hs6'] == hs_target) & (df_test['destination_iso3'] == iso_target)]
        if len(row_match) > 0:
            idx_in_test = df_test.index.get_loc(row_match.index[0])
            shap_row = shap_values[idx_in_test]
            feat_names = X_test.columns.tolist()
            
            top_pos_idx = np.argsort(shap_row)[::-1][:3]
            top_neg_idx = np.argsort(shap_row)[:3]
            
            pos_drivers = [(feat_names[i], round(float(shap_row[i]), 3), round(float(X_test.iloc[idx_in_test, i]), 3)) for i in top_pos_idx if shap_row[i] > 0]
            neg_drivers = [(feat_names[i], round(float(shap_row[i]), 3), round(float(X_test.iloc[idx_in_test, i]), 3)) for i in top_neg_idx if shap_row[i] < 0]
            
            case_studies.append({
                'hs6': hs_target,
                'product': desc,
                'destination': iso_target,
                'actual_export_usd': float(row_match['target_future_export_value_usd'].values[0]),
                'pred_log_value': float(row_match['pred_xgboost'].values[0]),
                'positive_drivers': pos_drivers,
                'negative_drivers': neg_drivers
            })
            
    with open('outputs/shap_case_studies.json', 'w') as f:
        json.dump(case_studies, f, indent=2)
    print("Saved SHAP case studies to outputs/shap_case_studies.json")
    
    # -------------------------------------------------------------------------
    # STEP 6: FORWARD INFERENCE DEMONSTRATION (2024 -> 2025 Prediction)
    # -------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print("STEP 6: LIVE FORWARD INFERENCE DEMO (2024 Features -> 2025 Targets)")
    print("=" * 70)
    X_demo = df_demo[all_features].fillna(0.0)
    df_demo['predicted_2025_log_export'] = model_xgb.predict(xgb.DMatrix(X_demo))
    df_demo['predicted_2025_export_usd'] = np.expm1(df_demo['predicted_2025_log_export']).clip(lower=0)
    
    # Rank destinations per product for 2025
    df_demo['predicted_2025_rank'] = df_demo.groupby('hs6')['predicted_2025_export_usd'].rank(ascending=False, method='min')
    demo_summary = df_demo[['hs6', 'product_category', 'destination_iso3', 'destination_name', 'india_export_value_usd', 'predicted_2025_export_usd', 'predicted_2025_rank']].sort_values(by=['hs6', 'predicted_2025_rank'])
    demo_summary.to_csv('outputs/forward_predictions_2025.csv', index=False)
    print("Saved forward 2025 predictions to outputs/forward_predictions_2025.csv")
    
    # Save final results summary dict
    primary_res = df_benchmark[df_benchmark['Model'] == 'Primary XGBoost (CBEC-AI v2)'].iloc[0].to_dict()
    with open('outputs/final_metrics_summary.json', 'w') as f:
        json.dump(primary_res, f, indent=2)
        
    print("\nRESEARCH PIPELINE COMPLETED SUCCESSFULLY!")

if __name__ == '__main__':
    run_experiment_pipeline()
