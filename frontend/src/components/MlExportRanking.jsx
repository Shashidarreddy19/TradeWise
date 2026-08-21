import React, { useState } from 'react';
import { TrendingUp, Globe, AlertCircle, Loader2, BarChart3, Award, DollarSign, Brain, Info } from 'lucide-react';
import { intelligenceApi } from '../services/api';

/**
 * ML Export Destination Ranking Component
 * 
 * Displays the v4 XGBRanker + XGBRegressor predictions for a given HS code.
 * Calls GET /api/v1/market-opportunity/{hsCode} which returns ML-ranked
 * destinations with opportunity scores, predicted export values, and model metadata.
 * 
 * Shows DATA_UNAVAILABLE honestly for products outside the model's scope.
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
    if (score >= 80) return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    if (score >= 60) return 'bg-sky-100 text-sky-700 border-sky-200';
    if (score >= 40) return 'bg-amber-100 text-amber-700 border-amber-200';
    return 'bg-slate-100 text-slate-600 border-slate-200';
  };

  const rankBadge = (rank) => {
    if (rank === 1) return 'bg-yellow-400 text-yellow-900';
    if (rank === 2) return 'bg-slate-300 text-slate-800';
    if (rank === 3) return 'bg-amber-600 text-amber-50';
    return 'bg-slate-100 text-slate-600';
  };

  return (
    <div className="w-full max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2.5 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg">
          <Brain className="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-slate-800">ML Export Destination Ranking</h2>
          <p className="text-xs text-slate-500">XGBRanker v4 (rank:pairwise) + XGBRegressor — 55 products × 10 destinations</p>
        </div>
      </div>

      {/* Input */}
      <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-sm mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-slate-600 mb-1.5">Product Name</label>
            <input
              type="text"
              value={productName}
              onChange={(e) => setProductName(e.target.value)}
              placeholder="e.g. Cotton T-Shirt"
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-200 focus:border-indigo-400 transition-all outline-none"
            />
          </div>
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-slate-600 mb-1.5">HS Code (6+ digits)</label>
            <input
              type="text"
              value={hsCode}
              onChange={(e) => setHsCode(e.target.value)}
              placeholder="e.g. 6109100000"
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-200 focus:border-indigo-400 transition-all outline-none font-mono"
            />
          </div>
          <div className="flex items-end">
            <button
              onClick={handleRank}
              disabled={loading}
              className="w-full px-5 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-sm font-semibold shadow-md hover:shadow-lg hover:from-indigo-700 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <TrendingUp className="w-4 h-4" />}
              {loading ? 'Ranking...' : 'Rank Destinations'}
            </button>
          </div>
        </div>
        {error && (
          <div className="mt-3 flex items-center gap-2 text-red-600 text-xs bg-red-50 p-3 rounded-xl border border-red-100">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            {error}
          </div>
        )}
      </div>

      {/* Results */}
      {rankings && (
        <div className="space-y-4">
          {/* Summary bar */}
          <div className="flex items-center justify-between p-4 rounded-xl bg-gradient-to-r from-indigo-50 to-purple-50 border border-indigo-100">
            <div className="flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-indigo-600" />
              <span className="text-sm font-semibold text-indigo-900">
                {rankings.totalCountries || rankings.rankings?.length || 0} destinations ranked
              </span>
              {productName && (
                <span className="text-xs text-indigo-600 bg-indigo-100 px-2 py-0.5 rounded-full font-medium">
                  {productName}
                </span>
              )}
            </div>
            <span className="text-xs text-indigo-500 font-medium">HS: {hsCode}</span>
          </div>

          {/* Ranking cards */}
          <div className="space-y-3">
            {(rankings.rankings || []).map((r, idx) => (
              <div
                key={idx}
                className="group p-4 rounded-2xl bg-white border border-slate-200 hover:border-indigo-200 hover:shadow-md transition-all"
              >
                <div className="flex items-center gap-4">
                  {/* Rank badge */}
                  <div className={`w-9 h-9 flex items-center justify-center rounded-full text-xs font-black ${rankBadge(r.rank)}`}>
                    #{r.rank}
                  </div>

                  {/* Country info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <Globe className="w-3.5 h-3.5 text-slate-400" />
                      <span className="text-sm font-bold text-slate-800 truncate">{r.country}</span>
                      {r.scoreSource === 'ML_MODEL_V4' && (
                        <span className="text-[10px] font-bold text-purple-600 bg-purple-50 px-1.5 py-0.5 rounded-md border border-purple-100">
                          ML v4
                        </span>
                      )}
                      {r.scoreSource === 'HEURISTIC_FALLBACK' && (
                        <span className="text-[10px] font-medium text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded-md">
                          Heuristic
                        </span>
                      )}
                    </div>
                    {/* Reasons */}
                    <div className="flex flex-wrap gap-1 mt-1.5">
                      {(r.reasons || []).slice(0, 3).map((reason, i) => (
                        <span key={i} className="text-[10px] text-slate-500 bg-slate-50 px-2 py-0.5 rounded-full border border-slate-100">
                          {reason}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* Metrics */}
                  <div className="flex items-center gap-3 flex-shrink-0">
                    {/* Opportunity Score */}
                    <div className="text-center">
                      <div className={`px-3 py-1.5 rounded-lg border text-xs font-black ${scoreColor(r.opportunityScore)}`}>
                        {r.opportunityScore}
                      </div>
                      <span className="text-[9px] text-slate-400 mt-0.5 block">Score</span>
                    </div>

                    {/* Predicted Export Value */}
                    {r.predictedExportValueUsd != null && r.predictedExportValueUsd > 0 && (
                      <div className="text-center">
                        <div className="flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1.5 rounded-lg border border-emerald-100">
                          <DollarSign className="w-3 h-3" />
                          {formatUsd(r.predictedExportValueUsd)}
                        </div>
                        <span className="text-[9px] text-slate-400 mt-0.5 block">Predicted</span>
                      </div>
                    )}

                    {/* ML Rank */}
                    {r.mlModelRank && (
                      <div className="text-center">
                        <div className="flex items-center gap-1 text-xs font-bold text-indigo-700 bg-indigo-50 px-2.5 py-1.5 rounded-lg border border-indigo-100">
                          <Award className="w-3 h-3" />
                          #{r.mlModelRank}
                        </div>
                        <span className="text-[9px] text-slate-400 mt-0.5 block">ML Rank</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Model info footer */}
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-100 flex items-start gap-3">
            <Info className="w-4 h-4 text-slate-400 mt-0.5 flex-shrink-0" />
            <div className="text-xs text-slate-500 space-y-1">
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
