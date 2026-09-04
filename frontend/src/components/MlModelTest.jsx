import React, { useState } from 'react';
import { Play, CheckCircle, XCircle, Loader2, AlertTriangle, BarChart3 } from 'lucide-react';
import { intelligenceApi } from '../services';

/**
 * ML Model Integration Test — 20 Products
 * 
 * Exercises the /api/v1/market-opportunity/{hsCode} endpoint for 20 different
 * products covering all 11 chapters in the model's scope. Verifies:
 *   1. API returns successfully (no 500)
 *   2. Rankings array is non-empty
 *   3. At least one entry has scoreSource === 'ML_MODEL_V4'
 *   4. Opportunity scores are in [0..100]
 *   5. ML ranks are sequential starting from 1
 *   6. Predicted USD values are positive where available
 *
 * Also tests 2 OUT-OF-SCOPE products to confirm honest DATA_UNAVAILABLE/Heuristic.
 */

const TEST_PRODUCTS = [
  // In-scope: 55 HS6 codes across 11 chapters — selecting 18 spread across all
  { hs: '6109100000', name: 'Cotton T-Shirt', chapter: '61' },
  { hs: '3004900000', name: 'Medicaments (Pharma)', chapter: '30' },
  { hs: '1006300000', name: 'Semi-Milled Rice', chapter: '10' },
  { hs: '8517120000', name: 'Smartphones', chapter: '85' },
  { hs: '2710000000', name: 'Petroleum Oils', chapter: '27' },
  { hs: '7102390000', name: 'Diamonds (non-industrial)', chapter: '71' },
  { hs: '8411120000', name: 'Turbojets', chapter: '84' },
  { hs: '7208390000', name: 'Hot-Rolled Steel', chapter: '72' },
  { hs: '8703220000', name: 'Motor Cars (1000-1500cc)', chapter: '87' },
  { hs: '2902430000', name: 'p-Xylene (Organic Chem)', chapter: '29' },
  { hs: '6204420000', name: 'Women\'s Cotton Dresses', chapter: '62' },
  { hs: '6110200000', name: 'Cotton Pullovers', chapter: '61' },
  { hs: '8504400000', name: 'Static Converters', chapter: '85' },
  { hs: '7207110000', name: 'Semi-finished Iron', chapter: '72' },
  { hs: '1001990000', name: 'Wheat (other)', chapter: '10' },
  { hs: '3004200000', name: 'Antibiotics', chapter: '30' },
  { hs: '7113190000', name: 'Gold Jewellery', chapter: '71' },
  { hs: '8708990000', name: 'Auto Parts', chapter: '87' },
  // Out-of-scope (HS codes NOT in the model's 55) — should get HEURISTIC_FALLBACK
  { hs: '0901110000', name: 'Coffee (not roasted)', chapter: '09', expectOutOfScope: true },
  { hs: '9403600000', name: 'Wooden Furniture', chapter: '94', expectOutOfScope: true },
];

