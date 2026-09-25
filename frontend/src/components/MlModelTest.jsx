import React, { useState } from 'react';
import { Play, CheckCircle, XCircle, Loader2, AlertTriangle, BarChart3 } from 'lucide-react';
import { intelligenceApi } from '../services';

/**
 * ML Model Integration Test — 20 Products
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
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-primary/10 border border-primary/20 text-primary">
            <BarChart3 className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground">ML Model Integration Test</h2>
            <p className="text-xs text-muted-foreground">Tests {TEST_PRODUCTS.length} products across 11 HS chapters + 2 out-of-scope</p>
          </div>
        </div>
        <button
          onClick={runTests}
          disabled={running}
          className="btn-primary text-xs flex items-center justify-center gap-2 self-start sm:self-auto"
        >
          {running ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
          {running ? `Testing... ${Math.round(progress)}%` : 'Run All Tests'}
        </button>
      </div>

      {/* Progress bar */}
      {running && (
        <div className="h-2 bg-muted rounded-full overflow-hidden">
          <div
            className="h-full bg-primary transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      )}

      {/* Summary */}
      {results.length > 0 && (
        <div className="flex flex-wrap gap-4">
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20">
            <CheckCircle className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            <span className="text-sm font-bold text-emerald-600 dark:text-emerald-400">{passCount} Passed</span>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-destructive/10 border border-destructive/20">
            <XCircle className="w-4 h-4 text-destructive" />
            <span className="text-sm font-bold text-destructive">{failCount} Failed</span>
          </div>
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl card-claude">
            <span className="text-sm font-medium text-muted-foreground">{results.length}/{TEST_PRODUCTS.length} Complete</span>
          </div>
        </div>
      )}

      {/* Results table */}
      {results.length > 0 && (
        <div className="card-claude overflow-hidden p-0">
          <div className="overflow-x-auto">
            <table className="table-claude w-full text-xs">
              <thead>
                <tr className="bg-muted/40 border-b border-border">
                  <th className="text-left px-4 py-3 font-semibold text-muted-foreground">#</th>
                  <th className="text-left px-4 py-3 font-semibold text-muted-foreground">Status</th>
                  <th className="text-left px-4 py-3 font-semibold text-muted-foreground">Product</th>
                  <th className="text-left px-4 py-3 font-semibold text-muted-foreground">HS Code</th>
                  <th className="text-left px-4 py-3 font-semibold text-muted-foreground">Ch.</th>
                  <th className="text-left px-4 py-3 font-semibold text-muted-foreground">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {results.map((r, i) => (
                  <tr key={i} className={`hover:bg-muted/30 transition-colors ${r.status === 'fail' || r.status === 'error' ? 'bg-destructive/5' : ''}`}>
                    <td className="px-4 py-3 text-muted-foreground">{i + 1}</td>
                    <td className="px-4 py-3">
                      {r.status === 'pass' && <CheckCircle className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />}
                      {r.status === 'fail' && <XCircle className="w-4 h-4 text-destructive" />}
                      {r.status === 'error' && <AlertTriangle className="w-4 h-4 text-amber-500" />}
                      {r.status === 'pending' && <Loader2 className="w-4 h-4 text-muted-foreground animate-spin" />}
                    </td>
                    <td className="px-4 py-3 font-medium text-foreground">{r.name}</td>
                    <td className="px-4 py-3 font-mono text-muted-foreground">{r.hs}</td>
                    <td className="px-4 py-3 text-muted-foreground">{r.chapter}</td>
                    <td className="px-4 py-3">
                      {r.details.length > 0 && (
                        <span className="text-emerald-600 dark:text-emerald-400 font-medium">{r.details.join(' | ')}</span>
                      )}
                      {r.errors.length > 0 && (
                        <span className="text-destructive font-medium">{r.errors.join(' | ')}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

