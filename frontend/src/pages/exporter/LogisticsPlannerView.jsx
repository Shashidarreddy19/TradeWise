import React, { useState, useEffect } from 'react';
import {
  Truck, Ship, Plane, Train, Box, ShieldCheck, MapPin, DollarSign, AlertTriangle,
  FileText, Clock, BarChart2, Layers, CheckCircle2, ArrowRight, RefreshCw, Sparkles,
  Zap, Search, Globe, ChevronRight, Anchor, Warehouse, Info, Activity, Navigation,
  Lock, Eye, Award, ExternalLink
} from 'lucide-react';
import { logisticsApi } from '../../services';

export default function LogisticsPlannerView({ addToast, products = [], countries = [] }) {
  // Active Module Tab
  const [activeTab, setActiveTab] = useState('planner'); // 'planner', 'mode', 'container', 'freight', 'route', 'ports', 'carriers', 'customs', 'tracking', 'warehouses', 'insurance', 'risk', 'incoterm', 'ai', 'analytics'

  // Input Form State
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

  // State Data
  const [planResult, setPlanResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [portsList, setPortsList] = useState([]);
  const [carriersList, setCarriersList] = useState([]);
  const [routesList, setRoutesList] = useState([]);
  const [containersList, setContainersList] = useState([]);
  const [warehousesList, setWarehousesList] = useState([]);
  const [trackingData, setTrackingData] = useState(null);
  const [analyticsData, setAnalyticsData] = useState(null);
  const [trackingRefInput, setTrackingRefInput] = useState('TW-PLN-847291');

  // Load Reference Directories on Mount
  useEffect(() => {
    fetchDirectories();
    handleCalculatePlan();
  }, []);

  const fetchDirectories = async () => {
    try {
      const [portsRes, carriersRes, routesRes, containersRes, warehousesRes, analyticsRes] = await Promise.all([
        logisticsApi.getPorts().catch(() => ({ data: [] })),
        logisticsApi.getCarriers().catch(() => ({ data: [] })),
        logisticsApi.getRoutes().catch(() => ({ data: [] })),
        logisticsApi.getContainers().catch(() => ({ data: [] })),
        logisticsApi.getWarehouses().catch(() => ({ data: [] })),
        logisticsApi.getAnalytics().catch(() => ({ data: null }))
      ]);

      if (portsRes?.data) setPortsList(portsRes.data);
      if (carriersRes?.data) setCarriersList(carriersRes.data);
      if (routesRes?.data) setRoutesList(routesRes.data);
      if (containersRes?.data) setContainersList(containersRes.data);
      if (warehousesRes?.data) setWarehousesList(warehousesRes.data);
      if (analyticsRes?.data) setAnalyticsData(analyticsRes.data);
    } catch (err) {
      console.error('Failed to load reference data', err);
    }
  };

  const handleCalculatePlan = async () => {
    setLoading(true);
    try {
      const res = await logisticsApi.estimatePlan(formData);
      setPlanResult(res.data);

      // Fetch tracking for initial reference
      const trackRes = await logisticsApi.trackShipment('TW-PLN-847291');
      setTrackingData(trackRes.data);

      if (addToast) addToast('End-to-End Shipment Plan generated!', 'success');
    } catch (err) {
      if (addToast) addToast(err.message || 'Failed to generate shipment plan', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateOfficialPlan = async () => {
    setLoading(true);
    try {
      const res = await logisticsApi.createPlan(formData);
      setPlanResult(res.data);
      if (addToast) addToast(`Shipment Plan #${res.data.planReference} booked & saved!`, 'success');
    } catch (err) {
      if (addToast) addToast(err.message || 'Failed to save shipment plan', 'error');
    } finally {
      setLoading(false);
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

  return (
    <div className="space-y-6 antialiased">
      {/* Top Header Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-3xl p-6 md:p-8 text-white shadow-2xl relative overflow-hidden">
        <div className="absolute right-0 top-0 w-96 h-96 bg-sky-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 relative z-10">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/20 border border-sky-400/30 text-sky-300 text-[10px] font-black tracking-widest uppercase mb-3">
              <Sparkles className="w-3 h-3 text-sky-400" /> CBEC-AI TradeWise Logistics Suite
            </div>
            <h1 className="text-2xl md:text-3xl font-black text-white tracking-tight font-display">
              Enterprise Shipment Planner & Route Optimizer
            </h1>
            <p className="text-xs text-slate-300 max-w-2xl mt-1 leading-relaxed">
              Plan multimodal shipments from Indian factories to global buyers with AI-driven freight estimation, port intelligence, container optimization, customs workflow, and real-time risk assessment.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={handleCalculatePlan}
              disabled={loading}
              className="px-5 py-3 rounded-2xl bg-sky-500 hover:bg-sky-400 text-white font-black text-xs shadow-lg shadow-sky-500/25 transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
              {loading ? 'Re-calculating...' : 'Generate AI Shipment Plan'}
            </button>
          </div>
        </div>

        {/* 15 Module Navigation Bar */}
        <div className="mt-8 pt-6 border-t border-white/10 flex items-center gap-1.5 overflow-x-auto pb-2 scrollbar-none">
          {[
            { id: 'planner', label: '1. Planner Wizard', icon: MapPin },
            { id: 'mode', label: '2. Transport Mode', icon: Truck },
            { id: 'container', label: '3. Containers', icon: Box },
            { id: 'freight', label: '4. Freight Rate Engine', icon: DollarSign },
            { id: 'route', label: '5. Route Optimization', icon: Navigation },
            { id: 'ports', label: '6. Port Intelligence', icon: Anchor },
            { id: 'carriers', label: '7. Carriers', icon: Ship },
            { id: 'customs', label: '8. Customs & Documents', icon: FileText },
            { id: 'tracking', label: '9. Live Tracking', icon: Activity },
            { id: 'warehouses', label: '10. Warehouses', icon: Warehouse },
            { id: 'insurance', label: '11. Cargo Insurance', icon: ShieldCheck },
            { id: 'risk', label: '12. Risk Assessment', icon: AlertTriangle },
            { id: 'incoterm', label: '13. Incoterm Engine', icon: Layers },
            { id: 'ai', label: '14. AI Strategy Cards', icon: Sparkles },
            { id: 'analytics', label: '15. Analytics Dashboard', icon: BarChart2 }
          ].map(tab => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-3.5 py-2 rounded-xl text-xxs font-bold transition-all whitespace-nowrap flex items-center gap-2 cursor-pointer ${
                  isActive
                    ? 'bg-white text-slate-900 shadow-md scale-105 font-extrabold'
                    : 'text-slate-300 hover:text-white hover:bg-white/10'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-sky-600' : 'text-slate-400'}`} />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* MODULE 1: SHIPMENT PLANNER WIZARD */}
      {activeTab === 'planner' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Input Form Column */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <span className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                <MapPin className="w-4 h-4 text-sky-500" /> Shipment Parameters
              </span>
              <span className="text-[10px] font-bold text-sky-600 bg-sky-50 px-2.5 py-1 rounded-full">Module 1</span>
            </div>

            {/* HS Code */}
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">HS Code</label>
              <input
                type="text"
                value={formData.hsCode}
                onChange={e => setFormData({ ...formData, hsCode: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-mono font-bold text-slate-800 focus:outline-none focus:border-sky-500"
              />
            </div>

            {/* Product Name */}
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Product Name</label>
              <input
                type="text"
                value={formData.productName}
                onChange={e => setFormData({ ...formData, productName: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs text-slate-800 focus:outline-none focus:border-sky-500"
              />
            </div>

            {/* Origin & Pickup */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Origin Location</label>
                <input
                  type="text"
                  value={formData.originLocation}
                  onChange={e => setFormData({ ...formData, originLocation: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-800"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Destination Country</label>
                <select
                  value={formData.destinationCountry}
                  onChange={e => setFormData({ ...formData, destinationCountry: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-800 bg-white cursor-pointer"
                >
                  <option>Germany</option>
                  <option>United States</option>
                  <option>UAE</option>
                  <option>Singapore</option>
                  <option>United Kingdom</option>
                  <option>Australia</option>
                </select>
              </div>
            </div>

            {/* Incoterm & Quantity */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Incoterm</label>
                <select
                  value={formData.incoterm}
                  onChange={e => setFormData({ ...formData, incoterm: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-800 font-bold bg-white cursor-pointer"
                >
                  <option>CIF</option>
                  <option>FOB</option>
                  <option>EXW</option>
                  <option>DDP</option>
                  <option>FCA</option>
                  <option>CFR</option>
                  <option>DAP</option>
                  <option>DPU</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Quantity (Units)</label>
                <input
                  type="number"
                  value={formData.quantity}
                  onChange={e => setFormData({ ...formData, quantity: parseInt(e.target.value) || 1 })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-800 font-bold"
                />
              </div>
            </div>

            {/* Unit Weight & Dimensions */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Unit Weight (KG)</label>
                <input
                  type="number"
                  step="0.01"
                  value={formData.weightPerUnitKg}
                  onChange={e => setFormData({ ...formData, weightPerUnitKg: parseFloat(e.target.value) || 0.1 })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-800"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Container Type</label>
                <select
                  value={formData.containerPreference}
                  onChange={e => setFormData({ ...formData, containerPreference: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-800 bg-white cursor-pointer"
                >
                  <option>20FT</option>
                  <option>40FT</option>
                  <option>40HC</option>
                  <option>LCL</option>
                  <option>REEFER</option>
                </select>
              </div>
            </div>

            <button
              onClick={handleCalculatePlan}
              disabled={loading}
              className="w-full py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs transition-all cursor-pointer flex justify-center items-center gap-2"
            >
              {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : 'Run Shipment Plan Calculation'}
            </button>
          </div>

          {/* Results Summary Columns */}
          {planResult && (
            <div className="lg:col-span-2 space-y-6">
              {/* Core Output Metrics Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Total Weight</span>
                  <p className="text-lg font-black text-slate-900 font-mono">{planResult.totalWeightKg} <span className="text-xs font-normal text-slate-500">kg</span></p>
                  <span className="text-[9px] text-emerald-600 font-bold">Vol: {planResult.totalVolumeCbm} CBM</span>
                </div>

                <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Recommended Mode</span>
                  <p className="text-lg font-black text-sky-600 uppercase flex items-center gap-1.5">
                    {planResult.recommendedMode === 'SEA' ? <Ship className="w-4 h-4" /> : <Plane className="w-4 h-4" />}
                    {planResult.recommendedMode}
                  </p>
                  <span className="text-[9px] text-slate-500 font-medium">Est. {planResult.estimatedTransitDays} Days Transit</span>
                </div>

                <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Total Logistics Cost</span>
                  <p className="text-lg font-black text-slate-900 font-mono">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</p>
                  <span className="text-[9px] text-slate-500 font-medium">₹{planResult.costPerKg}/kg • ₹{planResult.costPerUnit}/unit</span>
                </div>

                <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm space-y-1">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Overall Risk Score</span>
                  <p className="text-lg font-black text-emerald-600 font-mono">{planResult.overallRiskScore} / 100</p>
                  <span className="text-[9px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 inline-block">{planResult.riskCategory} Risk</span>
                </div>
              </div>

              {/* End-to-End Workflow Diagram */}
              <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-4">
                <div className="flex justify-between items-center">
                  <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest flex items-center gap-2">
                    <Navigation className="w-4 h-4 text-sky-500" /> End-to-End Shipment Route & Workflow
                  </h3>
                  <button
                    onClick={handleCreateOfficialPlan}
                    className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md transition-all cursor-pointer"
                  >
                    Confirm & Save Official Plan
                  </button>
                </div>

                {/* Milestone Stepper */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                  {planResult.workflowMilestones?.map((m, idx) => (
                    <div key={idx} className="p-3 rounded-2xl border border-slate-100 bg-slate-50/50 space-y-1 relative">
                      <div className="flex items-center gap-2">
                        <span className="w-5 h-5 rounded-full bg-sky-500 text-white text-[10px] font-black flex items-center justify-center">{idx + 1}</span>
                        <span className="text-xs font-bold text-slate-800">{m.milestone}</span>
                      </div>
                      <p className="text-[10px] text-slate-500 font-mono ml-7">{m.date}</p>
                    </div>
                  ))}
                </div>

                {/* Carbon Footprint & Details */}
                <div className="p-4 rounded-2xl bg-gradient-to-r from-emerald-50 to-teal-50 border border-emerald-200 flex justify-between items-center text-xs">
                  <div className="flex items-center gap-2 text-emerald-900 font-bold">
                    <Sparkles className="w-4 h-4 text-emerald-600" /> Total Estimated Carbon Footprint:
                    <span className="font-mono text-sm">{planResult.carbonEmissionsKg} kg CO₂e</span>
                  </div>
                  <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-3 py-1 rounded-full">Eco-Optimized Lane</span>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* MODULE 2: TRANSPORT MODE RECOMMENDATION */}
      {activeTab === 'mode' && planResult && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-6">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                <Truck className="w-4 h-4 text-sky-500" /> Module 2: Transport Mode Recommendation Engine
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Automated mode scoring based on cost, transit time, weight, volume, urgency, and carbon emissions.</p>
            </div>
            <span className="px-3 py-1 rounded-full bg-sky-50 text-sky-700 text-xs font-black">Recommended: {planResult.recommendedMode}</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Primary Recommended Mode Card */}
            <div className="p-6 rounded-3xl border-2 border-sky-500 bg-sky-50/20 space-y-4 relative">
              <span className="absolute top-4 right-4 px-3 py-1 rounded-full bg-sky-500 text-white text-[9px] font-black uppercase tracking-wider">Optimal Match</span>
              <div className="flex items-center gap-3">
                <div className="p-3 rounded-2xl bg-sky-500 text-white">
                  {planResult.recommendedMode === 'SEA' ? <Ship className="w-6 h-6" /> : <Plane className="w-6 h-6" />}
                </div>
                <div>
                  <h3 className="text-base font-black text-slate-900">{planResult.recommendedMode} Freight</h3>
                  <span className="text-xs font-bold text-sky-700">Recommended Primary Transport</span>
                </div>
              </div>

              <div className="space-y-2 text-xs">
                <div className="flex justify-between text-slate-600"><span className="font-medium">Transit Time:</span> <span className="font-bold font-mono text-slate-900">{planResult.estimatedTransitDays} Days</span></div>
                <div className="flex justify-between text-slate-600"><span className="font-medium">Total Cost:</span> <span className="font-bold font-mono text-slate-900">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</span></div>
                <div className="flex justify-between text-slate-600"><span className="font-medium">Carbon Emissions:</span> <span className="font-bold font-mono text-emerald-600">{planResult.carbonEmissionsKg} kg CO₂</span></div>
              </div>

              <div className="p-3 rounded-xl bg-white border border-sky-200 text-xs text-slate-700 font-medium leading-relaxed">
                ✓ Best balance of cost and capacity for {planResult.totalWeightKg} kg shipment. Eligible for Trade Agreement tariff preferences.
              </div>
            </div>

            {/* Alternative Modes */}
            {planResult.alternativeModes?.map((alt, idx) => (
              <div key={idx} className="p-6 rounded-3xl border border-slate-200 bg-slate-50/50 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="p-3 rounded-2xl bg-slate-200 text-slate-700">
                    {alt.mode.includes('AIR') ? <Plane className="w-6 h-6" /> : alt.mode.includes('SEA') ? <Ship className="w-6 h-6" /> : <Train className="w-6 h-6" />}
                  </div>
                  <div>
                    <h3 className="text-base font-black text-slate-900">{alt.mode}</h3>
                    <span className="text-xs font-bold text-slate-500">Alternative Option</span>
                  </div>
                </div>

                <div className="space-y-2 text-xs">
                  <div className="flex justify-between text-slate-600"><span className="font-medium">Transit Time:</span> <span className="font-bold font-mono text-slate-900">{alt.transitDays} Days</span></div>
                  <div className="flex justify-between text-slate-600"><span className="font-medium">Estimated Cost:</span> <span className="font-bold font-mono text-slate-900">₹{Number(alt.estimatedCost).toLocaleString('en-IN')}</span></div>
                  <div className="flex justify-between text-slate-600"><span className="font-medium">Carbon Footprint:</span> <span className="font-bold font-mono text-slate-700">{alt.carbonKg} kg CO₂</span></div>
                </div>

                <div className="space-y-1 text-xxs">
                  <p className="text-emerald-700 font-bold">✓ Advantage: {alt.advantages}</p>
                  <p className="text-amber-700 font-bold">⚠ Disadvantage: {alt.disadvantages}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* MODULE 3: CONTAINER RECOMMENDATION */}
      {activeTab === 'container' && planResult && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-6">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                <Box className="w-4 h-4 text-sky-500" /> Module 3: Container Recommendation & Utilization
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Calculates volume, weight utilization, loading efficiency, and unused capacity.</p>
            </div>
            <span className="px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-black">
              Recommended Container: {planResult.containerRecommendation?.recommendedType}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-6 rounded-3xl bg-slate-900 text-white space-y-4">
              <span className="text-[10px] font-bold text-sky-400 uppercase tracking-widest block">Utilization Gauge</span>
              <div className="text-4xl font-black font-mono text-white">
                {planResult.containerRecommendation?.containerUtilizationPct}%
              </div>
              <div className="w-full bg-slate-800 rounded-full h-3 overflow-hidden">
                <div
                  className="bg-sky-400 h-3 rounded-full transition-all"
                  style={{ width: `${planResult.containerRecommendation?.containerUtilizationPct}%` }}
                ></div>
              </div>
              <p className="text-xs text-slate-300">
                Loading Efficiency: <span className="font-bold text-emerald-400">{planResult.containerRecommendation?.loadingEfficiency}</span>
              </p>
            </div>

            <div className="p-6 rounded-3xl border border-slate-200 bg-white space-y-3 md:col-span-2">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-wider">Container Specification & Capacity Analysis</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-slate-400 font-bold block text-[10px]">Type</span>
                  <span className="font-black text-slate-900">{planResult.containerRecommendation?.recommendedType}</span>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-slate-400 font-bold block text-[10px]">Number Required</span>
                  <span className="font-black text-slate-900">{planResult.containerRecommendation?.containerCount} Container(s)</span>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-slate-400 font-bold block text-[10px]">Unused Space</span>
                  <span className="font-black text-slate-900">{planResult.containerRecommendation?.unusedCapacityCbm} CBM</span>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-slate-400 font-bold block text-[10px]">Payload Fit</span>
                  <span className="font-black text-emerald-600">Within Weight Limit</span>
                </div>
              </div>
              <p className="text-xs text-slate-600 italic bg-sky-50/50 p-3 rounded-xl border border-sky-100">
                💡 Note: {planResult.containerRecommendation?.recommendationNote}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* MODULE 4: FREIGHT RATE ENGINE */}
      {activeTab === 'freight' && planResult && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-6">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                <DollarSign className="w-4 h-4 text-sky-500" /> Module 4: Granular Freight Cost Breakdown
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Itemized estimation of road, ocean/air, terminal, fuel surcharges, customs, and delivery fees.</p>
            </div>
            <div className="text-right">
              <span className="text-[10px] font-bold text-slate-400 block uppercase">Grand Total</span>
              <span className="text-xl font-black text-slate-900 font-mono">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</span>
            </div>
          </div>

          {/* Granular Cost Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 text-slate-400 font-bold text-[10px] uppercase">
                  <th className="py-3 px-4">Cost Component</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4 text-right">Amount (INR)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-slate-700">
                <tr><td className="py-3 px-4 font-bold text-slate-900">Inland Road Freight (Pickup)</td><td className="py-3 px-4 text-slate-500">Factory in Nashik to JNPT Port Terminal</td><td className="py-3 px-4 text-right font-bold">₹{Number(planResult.costBreakdown?.roadFreight).toLocaleString('en-IN')}</td></tr>
                <tr><td className="py-3 px-4 font-bold text-slate-900">Main Sea / Ocean Freight</td><td className="py-3 px-4 text-slate-500">JNPT (India) to Hamburg Port (Germany)</td><td className="py-3 px-4 text-right font-bold">₹{Number(planResult.costBreakdown?.oceanFreight || planResult.costBreakdown?.airFreight).toLocaleString('en-IN')}</td></tr>
                <tr><td className="py-3 px-4 font-bold text-slate-900">Terminal Handling Charges (THC)</td><td className="py-3 px-4 text-slate-500">Port loading & container crane operations</td><td className="py-3 px-4 text-right">₹{Number(planResult.costBreakdown?.terminalCharges).toLocaleString('en-IN')}</td></tr>
                <tr><td className="py-3 px-4 font-bold text-slate-900">Fuel Surcharge (FSC / BAF / CAF)</td><td className="py-3 px-4 text-slate-500">Bunker adjustment & fuel fluctuation fee</td><td className="py-3 px-4 text-right">₹{Number(planResult.costBreakdown?.fuelSurcharge).toLocaleString('en-IN')}</td></tr>
                <tr><td className="py-3 px-4 font-bold text-slate-900">Customs Clearance & Documentation</td><td className="py-3 px-4 text-slate-500">ICEGATE export shipping bill & EDI filing</td><td className="py-3 px-4 text-right">₹{Number(planResult.costBreakdown?.customsCharges).toLocaleString('en-IN')}</td></tr>
                <tr><td className="py-3 px-4 font-bold text-slate-900">Marine Cargo Insurance Premium</td><td className="py-3 px-4 text-slate-500">Institute Cargo Clauses (A) 110% coverage</td><td className="py-3 px-4 text-right">₹{Number(planResult.costBreakdown?.insurancePremium).toLocaleString('en-IN')}</td></tr>
                <tr><td className="py-3 px-4 font-bold text-slate-900">Last-Mile Inland Delivery</td><td className="py-3 px-4 text-slate-500">Hamburg Port to Importer Warehouse</td><td className="py-3 px-4 text-right font-bold">₹{Number(planResult.costBreakdown?.deliveryCharges).toLocaleString('en-IN')}</td></tr>
                <tr className="bg-slate-900 text-white font-bold text-sm">
                  <td className="py-4 px-4 font-black">GRAND TOTAL LOGISTICS COST</td>
                  <td className="py-4 px-4 font-normal text-xs text-slate-300">Cost/KG: ₹{planResult.costPerKg} | Cost/Unit: ₹{planResult.costPerUnit}</td>
                  <td className="py-4 px-4 text-right font-black font-mono">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* MODULE 6 & 7: PORTS & CARRIERS DIRECTORY */}
      {activeTab === 'ports' && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-4">
          <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
            <Anchor className="w-4 h-4 text-sky-500" /> Module 6: Port Intelligence Database
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {portsList.map((port, idx) => (
              <div key={idx} className="p-4 rounded-2xl border border-slate-100 bg-slate-50/50 space-y-2">
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="text-xs font-black text-slate-900">{port.name}</h3>
                    <span className="text-[10px] font-mono text-sky-600 font-bold">UN/LOCODE: {port.unlocode}</span>
                  </div>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[9px] font-bold">
                    {port.congestionLevel} Congestion
                  </span>
                </div>
                <p className="text-[11px] text-slate-500">Country: <span className="font-bold text-slate-800">{port.country}</span> | Type: <span className="font-bold text-slate-800">{port.portType}</span></p>
                <div className="text-[10px] text-slate-400">Waiting Time: {port.avgWaitingDays} day(s) avg</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'carriers' && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-4">
          <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
            <Ship className="w-4 h-4 text-sky-500" /> Module 7: Carrier Performance & Comparison
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {carriersList.map((c, idx) => (
              <div key={idx} className="p-4 rounded-2xl border border-slate-200 bg-white space-y-3">
                <div className="flex justify-between items-center">
                  <h3 className="text-xs font-black text-slate-900">{c.name}</h3>
                  <span className="text-[10px] font-mono font-bold bg-slate-100 px-2 py-0.5 rounded">{c.code}</span>
                </div>
                <div className="text-xs space-y-1 text-slate-600">
                  <div>Reliability Score: <span className="font-bold text-emerald-600">{c.reliabilityScore}%</span></div>
                  <div>Historic On-Time: <span className="font-bold text-slate-900">{c.historicOnTimePct}%</span></div>
                  <div>Rating: <span className="font-bold text-sky-600">{c.costRating}</span></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* MODULE 8: CUSTOMS WORKFLOW */}
      {activeTab === 'customs' && planResult && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-6">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                <FileText className="w-4 h-4 text-sky-500" /> Module 8: Customs Document Checklist Generator
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Auto-generated regulatory document checklist for HS Code {planResult.hsCode} to {planResult.destinationCountry}.</p>
            </div>
            <span className="px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-black">ICEGATE Enabled</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {planResult.requiredDocuments?.map((doc, idx) => (
              <div key={idx} className="p-4 rounded-2xl border border-slate-200 bg-slate-50/50 flex items-start gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <h3 className="text-xs font-bold text-slate-900">{doc.documentName}</h3>
                    <span className="text-[9px] font-mono font-bold bg-sky-100 text-sky-800 px-1.5 py-0.5 rounded">{doc.code}</span>
                  </div>
                  <p className="text-[11px] text-slate-500">{doc.description}</p>
                  <span className="text-[10px] text-slate-400 block font-semibold">Issuer: {doc.issuingAuthority}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* MODULE 9: LIVE TRACKING */}
      {activeTab === 'tracking' && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-6">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                <Activity className="w-4 h-4 text-sky-500" /> Module 9: Live Shipment Tracking & Milestones
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Real-time GPS milestone tracking and delay probability forecasting.</p>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={trackingRefInput}
                onChange={e => setTrackingRefInput(e.target.value)}
                placeholder="Enter Reference (e.g. TW-PLN-847291)"
                className="px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-mono"
              />
              <button onClick={handleTrackSearch} className="px-3 py-1.5 rounded-xl bg-slate-900 text-white text-xs font-bold cursor-pointer">Track</button>
            </div>
          </div>

          {trackingData && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-4 rounded-2xl bg-sky-50 border border-sky-100">
                  <span className="text-[10px] font-bold text-sky-600 block uppercase">Current Location</span>
                  <span className="text-sm font-black text-slate-900">{trackingData.currentLocation}</span>
                </div>
                <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-100">
                  <span className="text-[10px] font-bold text-emerald-600 block uppercase">Carrier Vessel</span>
                  <span className="text-sm font-black text-slate-900">{trackingData.carrier} ({trackingData.vesselName})</span>
                </div>
                <div className="p-4 rounded-2xl bg-indigo-50 border border-indigo-100">
                  <span className="text-[10px] font-bold text-indigo-600 block uppercase">Estimated Arrival (ETA)</span>
                  <span className="text-sm font-black text-slate-900 font-mono">{trackingData.eta}</span>
                </div>
              </div>

              {/* Milestones Vertical Timeline */}
              <div className="space-y-3 pl-4 border-l-2 border-slate-200">
                {trackingData.events?.map((e, idx) => (
                  <div key={idx} className="relative pl-6 pb-4">
                    <div className={`absolute -left-[31px] top-0.5 w-4 h-4 rounded-full border-2 bg-white ${e.completed ? 'border-emerald-500 bg-emerald-500' : 'border-slate-300'}`}></div>
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className={`text-xs font-bold ${e.completed ? 'text-slate-900' : 'text-slate-400'}`}>{e.status}</h4>
                        <p className="text-[11px] text-slate-500">{e.location}</p>
                      </div>
                      <span className="text-[10px] font-mono text-slate-400">{e.timestamp}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* MODULE 12: RISK ASSESSMENT */}
      {activeTab === 'risk' && planResult && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-6">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-amber-500" /> Module 12: Multi-Factor Risk Assessment
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Evaluates country, weather, port congestion, customs delay, and carrier risk factors.</p>
            </div>
            <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-black">
              Overall Score: {planResult.overallRiskScore} / 100 ({planResult.riskCategory})
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-3">
              <h3 className="text-xs font-black text-slate-800 uppercase tracking-wider">Risk Factors Breakdown</h3>
              {Object.entries(planResult.riskBreakdown || {}).map(([key, val], idx) => (
                <div key={idx} className="space-y-1">
                  <div className="flex justify-between text-xs font-bold text-slate-700">
                    <span className="capitalize">{key.replace(/([A-Z])/g, ' $1')}</span>
                    <span className="font-mono">{val}%</span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div className="bg-amber-400 h-2 rounded-full" style={{ width: `${val}%` }}></div>
                  </div>
                </div>
              ))}
            </div>

            <div className="p-5 rounded-2xl bg-amber-50/50 border border-amber-200 space-y-3">
              <h3 className="text-xs font-black text-amber-900 uppercase tracking-wider">Recommended Risk Mitigations</h3>
              <ul className="space-y-2 text-xs text-amber-800">
                {planResult.mitigationSuggestions?.map((m, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <ShieldCheck className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                    <span>{m}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* MODULE 15: ANALYTICS DASHBOARD */}
      {activeTab === 'analytics' && analyticsData && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-6">
          <div className="flex justify-between items-center border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-widest flex items-center gap-2">
                <BarChart2 className="w-4 h-4 text-sky-500" /> Module 15: Logistics & Export Analytics Dashboard
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">Historical shipping trends, container utilization, carrier shares, and delivery metrics.</p>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
              <span className="text-[10px] text-slate-400 font-bold block uppercase">Avg Transit Time</span>
              <span className="text-2xl font-black text-slate-900 font-mono">{analyticsData.avgTransitTimeDays} Days</span>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
              <span className="text-[10px] text-slate-400 font-bold block uppercase">Container Utilization</span>
              <span className="text-2xl font-black text-sky-600 font-mono">{analyticsData.avgContainerUtilization}</span>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
              <span className="text-[10px] text-slate-400 font-bold block uppercase">Shipment Success Rate</span>
              <span className="text-2xl font-black text-emerald-600 font-mono">{analyticsData.shipmentSuccessRate}</span>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
              <span className="text-[10px] text-slate-400 font-bold block uppercase">Avg Customs Delay</span>
              <span className="text-2xl font-black text-indigo-600 font-mono">{analyticsData.avgCustomsDelayDays} Days</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
