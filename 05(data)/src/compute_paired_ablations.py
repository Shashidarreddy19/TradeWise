import os
import json
import numpy as np
import pandas as pd
import scipy.stats as stats
import xgboost as xgb

SEEDS_ABLATION = [42, 101, 2024, 777, 999]

def run_paired_ablation_tests():
    data_path = os.path.join('filtered_for_model', 'master_cbec_train_dataset.csv')
    df = pd.read_csv(data_path)
    
    for col in ['hs2', 'hs4', 'hs6', 'product_category', 'destination_iso3']:
        df[f'{col}_enc'] = df[col].astype('category').cat.codes

    group_a = ['hs2_enc', 'hs4_enc', 'hs6_enc', 'product_category_enc']
    group_b = ['destination_total_imports_usd', 'destination_import_growth_1y', 'destination_import_growth_3y', 
               'num_supplying_countries', 'hhi_concentration', 'import_volatility', 'top_competitor_share_pct']
    group_c = ['india_export_value_usd', 'india_export_growth_1y', 'india_export_growth_3y',
               'india_market_share_pct', 'india_market_share_change_trailing', 'india_export_rank_among_suppliers']
    group_d = ['fta_wto_agreement', 'market_growth_x_fta']
    group_f = ['gdp_usd', 'gdp_per_capita_usd', 'population', 'distance_km', 'common_border', 'common_language']
    df['feature_year_num'] = df['year'] - 2017
    group_time = ['feature_year_num']
    group_eng = ['demand_per_capita', 'india_export_intensity', 'competition_intensity', 
                 'entry_cost_pct_gdpcap', 'entry_procedures_count', 'entry_time_days', 'entry_metrics_imputed']

    features_with_year = group_a + group_b + group_c + group_d + group_f + group_eng + group_time

    train_mask = (df['year'] >= 2017) & (df['year'] <= 2021)
    val_mask = (df['year'] == 2022)
    test_mask = (df['year'] == 2023)

    df_train = df[train_mask].copy()
    df_val = df[val_mask].copy()
    df_test = df[test_mask].copy()

    target_col = 'target_log_export_value'
    y_tr = df_train[target_col]
    y_val = df_val[target_col]
    y_test = df_test[target_col]

    def ndcg_at_k(actual_scores, predicted_scores, k=5):
        actual_scores = np.asarray(actual_scores)
        predicted_scores = np.asarray(predicted_scores)
        if len(actual_scores) == 0:
            return 0.0
        k = min(k, len(actual_scores))
        order_pred = np.argsort(predicted_scores)[::-1]
        order_ideal = np.argsort(actual_scores)[::-1]
        actual_sorted_by_pred = actual_scores[order_pred][:k]
        actual_sorted_ideal = actual_scores[order_ideal][:k]
        gains_pred = np.maximum(0, actual_sorted_by_pred)
        gains_ideal = np.maximum(0, actual_sorted_ideal)
        discounts = np.log2(np.arange(len(gains_pred)) + 2)
        dcg = np.sum(gains_pred / discounts)
        idcg = np.sum(gains_ideal / discounts)
        if idcg == 0:
            return 1.0 if dcg == 0 else 0.0
        return float(dcg / idcg)

    def top_k_overlap(actual_scores, predicted_scores, k=3):
        actual_scores = np.asarray(actual_scores)
        predicted_scores = np.asarray(predicted_scores)
        k = min(k, len(actual_scores))
        if k == 0:
            return 0.0
        top_actual_idx = set(np.argsort(actual_scores)[::-1][:k])
        top_pred_idx = set(np.argsort(predicted_scores)[::-1][:k])
        return len(top_actual_idx.intersection(top_pred_idx)) / float(k)

    def evaluate_rankings(df_subset, pred_col):
        rhos = []
        top3s = []
        for hs, group in df_subset.groupby('hs6'):
            if len(group) < 2:
                continue
            y_true = group[target_col].values
            y_pred = group[pred_col].values
            if np.std(y_true) > 1e-6 and np.std(y_pred) > 1e-6:
                r, _ = stats.spearmanr(y_true, y_pred)
                rhos.append(0.0 if np.isnan(r) else r)
            else:
                rhos.append(0.0)
            top3s.append(top_k_overlap(y_true, y_pred, k=3))
        return float(np.mean(rhos)), float(np.mean(top3s))

    all_ablation_configs = {
        'Full Model (All Features + Year)': features_with_year,
        'Model A: Trade Features Only (Group B+C)': group_b + group_c,
        'Model B: Trade + Macro (Group B+C+F)': group_b + group_c + group_f,
        'Model C: Full v1 Spec (Groups A+B+C+D+F)': group_a + group_b + group_c + group_d + group_f,
        'Drop India Baseline Export Value': [f for f in features_with_year if f != 'india_export_value_usd'],
        'Drop Destination Market Demand': [f for f in features_with_year if f != 'destination_total_imports_usd'],
        'Drop India Market Share %': [f for f in features_with_year if f != 'india_market_share_pct'],
        'Drop Geographic Distance (Gravity)': [f for f in features_with_year if f != 'distance_km'],
        'Drop Destination 1Y Import Growth': [f for f in features_with_year if f != 'destination_import_growth_1y'],
        'Drop Destination GDP Per Capita': [f for f in features_with_year if f != 'gdp_per_capita_usd']
    }

    seed_rhos = {name: [] for name in all_ablation_configs}
    seed_top3s = {name: [] for name in all_ablation_configs}
    seed_rmses = {name: [] for name in all_ablation_configs}

    for seed_val in SEEDS_ABLATION:
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
            'seed': seed_val
        }
        for name, feats in all_ablation_configs.items():
            dtr = xgb.DMatrix(df_train[feats].fillna(0.0), label=y_tr)
            dval = xgb.DMatrix(df_val[feats].fillna(0.0), label=y_val)
            dte = xgb.DMatrix(df_test[feats].fillna(0.0), label=y_test)
            m = xgb.train(xgb_params, dtr, num_boost_round=350, evals=[(dtr, 'train'), (dval, 'val')], early_stopping_rounds=35, verbose_eval=False)
            preds = m.predict(dte)
            df_test['p'] = preds
            r, t3 = evaluate_rankings(df_test, 'p')
            rmse = float(np.sqrt(np.mean((y_test.values - preds)**2)))
            seed_rhos[name].append(r)
            seed_top3s[name].append(t3)
            seed_rmses[name].append(rmse)

    full_seed_rhos = np.array(seed_rhos['Full Model (All Features + Year)'])
    print("Full Model per-seed rhos across seeds [42, 101, 2024, 777, 999]:", [round(x, 4) for x in full_seed_rhos])

    rows = []
    for name in all_ablation_configs:
        arr_rho = np.array(seed_rhos[name])
        arr_t3 = np.array(seed_top3s[name])
        arr_rmse = np.array(seed_rmses[name])
        
        mean_rho = float(np.mean(arr_rho))
        std_rho = float(np.std(arr_rho))
        mean_t3 = float(np.mean(arr_t3))
        std_t3 = float(np.std(arr_t3))
        mean_rmse = float(np.mean(arr_rmse))
        std_rmse = float(np.std(arr_rmse))
        
        if name == 'Full Model (All Features + Year)':
            paired_t = np.nan
            p_val = np.nan
            paired_w = np.nan
            p_val_w = np.nan
            delta_rho = 0.0
            status = 'Baseline Reference'
        else:
            diff = arr_rho - full_seed_rhos
            delta_rho = float(np.mean(diff))
            
            # Paired t-test
            t_res = stats.ttest_rel(arr_rho, full_seed_rhos)
            paired_t = float(t_res.statistic)
            p_val = float(t_res.pvalue)
            
            # Paired Wilcoxon
            try:
                w_res = stats.wilcoxon(arr_rho, full_seed_rhos, alternative='two-sided')
                paired_w = float(w_res.statistic)
                p_val_w = float(w_res.pvalue)
            except Exception:
                paired_w = np.nan
                p_val_w = 1.0
                
            if p_val < 0.05:
                status = f'Significant (p = {p_val:.4f})'
            else:
                status = f'Not significant (p = {p_val:.3f})'

        rows.append({
            'Ablation Experiment': name,
            'Spearman_rho (mean +/- std)': f"{mean_rho:.4f} +/- {std_rho:.4f}",
            'Top-3 Overlap (mean +/- std)': f"{mean_t3*100:.1f}% +/- {std_t3*100:.1f}%",
            'RMSE (mean +/- std)': f"{mean_rmse:.3f} +/- {std_rmse:.3f}",
            'Delta_rho (Paired Mean)': delta_rho,
            'Paired_t_stat': paired_t,
            'p_value (Paired t-test)': p_val,
            'Wilcoxon_W': paired_w,
            'p_value (Wilcoxon)': p_val_w,
            'Significance (alpha=0.05)': status
        })

    df_out = pd.DataFrame(rows)
    df_out.to_csv('outputs/ablation_study_5seeds.csv', index=False)
    
    print("\n" + "=" * 90)
    print("TABLE 2: ABLATION STUDY ACROSS 5 SEEDS WITH PAIRED SIGNIFICANCE TESTS (alpha=0.05)")
    print("=" * 90)
    print(df_out[['Ablation Experiment', 'Spearman_rho (mean +/- std)', 'Delta_rho (Paired Mean)', 'Paired_t_stat', 'p_value (Paired t-test)', 'Significance (alpha=0.05)']].to_string(index=False))

    # Print specific Full vs Model B and Full vs Model C results
    row_b = df_out[df_out['Ablation Experiment'] == 'Model B: Trade + Macro (Group B+C+F)'].iloc[0]
    row_c = df_out[df_out['Ablation Experiment'] == 'Model C: Full v1 Spec (Groups A+B+C+D+F)'].iloc[0]
    row_a = df_out[df_out['Ablation Experiment'] == 'Model A: Trade Features Only (Group B+C)'].iloc[0]

    print("\n" + "-" * 70)
    print("EXPLICIT HYPOTHESIS TEST RESULTS:")
    print(f"Full vs. Model B: Paired Delta = {row_b['Delta_rho (Paired Mean)']:+.4f}, t = {row_b['Paired_t_stat']:.3f}, p = {row_b['p_value (Paired t-test)']:.4f} -> {row_b['Significance (alpha=0.05)']}")
    print(f"Full vs. Model C: Paired Delta = {row_c['Delta_rho (Paired Mean)']:+.4f}, t = {row_c['Paired_t_stat']:.3f}, p = {row_c['p_value (Paired t-test)']:.4f} -> {row_c['Significance (alpha=0.05)']}")
    print(f"Full vs. Model A: Paired Delta = {row_a['Delta_rho (Paired Mean)']:+.4f}, t = {row_a['Paired_t_stat']:.3f}, p = {row_a['p_value (Paired t-test)']:.4f} -> {row_a['Significance (alpha=0.05)']}")
    print("-" * 70)

if __name__ == '__main__':
    run_paired_ablation_tests()