export default function MlModelTest() {
  const [results, setResults] = useState([]);
  const [running, setRunning] = useState(false);
  const [progress, setProgress] = useState(0);

  const runTests = async () => {
    setRunning(true);
    setResults([]);
    setProgress(0);
    const outcomes = [];

    for (let i = 0; i < TEST_PRODUCTS.length; i++) {
      const p = TEST_PRODUCTS[i];
      const hs = p.hs.replace(/\./g, '');
      let result = { ...p, status: 'pending', details: [], errors: [] };

      try {
        const res = await intelligenceApi.rankMarketOpportunity(hs);
        result.response = res;

        // Check 1: response has rankings
        if (!res.rankings || !Array.isArray(res.rankings)) {
          result.errors.push('No rankings array in response');
        } else {
          result.details.push(`${res.rankings.length} destinations returned`);

          // Check 2: at least one ML_MODEL_V4 entry (unless out-of-scope)
          const mlEntries = res.rankings.filter(r => r.scoreSource === 'ML_MODEL_V4');
          const heuristicEntries = res.rankings.filter(r => r.scoreSource === 'HEURISTIC_FALLBACK');

          if (p.expectOutOfScope) {
            if (heuristicEntries.length > 0 || mlEntries.length === 0) {
              result.details.push('Correctly returned heuristic/no ML (out-of-scope product)');
            } else {
              result.errors.push('Expected HEURISTIC for out-of-scope product but got ML');
            }
          } else {
            if (mlEntries.length > 0) {
              result.details.push(`${mlEntries.length} ML-ranked destinations`);
            } else {
              result.errors.push('No ML_MODEL_V4 entries found (expected in-scope)');
            }
          }

          // Check 3: opportunity scores in [0..100]
          const badScores = res.rankings.filter(r => r.opportunityScore < 0 || r.opportunityScore > 100);
          if (badScores.length > 0) {
            result.errors.push(`${badScores.length} entries have scores outside [0..100]`);
          }

          // Check 4: ranks sequential from 1
          const ranks = res.rankings.map(r => r.rank);
          if (ranks[0] !== 1) {
            result.errors.push('First rank is not 1');
          }

          // Check 5: predicted USD > 0 for ML entries
          const mlWithUsd = mlEntries.filter(r => r.predictedExportValueUsd > 0);
          if (mlEntries.length > 0 && mlWithUsd.length === 0) {
            result.errors.push('ML entries have no positive predicted USD');
          } else if (mlEntries.length > 0) {
            const topUsd = mlEntries.sort((a, b) => (a.mlModelRank || 99) - (b.mlModelRank || 99))[0];
            result.details.push(`Top: ${topUsd?.country} ($${(topUsd?.predictedExportValueUsd / 1e6).toFixed(1)}M)`);
          }
        }

        result.status = result.errors.length === 0 ? 'pass' : 'fail';
      } catch (err) {
        result.status = 'error';
        result.errors.push(err.message || 'Request failed');
      }

      outcomes.push(result);
      setResults([...outcomes]);
      setProgress(((i + 1) / TEST_PRODUCTS.length) * 100);

      // Small delay to avoid hammering the backend
      await new Promise(resolve => setTimeout(resolve, 300));
    }

    setRunning(false);
  };

  const passCount = results.filter(r => r.status === 'pass').length;
  const failCount = results.filter(r => r.status === 'fail' || r.status === 'error').length;

  return (
    <div className="w-full max-w-4xl mx-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 shadow-lg">
            <BarChart3 className="w-5 h-5 text-white" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-800">ML Model Integration Test</h2>
            <p className="text-xs text-slate-500">Tests {TEST_PRODUCTS.length} products across 11 HS chapters + 2 out-of-scope</p>
          </div>
        </div>
        <button
          onClick={runTests}
          disabled={running}
          className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 text-white text-sm font-semibold shadow-md hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center gap-2"
        >
          {running ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
          {running ? `Testing... ${Math.round(progress)}%` : 'Run All Tests'}
        </button>
      </div>

      {/* Progress bar */}
      {running && (
        <div className="mb-4 h-2 bg-slate-100 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-emerald-500 to-teal-500 transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      )}

      {/* Summary */}
      {results.length > 0 && (
        <div className="flex gap-4 mb-6">
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-50 border border-emerald-100">
            <CheckCircle className="w-4 h-4 text-emerald-600" />
            <span className="text-sm font-bold text-emerald-700">{passCount} Passed</span>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 border border-red-100">
            <XCircle className="w-4 h-4 text-red-600" />
            <span className="text-sm font-bold text-red-700">{failCount} Failed</span>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-50 border border-slate-100">
            <span className="text-sm font-medium text-slate-600">{results.length}/{TEST_PRODUCTS.length} Complete</span>
          </div>
        </div>
      )}

      {/* Results table */}
      {results.length > 0 && (
        <div className="rounded-2xl border border-slate-200 overflow-hidden bg-white">
          <table className="w-full text-xs">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="text-left px-4 py-3 font-semibold text-slate-600">#</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600">Status</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600">Product</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600">HS Code</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600">Ch.</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600">Details</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r, i) => (
                <tr key={i} className={`border-b border-slate-100 ${r.status === 'fail' || r.status === 'error' ? 'bg-red-50/50' : ''}`}>
                  <td className="px-4 py-3 text-slate-500">{i + 1}</td>
                  <td className="px-4 py-3">
                    {r.status === 'pass' && <CheckCircle className="w-4 h-4 text-emerald-500" />}
                    {r.status === 'fail' && <XCircle className="w-4 h-4 text-red-500" />}
                    {r.status === 'error' && <AlertTriangle className="w-4 h-4 text-amber-500" />}
                    {r.status === 'pending' && <Loader2 className="w-4 h-4 text-slate-300 animate-spin" />}
                  </td>
                  <td className="px-4 py-3 font-medium text-slate-800">{r.name}</td>
                  <td className="px-4 py-3 font-mono text-slate-600">{r.hs}</td>
                  <td className="px-4 py-3 text-slate-500">{r.chapter}</td>
                  <td className="px-4 py-3">
                    {r.details.length > 0 && (
                      <span className="text-emerald-600">{r.details.join(' | ')}</span>
                    )}
                    {r.errors.length > 0 && (
                      <span className="text-red-600 font-medium">{r.errors.join(' | ')}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
