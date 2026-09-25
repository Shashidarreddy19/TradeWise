import React, { useState } from 'react';
import { TrendingUp, Globe, AlertCircle, Loader2, BarChart3, Award, DollarSign, Brain, Info } from 'lucide-react';
import { intelligenceApi } from '../services';

/**
 * ML Export Destination Ranking Component
 * 
 * Displays the v4 XGBRanker + XGBRegressor predictions for a given HS code.
 * Calls GET /api/v1/market-opportunity/{hsCode} which returns ML-ranked
 * destinations with opportunity scores, predicted export values, and model metadata.
 */
export default function MlExportRanking({ hsCode: propHsCode, productName: propProductName }) {
  const [hsCode, setHsCode] = useState(propHsCode || '');
  const [productName, setProductName] = useState(propProductName || '');
  const [rankings, setRankings] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleRank = async () => {
    const code = hsCode.trim().replace(/\./g, '');
    if (!code || code.length < 6) {
      setError('Please enter a valid HS code (at least 6 digits).');
      return;
    }
    setLoading(true);
    setError('');
    setRankings(null);
    try {
      const res = await intelligenceApi.rankMarketOpportunity(code);
      setRankings(res);
    } catch (err) {
      setError(err.message || 'Failed to fetch rankings. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const formatUsd = (val) => {
    if (val == null || val === 0) return '—';
    if (val >= 1e9) return `$${(val / 1e9).toFixed(2)}B`;
    if (val >= 1e6) return `$${(val / 1e6).toFixed(1)}M`;
    if (val >= 1e3) return `$${(val / 1e3).toFixed(0)}K`;
    return `$${val.toFixed(0)}`;
  };

  const scoreColor = (score) => {
    if (score >= 80) return 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20';
    if (score >= 60) return 'bg-primary/10 text-primary border-primary/20';
    if (score >= 40) return 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20';
    return 'bg-muted text-muted-foreground border-border';
  };

  const rankBadge = (rank) => {
    if (rank === 1) return 'bg-amber-500 text-amber-950 font-bold';
    if (rank === 2) return 'bg-muted text-foreground font-semibold';
    if (rank === 3) return 'bg-primary/20 text-primary font-semibold';
    return 'bg-muted/60 text-muted-foreground';
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="p-2.5 rounded-xl bg-primary/10 border border-primary/20 text-primary">
          <Brain className="w-5 h-5" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-foreground">ML Export Destination Ranking</h2>
          <p className="text-xs text-muted-foreground">XGBRanker v4 (rank:pairwise) + XGBRegressor — 55 products × 10 destinations</p>
        </div>
      </div>

      {/* Input */}
      <div className="card-claude p-5">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-foreground mb-1.5">Product Name</label>
            <input
              type="text"
              value={productName}
              onChange={(e) => setProductName(e.target.value)}
              placeholder="e.g. Cotton T-Shirt"
              className="input-claude"
            />
          </div>
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-foreground mb-1.5">HS Code (6+ digits)</label>
            <input
              type="text"
              value={hsCode}
              onChange={(e) => setHsCode(e.target.value)}
              placeholder="e.g. 6109100000"
              className="input-claude font-mono"
            />
          </div>
          <div className="flex items-end">
            <button
              onClick={handleRank}
              disabled={loading}
              className="btn-primary w-full py-2.5 text-xs flex items-center justify-center gap-2"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <TrendingUp className="w-4 h-4" />}
              {loading ? 'Ranking...' : 'Rank Destinations'}
            </button>
          </div>
        </div>
        {error && (
          <div className="mt-3 flex items-center gap-2 text-destructive text-xs bg-destructive/10 p-3 rounded-xl border border-destructive/20">
            <AlertCircle className="w-4 h-4 shrink-0" />
            {error}
          </div>
        )}
      </div>

      {/* Results */}
      {rankings && (
        <div className="space-y-4">
          {/* Summary bar */}
          <div className="flex items-center justify-between p-4 rounded-xl card-claude">
            <div className="flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-primary" />
              <span className="text-sm font-semibold text-foreground">
                {rankings.totalCountries || rankings.rankings?.length || 0} destinations ranked
              </span>
              {productName && (
                <span className="text-xs text-primary bg-primary/10 px-2 py-0.5 rounded-full font-medium">
                  {productName}
                </span>
              )}
            </div>
            <span className="text-xs text-muted-foreground font-medium">HS: {hsCode}</span>
          </div>

          {/* Ranking cards */}
          <div className="space-y-3">
            {(rankings.rankings || []).map((r, idx) => (
              <div
                key={idx}
                className="card-claude p-4 hover:border-primary/40 transition-all"
              >
                <div className="flex items-center gap-4">
                  {/* Rank badge */}
                  <div className={`w-8 h-8 flex items-center justify-center rounded-full text-xs font-bold shrink-0 ${rankBadge(r.rank)}`}>
                    #{r.rank}
                  </div>

                  {/* Country info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <Globe className="w-3.5 h-3.5 text-muted-foreground" />
                      <span className="text-sm font-bold text-foreground truncate">{r.country}</span>
                      {r.scoreSource === 'ML_MODEL_V4' && (
                        <span className="text-[10px] font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded-md border border-primary/20">
                          ML v4
                        </span>
                      )}
                      {r.scoreSource === 'HEURISTIC_FALLBACK' && (
                        <span className="text-[10px] font-medium text-muted-foreground bg-muted px-1.5 py-0.5 rounded-md">
                          Heuristic
                        </span>
                      )}
                    </div>
                    {/* Reasons */}
                    <div className="flex flex-wrap gap-1 mt-1.5">
                      {(r.reasons || []).slice(0, 3).map((reason, i) => (
                        <span key={i} className="text-[10px] text-muted-foreground bg-muted/60 px-2 py-0.5 rounded-full border border-border">
                          {reason}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* Metrics */}
                  <div className="flex items-center gap-3 shrink-0">
                    {/* Opportunity Score */}
                    <div className="text-center">
                      <div className={`px-3 py-1.5 rounded-lg border text-xs font-bold ${scoreColor(r.opportunityScore)}`}>
                        {r.opportunityScore}
                      </div>
                      <span className="text-[9px] text-muted-foreground mt-0.5 block">Score</span>
                    </div>

                    {/* Predicted Export Value */}
                    {r.predictedExportValueUsd != null && r.predictedExportValueUsd > 0 && (
                      <div className="text-center">
                        <div className="flex items-center gap-1 text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2.5 py-1.5 rounded-lg border border-emerald-500/20">
                          <DollarSign className="w-3 h-3" />
                          {formatUsd(r.predictedExportValueUsd)}
                        </div>
                        <span className="text-[9px] text-muted-foreground mt-0.5 block">Predicted</span>
                      </div>
                    )}

                    {/* ML Rank */}
                    {r.mlModelRank && (
                      <div className="text-center">
                        <div className="flex items-center gap-1 text-xs font-bold text-primary bg-primary/10 px-2.5 py-1.5 rounded-lg border border-primary/20">
                          <Award className="w-3 h-3" />
                          #{r.mlModelRank}
                        </div>
                        <span className="text-[9px] text-muted-foreground mt-0.5 block">ML Rank</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Model info footer */}
          <div className="p-4 rounded-xl bg-muted/30 border border-border flex items-start gap-3">
            <Info className="w-4 h-4 text-muted-foreground mt-0.5 shrink-0" />
            <div className="text-xs text-muted-foreground space-y-1">
              <p><strong>Model:</strong> XGBRanker (rank:pairwise) v4 — Spearman rho 0.896 on 2023→2024 holdout. Predicted values from XGBRegressor v4.</p>
              <p><strong>Scope:</strong> 55 HS6 Indian export products × 10 trade-partner destinations. Products outside this scope show "Heuristic" (regulatory complexity fallback).</p>
              <p><strong>Training data:</strong> CEPII BACI bilateral trade (2017–2024), CEPII Gravity, World Bank WDI. Leakage-audited, time-aware CV.</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

