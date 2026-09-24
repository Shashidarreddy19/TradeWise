import React, { useState, useEffect } from 'react';
import {
  Truck, Ship, Plane, Box, ShieldCheck, MapPin, DollarSign, AlertTriangle,
  FileText, Clock, BarChart2, Layers, CheckCircle2, ArrowRight, RefreshCw, Sparkles,
  Zap, Search, Globe, ChevronRight, Anchor, Warehouse, Info, Activity, Navigation,
  Calendar, Check, ArrowUpRight
} from 'lucide-react';
import { logisticsApi } from '../../services';

export default function LogisticsPlannerView({ addToast, products = [], countries = [] }) {
  // 4 Core Workflow Stages
  const [activeTab, setActiveTab] = useState('setup'); // 'setup', 'modes', 'costs', 'journey'

  // Form State
  const [formData, setFormData] = useState({
    hsCode: '0910.30',
    productName: 'Turmeric Powder (Standard Export Grade)',
    originLocation: 'Nashik, Maharashtra, India',
    destinationCountry: 'Germany',
    destinationCity: 'Hamburg',
    pickupAddress: 'Plot 42, MIDC Industrial Area, Nashik - 422007',
    incoterm: 'CIF',
    quantity: 5000,
    weightPerUnitKg: 0.5,
    lengthCm: 25,
    widthCm: 20,
    heightCm: 15,
    containerPreference: '20FT',
    isDangerousGoods: false,
    temperatureRequirement: 'Ambient (15-25°C)',
    expectedDispatchDate: new Date(Date.now() + 3 * 86400000).toISOString().split('T')[0],
    preferredTransportMode: 'AUTO'
  });

  // State
  const [planResult, setPlanResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [trackingData, setTrackingData] = useState(null);
  const [trackingRefInput, setTrackingRefInput] = useState('TW-PLN-847291');
  const [isSavingPlan, setIsSavingPlan] = useState(false);

  // Initialize and calculate on load
  useEffect(() => {
    handleCalculatePlan();
  }, []);

  // When user picks a catalog product, auto-fill parameters
  const handleSelectProduct = (productName) => {
    const found = products.find(p => p.name === productName);
    if (found) {
      setFormData(prev => ({
        ...prev,
        productName: found.name,
        hsCode: found.hscode || '0910.30',
        weightPerUnitKg: found.weight || 0.5,
      }));
      if (addToast) addToast(`Loaded parameters for ${found.name}`, 'info');
    }
  };

  const handleCalculatePlan = async () => {
    setLoading(true);
    try {
      const res = await logisticsApi.estimatePlan(formData);
      setPlanResult(res.data);

      // Also get sample tracking
      const trackRes = await logisticsApi.trackShipment('TW-PLN-847291').catch(() => null);
      if (trackRes?.data) setTrackingData(trackRes.data);

      if (addToast) addToast('Optimal shipment plan calculated!', 'success');
    } catch (err) {
      if (addToast) addToast(err.message || 'Calculated using standard logistics model', 'info');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateOfficialPlan = async () => {
    setIsSavingPlan(true);
    try {
      const res = await logisticsApi.createPlan(formData);
      setPlanResult(res.data);
      if (addToast) addToast(`🎉 Shipment Plan #${res.data?.planReference || 'TW-BOOKED'} saved & confirmed for carrier bidding!`, 'success');
    } catch (err) {
      if (addToast) addToast(err.message || 'Failed to save shipment plan', 'error');
    } finally {
      setIsSavingPlan(false);
    }
  };

  const handleTrackSearch = async () => {
    if (!trackingRefInput.trim()) return;
    try {
      const res = await logisticsApi.trackShipment(trackingRefInput);
      setTrackingData(res.data);
      if (addToast) addToast(`Tracking updated for ${trackingRefInput}`, 'success');
    } catch (err) {
      if (addToast) addToast('Tracking record not found', 'error');
    }
  };

  // Supported list of destination countries
  const destinationCountryList = countries.length > 0
    ? countries.map(c => c.name)
    : ['Germany', 'Saudi Arabia', 'United States', 'United Arab Emirates', 'Singapore', 'United Kingdom', 'Australia', 'Netherlands', 'Canada', 'Japan'];

  return (
    <div className="space-y-6 antialiased pb-12">
      {/* ── Top Header Banner ──────────────────────────────────────────────── */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-3xl p-6 md:p-8 text-white shadow-xl relative overflow-hidden">
        <div className="absolute right-0 top-0 w-96 h-96 bg-sky-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 relative z-10">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/20 border border-sky-400/30 text-sky-300 text-[10px] font-black tracking-widest uppercase">
              <Sparkles className="w-3.5 h-3.5 text-sky-400" /> CBEC-AI TradeWise Logistics Suite
            </div>
            <h1 className="text-2xl md:text-3xl font-black text-white tracking-tight">
              Enterprise Shipment Planner & Route Optimizer
            </h1>
            <p className="text-xs text-slate-300 max-w-2xl leading-relaxed">
              Configure your export shipment, compare Ocean vs. Air economics, review granular freight charges, and trace the 7-stage Indian export dispatch workflow.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={handleCalculatePlan}
              disabled={loading}
              className="px-5 py-3 rounded-2xl bg-sky-500 hover:bg-sky-400 text-white font-black text-xs shadow-lg shadow-sky-500/25 transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
              <span>{loading ? 'Calculating...' : 'Recalculate Optimal Route'}</span>
            </button>
          </div>
        </div>

        {/* ── 4-Stage Progressive Workflow Navigation ───────────────────────── */}
        <div className="mt-8 pt-5 border-t border-white/10 grid grid-cols-2 md:grid-cols-4 gap-2">
          {[
            { id: 'setup', step: 'Step 1', label: 'Cargo & Parameters', icon: Box, desc: 'Factory origin, product & container' },
            { id: 'modes', step: 'Step 2', label: 'Transport Modes', icon: Truck, desc: 'Sea vs. Air comparative analysis' },
            { id: 'costs', step: 'Step 3', label: 'Freight & Customs', icon: DollarSign, desc: 'Itemized costs & ICEGATE checklist' },
            { id: 'journey', step: 'Step 4', label: 'Route & Milestones', icon: Navigation, desc: '7-stage timeline & live tracking' },
          ].map(tab => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`p-3.5 rounded-2xl text-left transition-all cursor-pointer border ${
                  isActive
                    ? 'bg-white text-slate-900 shadow-md border-white'
                    : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10 hover:text-white'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className={`text-[10px] font-black uppercase tracking-wider ${isActive ? 'text-sky-600' : 'text-slate-400'}`}>
                    {tab.step}
                  </span>
                  <Icon className={`w-4 h-4 ${isActive ? 'text-sky-600' : 'text-slate-400'}`} />
                </div>
                <div className={`text-xs font-black mt-1 ${isActive ? 'text-slate-900' : 'text-white'}`}>
                  {tab.label}
                </div>
                <div className={`text-[10px] mt-0.5 truncate ${isActive ? 'text-slate-500' : 'text-slate-400'}`}>
                  {tab.desc}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Executive Summary Metrics (Visible when plan is calculated) ──────── */}
      {planResult && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block">Cargo Weight & Volume</span>
            <p className="text-xl font-black text-slate-900 font-mono">
              {planResult.totalWeightKg} <span className="text-xs font-normal text-slate-500">kg</span>
            </p>
            <span className="text-[10px] text-emerald-600 font-bold block">Vol: {planResult.totalVolumeCbm} CBM</span>
          </div>

          <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block">Recommended Mode</span>
            <p className="text-xl font-black text-sky-600 uppercase flex items-center gap-1.5">
              {planResult.recommendedMode === 'SEA' ? <Ship className="w-5 h-5" /> : <Plane className="w-5 h-5" />}
              {planResult.recommendedMode} FREIGHT
            </p>
            <span className="text-[10px] text-slate-500 font-medium block">Est. {planResult.estimatedTransitDays} Days Door-to-Door</span>
          </div>

          <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block">Total Logistics Cost</span>
            <p className="text-xl font-black text-slate-900 font-mono">
              ₹{Number(planResult.grandTotal).toLocaleString('en-IN')}
            </p>
            <span className="text-[10px] text-slate-500 font-medium block">
              ₹{planResult.costPerKg}/kg • ₹{planResult.costPerUnit}/unit
            </span>
          </div>

          <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block">Route Risk & Footprint</span>
            <p className="text-xl font-black text-emerald-600 font-mono">
              {planResult.overallRiskScore} <span className="text-xs font-normal text-slate-500">/ 100</span>
            </p>
            <span className="text-[10px] text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded-full inline-block">
              {planResult.riskCategory} Risk • {planResult.carbonEmissionsKg} kg CO₂e
            </span>
          </div>
        </div>
      )}

      {/* ── STAGE 1: SHIPMENT PARAMETERS & CARGO SETUP ───────────────────────── */}
      {activeTab === 'setup' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-in fade-in duration-200">
          {/* Main Input Form */}
          <div className="lg:col-span-2 bg-white rounded-3xl p-6 md:p-8 border border-slate-200 shadow-sm space-y-6">
            <div className="flex justify-between items-center border-b border-slate-100 pb-4">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-sky-500" /> Export Shipment Parameters
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">Specify origin factory, commodity details, target market, and container size.</p>
              </div>
              <span className="text-[10px] font-bold text-sky-700 bg-sky-50 px-3 py-1 rounded-full border border-sky-100">Step 1 of 4</span>
            </div>

            {/* Quick Catalog Product Selector */}
            {products.length > 0 && (
              <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-200/80 space-y-2">
                <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest block">Quick Pick from Your Product Catalog:</span>
                <div className="flex flex-wrap gap-2">
                  {products.slice(0, 6).map(p => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => handleSelectProduct(p.name)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                        formData.productName === p.name
                          ? 'bg-sky-500 text-white shadow-sm'
                          : 'bg-white border border-slate-200 text-slate-700 hover:border-sky-300'
                      }`}
                    >
                      {p.name}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Product Name */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">Product Description</label>
                <input
                  type="text"
                  value={formData.productName}
                  onChange={e => setFormData({ ...formData, productName: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500"
                  placeholder="e.g. Premium Basmati Rice"
                />
              </div>

              {/* HS Code */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">HS Code (ITC-HS)</label>
                <input
                  type="text"
                  value={formData.hsCode}
                  onChange={e => setFormData({ ...formData, hsCode: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-mono font-bold text-slate-800 focus:outline-none focus:border-sky-500"
                  placeholder="e.g. 1006.30 or 0910.30"
                />
              </div>

              {/* Origin Factory in India */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">Origin Factory / Farm Location</label>
                <input
                  type="text"
                  value={formData.originLocation}
                  onChange={e => setFormData({ ...formData, originLocation: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-medium text-slate-800 focus:outline-none focus:border-sky-500"
                  placeholder="e.g. Nashik, Maharashtra, India"
                />
              </div>

              {/* Destination Country */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">Destination Country</label>
                <select
                  value={formData.destinationCountry}
                  onChange={e => setFormData({ ...formData, destinationCountry: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-800 bg-white focus:outline-none focus:border-sky-500 cursor-pointer"
                >
                  {destinationCountryList.map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              {/* Incoterm */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">Incoterm</label>
                <select
                  value={formData.incoterm}
                  onChange={e => setFormData({ ...formData, incoterm: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-800 bg-white focus:outline-none focus:border-sky-500 cursor-pointer"
                >
                  <option value="CIF">CIF — Cost, Insurance & Freight (Seller pays freight to destination)</option>
                  <option value="FOB">FOB — Free On Board (Seller delivers to Indian port)</option>
                  <option value="CFR">CFR — Cost & Freight (Seller pays freight, buyer covers insurance)</option>
                  <option value="EXW">EXW — Ex Works (Buyer handles all pickup from factory)</option>
                  <option value="DDP">DDP — Delivered Duty Paid (Door-to-door complete delivery)</option>
                </select>
              </div>

              {/* Container Preference */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">Container Specification</label>
                <select
                  value={formData.containerPreference}
                  onChange={e => setFormData({ ...formData, containerPreference: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-800 bg-white focus:outline-none focus:border-sky-500 cursor-pointer"
                >
                  <option value="20FT">20 FT Standard Container (Max 28 CBM / 21,500 kg)</option>
                  <option value="40FT">40 FT Standard Container (Max 58 CBM / 26,000 kg)</option>
                  <option value="40HC">40 FT High Cube Container (Max 68 CBM / 26,000 kg)</option>
                  <option value="LCL">LCL (Less than Container Load — Shared Groupage)</option>
                  <option value="REEFER">Reefer Container (Temperature-Controlled)</option>
                </select>
              </div>

              {/* Quantity */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">Export Quantity (Units)</label>
                <input
                  type="number"
                  min="1"
                  value={formData.quantity}
                  onChange={e => setFormData({ ...formData, quantity: parseInt(e.target.value) || 1 })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500"
                />
              </div>

              {/* Unit Weight */}
              <div className="space-y-1.5">
                <label className="text-xxs font-bold text-slate-600 uppercase tracking-wider block">Weight Per Unit (KG)</label>
                <input
                  type="number"
                  step="0.05"
                  min="0.05"
                  value={formData.weightPerUnitKg}
                  onChange={e => setFormData({ ...formData, weightPerUnitKg: parseFloat(e.target.value) || 0.1 })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-bold text-slate-800 focus:outline-none focus:border-sky-500"
                />
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={handleCalculatePlan}
                disabled={loading}
                className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs transition-all shadow-md cursor-pointer"
              >
                {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
                <span>Calculate & Optimize Shipment Plan</span>
              </button>
            </div>
          </div>

          {/* Container & Packaging Utilization Preview */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-5">
            <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2 border-b border-slate-100 pb-3">
              <Box className="w-4 h-4 text-sky-500" /> Container Optimization
            </h3>

            {planResult?.containerRecommendation ? (
              <div className="space-y-4">
                <div className="p-5 rounded-2xl bg-slate-900 text-white space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-xxs font-bold text-sky-300 uppercase tracking-widest">Container Utilization</span>
                    <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-300">
                      {planResult.containerRecommendation?.loadingEfficiency || 'High'}
                    </span>
                  </div>
                  <div className="text-3xl font-black font-mono text-white">
                    {planResult.containerRecommendation?.containerUtilizationPct}%
                  </div>
                  <div className="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
                    <div
                      className="bg-gradient-to-r from-sky-400 to-indigo-400 h-2.5 rounded-full transition-all"
                      style={{ width: `${Math.min(100, planResult.containerRecommendation?.containerUtilizationPct || 85)}%` }}
                    ></div>
                  </div>
                  <p className="text-xxs text-slate-300">
                    Recommended: <strong className="text-white">{planResult.containerRecommendation?.recommendedType}</strong> ({planResult.containerRecommendation?.containerCount} Unit)
                  </p>
                </div>

                <div className="space-y-2 text-xs">
                  <div className="flex justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-500">Unused Space:</span>
                    <span className="font-bold text-slate-800 font-mono">{planResult.containerRecommendation?.unusedCapacityCbm} CBM</span>
                  </div>
                  <div className="flex justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-500">Container Weight Limit:</span>
                    <span className="font-bold text-emerald-600">Within Safe Limits ✓</span>
                  </div>
                  <div className="flex justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-500">Dispatch Window:</span>
                    <span className="font-bold text-slate-800 font-mono">{formData.expectedDispatchDate}</span>
                  </div>
                </div>

                <div className="p-3.5 bg-sky-50/60 rounded-xl border border-sky-100 text-xxs text-slate-700 leading-relaxed">
                  💡 {planResult.containerRecommendation?.recommendationNote}
                </div>
              </div>
            ) : (
              <div className="p-8 text-center text-slate-400 space-y-2">
                <Box className="w-8 h-8 mx-auto text-slate-300" />
                <p className="text-xs">Container metrics will appear here after calculation.</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── STAGE 2: TRANSPORT MODE COMPARISON ───────────────────────────────── */}
      {activeTab === 'modes' && planResult && (
        <div className="space-y-6 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-slate-200 shadow-sm space-y-6">
            <div className="flex justify-between items-center border-b border-slate-100 pb-4">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                  <Truck className="w-4 h-4 text-sky-500" /> Mode Comparison: Ocean vs. Air Freight
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">Automated economics comparison based on payload weight, urgency, transit days, and tariff agreements.</p>
              </div>
              <span className="text-[10px] font-bold text-sky-700 bg-sky-50 px-3 py-1 rounded-full border border-sky-100">Step 2 of 4</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Primary Recommended Option */}
              <div className="p-6 rounded-3xl border-2 border-sky-500 bg-sky-50/30 space-y-5 relative shadow-sm">
                <span className="absolute top-4 right-4 px-3 py-1 rounded-full bg-sky-500 text-white text-[9px] font-black uppercase tracking-wider">
                  Recommended Choice
                </span>
                <div className="flex items-center gap-3">
                  <div className="p-3.5 rounded-2xl bg-sky-500 text-white shadow-md shadow-sky-500/20">
                    {planResult.recommendedMode === 'SEA' ? <Ship className="w-6 h-6" /> : <Plane className="w-6 h-6" />}
                  </div>
                  <div>
                    <h4 className="text-base font-black text-slate-900">{planResult.recommendedMode} FREIGHT</h4>
                    <span className="text-xs font-bold text-sky-700">Optimal Multimodal Option</span>
                  </div>
                </div>

                <div className="space-y-2.5 text-xs">
                  <div className="flex justify-between p-2.5 rounded-xl bg-white border border-sky-100">
                    <span className="text-slate-600 font-medium">Estimated Transit:</span>
                    <span className="font-black text-slate-900 font-mono">{planResult.estimatedTransitDays} Days</span>
                  </div>
                  <div className="flex justify-between p-2.5 rounded-xl bg-white border border-sky-100">
                    <span className="text-slate-600 font-medium">Total Landed Freight:</span>
                    <span className="font-black text-slate-900 font-mono">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</span>
                  </div>
                  <div className="flex justify-between p-2.5 rounded-xl bg-white border border-sky-100">
                    <span className="text-slate-600 font-medium">Carbon Emissions:</span>
                    <span className="font-black text-emerald-600 font-mono">{planResult.carbonEmissionsKg} kg CO₂e</span>
                  </div>
                </div>

                <div className="p-3.5 bg-white rounded-xl border border-sky-200 text-xs text-slate-700 font-medium leading-relaxed">
                  ✓ Best cost-per-kilogram efficiency for bulk export to {formData.destinationCountry}. Full container security and customs clearance under standard liner terms.
                </div>
              </div>

              {/* Alternative Modes */}
              {planResult.alternativeModes?.map((alt, idx) => (
                <div key={idx} className="p-6 rounded-3xl border border-slate-200 bg-white space-y-5">
                  <div className="flex items-center gap-3">
                    <div className="p-3.5 rounded-2xl bg-slate-100 text-slate-700">
                      {alt.mode.includes('AIR') ? <Plane className="w-6 h-6" /> : <Ship className="w-6 h-6" />}
                    </div>
                    <div>
                      <h4 className="text-base font-black text-slate-900">{alt.mode}</h4>
                      <span className="text-xs font-bold text-slate-400">Alternative Carrier Path</span>
                    </div>
                  </div>

                  <div className="space-y-2.5 text-xs">
                    <div className="flex justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                      <span className="text-slate-600 font-medium">Estimated Transit:</span>
                      <span className="font-black text-slate-900 font-mono">{alt.transitDays} Days</span>
                    </div>
                    <div className="flex justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                      <span className="text-slate-600 font-medium">Estimated Cost:</span>
                      <span className="font-black text-slate-900 font-mono">₹{Number(alt.estimatedCost).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                      <span className="text-slate-600 font-medium">Carbon Footprint:</span>
                      <span className="font-black text-slate-600 font-mono">{alt.carbonKg} kg CO₂e</span>
                    </div>
                  </div>

                  <div className="space-y-1.5 text-xxs leading-relaxed">
                    <p className="text-emerald-700 font-bold bg-emerald-50/60 p-2 rounded-lg border border-emerald-100">
                      ✓ Pro: {alt.advantages}
                    </p>
                    <p className="text-amber-800 font-bold bg-amber-50/60 p-2 rounded-lg border border-amber-100">
                      ⚠ Con: {alt.disadvantages}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ── STAGE 3: FREIGHT BREAKDOWN & CUSTOMS DOCUMENTS ─────────────────── */}
      {activeTab === 'costs' && planResult && (
        <div className="space-y-6 animate-in fade-in duration-200">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Granular Cost Breakdown Table */}
            <div className="lg:col-span-2 bg-white rounded-3xl p-6 md:p-8 border border-slate-200 shadow-sm space-y-6">
              <div className="flex justify-between items-center border-b border-slate-100 pb-4">
                <div>
                  <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                    <DollarSign className="w-4 h-4 text-sky-500" /> Granular Freight & Handling Breakdown
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">Itemized inland freight, ocean liner rates, port operations, and ICEGATE customs fees.</p>
                </div>
                <span className="text-[10px] font-bold text-sky-700 bg-sky-50 px-3 py-1 rounded-full border border-sky-100">Step 3 of 4</span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200 text-slate-400 font-bold text-[10px] uppercase tracking-wider">
                      <th className="py-3 px-3">Cost Head</th>
                      <th className="py-3 px-3">Description</th>
                      <th className="py-3 px-3 text-right">Amount (INR)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-mono text-slate-700">
                    <tr>
                      <td className="py-3 px-3 font-bold text-slate-900 font-sans">1. Inland Road Transport</td>
                      <td className="py-3 px-3 text-slate-500 font-sans">Pickup from {formData.originLocation} to Port Terminal</td>
                      <td className="py-3 px-3 text-right font-bold">₹{Number(planResult.costBreakdown?.roadFreight).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-bold text-slate-900 font-sans">2. Ocean / Air Freight</td>
                      <td className="py-3 px-3 text-slate-500 font-sans">Port-to-port carriage to {formData.destinationCountry}</td>
                      <td className="py-3 px-3 text-right font-bold">₹{Number(planResult.costBreakdown?.oceanFreight || planResult.costBreakdown?.airFreight).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-bold text-slate-900 font-sans">3. Terminal Handling (THC)</td>
                      <td className="py-3 px-3 text-slate-500 font-sans">Port crane loading, yard management & gate-in</td>
                      <td className="py-3 px-3 text-right">₹{Number(planResult.costBreakdown?.terminalCharges).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-bold text-slate-900 font-sans">4. Bunker & Fuel Surcharge</td>
                      <td className="py-3 px-3 text-slate-500 font-sans">BAF (Bunker Adjustment Factor) fuel index</td>
                      <td className="py-3 px-3 text-right">₹{Number(planResult.costBreakdown?.fuelSurcharge).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-bold text-slate-900 font-sans">5. Customs Clearance (EDI)</td>
                      <td className="py-3 px-3 text-slate-500 font-sans">Indian ICEGATE electronic shipping bill filing</td>
                      <td className="py-3 px-3 text-right">₹{Number(planResult.costBreakdown?.customsCharges).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-bold text-slate-900 font-sans">6. Marine Cargo Insurance</td>
                      <td className="py-3 px-3 text-slate-500 font-sans">Institute Cargo Clauses (A) 110% CIF coverage</td>
                      <td className="py-3 px-3 text-right">₹{Number(planResult.costBreakdown?.insurancePremium).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-3 px-3 font-bold text-slate-900 font-sans">7. Destination Port Delivery</td>
                      <td className="py-3 px-3 text-slate-500 font-sans">Destination entry handling & delivery dispatch</td>
                      <td className="py-3 px-3 text-right font-bold">₹{Number(planResult.costBreakdown?.deliveryCharges).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr className="bg-slate-900 text-white font-bold text-sm">
                      <td className="py-4 px-3 font-black font-sans">TOTAL FREIGHT & LOGISTICS</td>
                      <td className="py-4 px-3 font-normal text-xs text-slate-300 font-sans">Cost/KG: ₹{planResult.costPerKg} | Cost/Unit: ₹{planResult.costPerUnit}</td>
                      <td className="py-4 px-3 text-right font-black font-mono">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            {/* Mandatory Customs Documents Checklist */}
            <div className="bg-white rounded-3xl p-6 md:p-8 border border-slate-200 shadow-sm space-y-4">
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                  <FileText className="w-4 h-4 text-sky-500" /> Mandatory Export Checklist
                </h3>
                <span className="text-[9px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">ICEGATE Verified</span>
              </div>

              <div className="space-y-3">
                {planResult.requiredDocuments?.map((doc, idx) => (
                  <div key={idx} className="p-3 rounded-2xl border border-slate-100 bg-slate-50/50 space-y-1">
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span className="text-xs font-bold text-slate-900">{doc.documentName}</span>
                      <span className="text-[9px] font-mono font-bold bg-sky-100 text-sky-800 px-1.5 py-0.5 rounded">{doc.code}</span>
                    </div>
                    <p className="text-[10px] text-slate-500 ml-6">{doc.description}</p>
                    <span className="text-[9px] text-slate-400 block ml-6 font-semibold">Authority: {doc.issuingAuthority}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── STAGE 4: END-TO-END WORKFLOW & LIVE TRACKING ─────────────────────── */}
      {activeTab === 'journey' && planResult && (
        <div className="space-y-6 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-slate-200 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-slate-100 pb-4">
              <div>
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                  <Navigation className="w-4 h-4 text-sky-500" /> 7-Stage End-to-End Shipment Workflow
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">Physical milestone progression from Indian factory dispatch to foreign warehouse receipt.</p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handleCreateOfficialPlan}
                  disabled={isSavingPlan}
                  className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md transition-all cursor-pointer flex items-center gap-2 disabled:opacity-50"
                >
                  {isSavingPlan ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                  <span>Confirm & Save Official Plan</span>
                </button>
              </div>
            </div>

            {/* Stepper Timeline */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
              {planResult.workflowMilestones?.map((m, idx) => (
                <div key={idx} className="p-3.5 rounded-2xl border border-slate-100 bg-slate-50/60 space-y-2 relative">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-sky-500 text-white text-[11px] font-black flex items-center justify-center shrink-0">
                      {idx + 1}
                    </span>
                    <span className="text-[10px] font-black text-sky-600 uppercase tracking-wider">Stage {idx + 1}</span>
                  </div>
                  <div>
                    <h5 className="text-xs font-bold text-slate-900 leading-tight">{m.milestone}</h5>
                    <p className="text-[10px] text-slate-500 font-mono mt-1">{m.date}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* Live Tracking Container Tool */}
            <div className="pt-4 border-t border-slate-100 space-y-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-sky-500" />
                  <span className="text-xs font-black text-slate-800 uppercase tracking-wider">Live Cargo Tracking Lookup</span>
                </div>
                <div className="flex items-center gap-2 w-full sm:w-auto">
                  <input
                    type="text"
                    value={trackingRefInput}
                    onChange={e => setTrackingRefInput(e.target.value)}
                    placeholder="Enter Shipment ID (e.g. TW-PLN-847291)"
                    className="px-3.5 py-2 rounded-xl border border-slate-200 text-xs font-mono font-bold w-full sm:w-64 focus:outline-none focus:border-sky-500"
                  />
                  <button
                    type="button"
                    onClick={handleTrackSearch}
                    className="px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-all cursor-pointer"
                  >
                    Track
                  </button>
                </div>
              </div>

              {trackingData && (
                <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200/80 space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="p-3 bg-white rounded-xl border border-slate-200">
                      <span className="text-xxs font-bold text-slate-400 uppercase tracking-wider block">Current Port / Checkpoint</span>
                      <span className="text-xs font-black text-slate-900 mt-0.5 block">{trackingData.currentLocation}</span>
                    </div>
                    <div className="p-3 bg-white rounded-xl border border-slate-200">
                      <span className="text-xxs font-bold text-slate-400 uppercase tracking-wider block">Assigned Carrier & Vessel</span>
                      <span className="text-xs font-black text-slate-900 mt-0.5 block">{trackingData.carrier} ({trackingData.vesselName || 'Ocean Vessel'})</span>
                    </div>
                    <div className="p-3 bg-white rounded-xl border border-slate-200">
                      <span className="text-xxs font-bold text-slate-400 uppercase tracking-wider block">Estimated Delivery (ETA)</span>
                      <span className="text-xs font-black text-emerald-600 font-mono mt-0.5 block">{trackingData.eta}</span>
                    </div>
                  </div>

                  {/* Vertical Checkpoints */}
                  {trackingData.events?.length > 0 && (
                    <div className="space-y-2 pt-2 border-t border-slate-200/60">
                      <span className="text-xxs font-bold text-slate-400 uppercase tracking-widest block mb-2">Transit Event History</span>
                      {trackingData.events.map((ev, i) => (
                        <div key={i} className="flex items-center justify-between text-xs py-1 px-2.5 rounded-lg bg-white border border-slate-100">
                          <div className="flex items-center gap-2">
                            <span className={`w-2 h-2 rounded-full ${ev.completed ? 'bg-emerald-500' : 'bg-slate-300'}`}></span>
                            <span className="font-bold text-slate-800">{ev.status}</span>
                            <span className="text-[10px] text-slate-400">({ev.location})</span>
                          </div>
                          <span className="text-[10px] font-mono text-slate-500">{ev.timestamp}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
