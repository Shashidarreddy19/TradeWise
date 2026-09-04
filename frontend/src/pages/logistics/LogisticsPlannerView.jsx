import React, { useState } from 'react';
import { Truck, FileText, DollarSign, MapPin, Package, Check, AlertTriangle, ChevronDown } from 'lucide-react';
import { countryFlag, shippingTransitDays, getCustomsDocuments, estimateFreightCost } from './utils';

const COUNTRIES = [
  'United States', 'Germany', 'United Arab Emirates', 'Singapore',
  'United Kingdom', 'Hong Kong', 'Bangladesh', 'China', 'Saudi Arabia', 'Netherlands',
];

const SHIPPING_MODES = ['Sea Freight', 'Air Freight'];

export default function LogisticsPlannerView({ shipments }) {
  const [selectedCountry, setSelectedCountry] = useState('United States');
  const [selectedMode, setSelectedMode] = useState('Sea Freight');
  const [weightKg, setWeightKg] = useState(1000);
  const [hsCode, setHsCode] = useState('');
  const [showPlan, setShowPlan] = useState(false);

  const handleGenerate = () => setShowPlan(true);

  const docs = showPlan ? getCustomsDocuments(hsCode, selectedCountry) : [];
  const freight = showPlan ? estimateFreightCost(selectedMode, selectedCountry, weightKg) : null;
  const transitDays = showPlan ? shippingTransitDays(selectedMode, selectedCountry) : null;

  const mandatoryDocs = docs.filter(d => d.mandatory);
  const optionalDocs = docs.filter(d => !d.mandatory);

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Logistics Planner</h1>
        <p className="text-xs text-slate-500 mt-1 font-medium">Route planning, customs document checklist, and freight cost estimation for any shipment.</p>
      </div>

      {/* Planner Input */}
      <div className="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
        <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest mb-5">Plan a Shipment</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="space-y-1.5">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Destination Country</label>
            <div className="relative">
              <select value={selectedCountry} onChange={e => { setSelectedCountry(e.target.value); setShowPlan(false); }}
                className="w-full px-3.5 py-2.5 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer appearance-none">
                {COUNTRIES.map(c => <option key={c}>{c}</option>)}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 pointer-events-none" />
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Shipping Mode</label>
            <div className="relative">
              <select value={selectedMode} onChange={e => { setSelectedMode(e.target.value); setShowPlan(false); }}
                className="w-full px-3.5 py-2.5 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer appearance-none">
                {SHIPPING_MODES.map(m => <option key={m}>{m}</option>)}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 pointer-events-none" />
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Cargo Weight (kg)</label>
            <input type="number" min="1" value={weightKg} onChange={e => { setWeightKg(Number(e.target.value)); setShowPlan(false); }}
              className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500" />
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">HS Code (optional)</label>
            <input type="text" value={hsCode} onChange={e => { setHsCode(e.target.value); setShowPlan(false); }}
              placeholder="e.g. 09103020"
              className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-xs font-mono focus:outline-none focus:border-sky-500" />
          </div>
        </div>
        <button onClick={handleGenerate}
          className="mt-5 px-6 py-3 text-xs font-bold text-white bg-indigo-500 hover:bg-indigo-400 rounded-xl shadow transition-all cursor-pointer">
          Generate Logistics Plan
        </button>
      </div>

      {/* Results */}
      {showPlan && (
        <div className="space-y-5 animate-in fade-in duration-300">

          {/* Route Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <MapPin className="w-4 h-4 text-sky-500" />
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Route</span>
              </div>
              <div className="text-xs space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-slate-400">Origin</span>
                  <span className="font-bold text-slate-800">🇮🇳 India</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Destination</span>
                  <span className="font-bold text-slate-800">{countryFlag(selectedCountry)} {selectedCountry}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Mode</span>
                  <span className="font-bold text-slate-800">{selectedMode}</span>
                </div>
              </div>
            </div>

            <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <Truck className="w-4 h-4 text-indigo-500" />
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Transit Time</span>
              </div>
              <div className="text-2xl font-black text-indigo-600">{transitDays}</div>
              <div className="text-[10px] text-slate-400 mt-1 font-semibold">{selectedMode} · estimated</div>
            </div>

            <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <DollarSign className="w-4 h-4 text-emerald-500" />
                <span className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Freight Estimate</span>
              </div>
              <div className="text-xl font-black text-emerald-600">{freight?.estimated}</div>
              <div className="text-[10px] text-slate-400 mt-1 font-semibold">{freight?.mode}</div>
              {freight?.ratePerKg !== 'N/A (container rate)' && (
                <div className="text-[10px] text-slate-400 font-semibold">{freight?.ratePerKg}</div>
              )}
            </div>
          </div>

          {/* Customs Document Checklist */}
          <div className="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
            <div className="flex items-center gap-2 mb-5">
              <FileText className="w-4 h-4 text-indigo-500" />
              <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest">Customs Document Checklist</h3>
              <span className="ml-auto text-[10px] bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-full font-bold">{docs.length} total</span>
            </div>

            {/* Mandatory */}
            <div className="mb-4">
              <div className="flex items-center gap-1.5 mb-2.5">
                <AlertTriangle className="w-3.5 h-3.5 text-red-500" />
                <span className="text-[10px] font-black text-red-500 uppercase tracking-wider">Mandatory ({mandatoryDocs.length})</span>
              </div>
              <div className="space-y-2">
                {mandatoryDocs.map((doc, i) => (
                  <div key={i} className="flex items-start gap-3 p-3 rounded-xl border border-red-50 bg-red-50/30">
                    <div className="w-5 h-5 rounded-full bg-red-100 flex items-center justify-center shrink-0 mt-0.5">
                      <span className="text-[9px] font-black text-red-500">!</span>
                    </div>
                    <div>
                      <span className="block text-xs font-bold text-slate-800">{doc.name}</span>
                      <span className="block text-[10px] text-slate-500 mt-0.5">{doc.note}</span>
                    </div>
                    <span className="ml-auto text-[8px] bg-red-100 text-red-500 px-1.5 py-0.5 rounded font-bold shrink-0">Mandatory</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Optional */}
            {optionalDocs.length > 0 && (
              <div>
                <div className="flex items-center gap-1.5 mb-2.5">
                  <Check className="w-3.5 h-3.5 text-slate-400" />
                  <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider">Optional / Conditional ({optionalDocs.length})</span>
                </div>
                <div className="space-y-2">
                  {optionalDocs.map((doc, i) => (
                    <div key={i} className="flex items-start gap-3 p-3 rounded-xl border border-slate-100 bg-slate-50/30">
                      <div className="w-5 h-5 rounded-full bg-slate-100 flex items-center justify-center shrink-0 mt-0.5">
                        <span className="text-[9px] font-black text-slate-400">+</span>
                      </div>
                      <div>
                        <span className="block text-xs font-bold text-slate-700">{doc.name}</span>
                        <span className="block text-[10px] text-slate-400 mt-0.5">{doc.note}</span>
                      </div>
                      <span className="ml-auto text-[8px] bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded font-bold shrink-0">Optional</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <p className="text-[9px] text-slate-400 mt-4 italic border-t border-slate-100 pt-3">
              Document requirements based on HS code chapter and destination country. Verify with customs broker before shipment.
              {!hsCode && ' Add an HS code above for product-specific requirements.'}
            </p>
          </div>

          {/* Shipments using same route */}
          {shipments.filter(s => s.dest?.toLowerCase().includes(selectedCountry.toLowerCase())).length > 0 && (
            <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <Package className="w-4 h-4 text-sky-500" />
                <h3 className="text-[10px] font-black text-slate-500 uppercase tracking-wider">Your Shipments to {selectedCountry}</h3>
              </div>
              <div className="space-y-2">
                {shipments.filter(s => s.dest?.toLowerCase().includes(selectedCountry.toLowerCase())).map(s => (
                  <div key={s.id} className="flex items-center justify-between text-xs p-2.5 rounded-lg border border-slate-100">
                    <span className="font-mono text-slate-400">#{s.id}</span>
                    <span className="font-bold text-slate-800">{s.product}</span>
                    <span className={`text-[9px] font-black px-2 py-0.5 rounded-full ${s.rawStatus === 'DELIVERED' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>{s.status}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
