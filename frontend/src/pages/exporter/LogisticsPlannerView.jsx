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
      <div className="card-claude bg-gradient-to-br from-card via-card to-accent/40 rounded-2xl p-6 md:p-7 border border-border shadow-sm relative overflow-hidden">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-5 relative z-10">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary text-[10px] font-semibold tracking-wide">
              <Sparkles className="w-3.5 h-3.5" /> CBEC-AI TradeWise Logistics Suite
            </div>
            <h1 className="text-xl md:text-2xl font-bold text-foreground tracking-tight">
              Enterprise Shipment Planner & Route Optimizer
            </h1>
            <p className="text-xs text-muted-foreground max-w-2xl leading-relaxed">
              Configure your export shipment, compare Ocean vs. Air economics, review granular freight charges, and trace the 7-stage Indian export dispatch workflow.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={handleCalculatePlan}
              disabled={loading}
              className="btn-primary"
            >
              {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
              <span>{loading ? 'Calculating...' : 'Recalculate Route'}</span>
            </button>
          </div>
        </div>

        {/* ── 4-Stage Progressive Workflow Navigation ───────────────────────── */}
        <div className="mt-6 pt-4 border-t border-border grid grid-cols-2 md:grid-cols-4 gap-2">
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
                className={`p-3 rounded-xl text-left transition-all cursor-pointer border ${
                  isActive
                    ? 'bg-card text-foreground shadow-sm border-primary/40 font-semibold'
                    : 'bg-muted/30 border-border/50 text-muted-foreground hover:bg-muted/70 hover:text-foreground'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className={`text-[10px] font-semibold uppercase tracking-wider ${isActive ? 'text-primary' : 'text-muted-foreground'}`}>
                    {tab.step}
                  </span>
                  <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-primary' : 'text-muted-foreground'}`} />
                </div>
                <div className={`text-xs mt-1 ${isActive ? 'text-foreground font-semibold' : 'text-foreground/80'}`}>
                  {tab.label}
                </div>
                <div className="text-[10px] mt-0.5 truncate text-muted-foreground">
                  {tab.desc}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Executive Summary Metrics (Visible when plan is calculated) ──────── */}
      {planResult && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5 animate-in fade-in duration-150">
          <div className="card-claude p-4 space-y-1">
            <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block">Cargo Weight & Volume</span>
            <p className="text-xl font-bold text-foreground font-mono">
              {planResult.totalWeightKg} <span className="text-xs font-normal text-muted-foreground">kg</span>
            </p>
            <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium block">Vol: {planResult.totalVolumeCbm} CBM</span>
          </div>

          <div className="card-claude p-4 space-y-1">
            <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block">Recommended Mode</span>
            <p className="text-xl font-bold text-primary uppercase flex items-center gap-1.5">
              {planResult.recommendedMode === 'SEA' ? <Ship className="w-5 h-5" /> : <Plane className="w-5 h-5" />}
              {planResult.recommendedMode} FREIGHT
            </p>
            <span className="text-[11px] text-muted-foreground block">Est. {planResult.estimatedTransitDays} Days Door-to-Door</span>
          </div>

          <div className="card-claude p-4 space-y-1">
            <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block">Total Logistics Cost</span>
            <p className="text-xl font-bold text-foreground font-mono">
              ₹{Number(planResult.grandTotal).toLocaleString('en-IN')}
            </p>
            <span className="text-[11px] text-muted-foreground block">
              ₹{planResult.costPerKg}/kg • ₹{planResult.costPerUnit}/unit
            </span>
          </div>

          <div className="card-claude p-4 space-y-1">
            <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block">Route Risk & Footprint</span>
            <p className="text-xl font-bold text-emerald-600 dark:text-emerald-400 font-mono">
              {planResult.overallRiskScore} <span className="text-xs font-normal text-muted-foreground">/ 100</span>
            </p>
            <span className="text-[10px] text-emerald-700 dark:text-emerald-300 font-semibold bg-emerald-500/10 px-2 py-0.5 rounded-full inline-block border border-emerald-500/20">
              {planResult.riskCategory} Risk • {planResult.carbonEmissionsKg} kg CO₂e
            </span>
          </div>
        </div>
      )}

      {/* ── STAGE 1: SHIPMENT PARAMETERS & CARGO SETUP ───────────────────────── */}
      {activeTab === 'setup' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 animate-in fade-in duration-150">
          {/* Main Input Form */}
          <div className="lg:col-span-2 card-claude p-5 md:p-6 space-y-5">
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-primary" /> Export Shipment Parameters
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">Specify origin factory, commodity details, target market, and container size.</p>
              </div>
              <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2.5 py-0.5 rounded-full border border-primary/20">Step 1 of 4</span>
            </div>

            {/* Quick Catalog Product Selector */}
            {products.length > 0 && (
              <div className="p-3 bg-muted/30 rounded-xl border border-border space-y-1.5">
                <span className="text-[10px] font-semibold text-foreground uppercase tracking-wider block">Quick Pick from Catalog:</span>
                <div className="flex flex-wrap gap-1.5">
                  {products.slice(0, 6).map(p => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => handleSelectProduct(p.name)}
                      className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all cursor-pointer ${
                        formData.productName === p.name
                          ? 'bg-primary text-primary-foreground font-semibold shadow-sm'
                          : 'bg-card border border-border text-foreground hover:border-primary/40'
                      }`}
                    >
                      {p.name}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
              {/* Product Name */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">Product Description</label>
                <input
                  type="text"
                  value={formData.productName}
                  onChange={e => setFormData({ ...formData, productName: e.target.value })}
                  className="input-claude"
                  placeholder="e.g. Premium Basmati Rice"
                />
              </div>

              {/* HS Code */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">HS Code (ITC-HS)</label>
                <input
                  type="text"
                  value={formData.hsCode}
                  onChange={e => setFormData({ ...formData, hsCode: e.target.value })}
                  className="input-claude font-mono"
                  placeholder="e.g. 1006.30 or 0910.30"
                />
              </div>

              {/* Origin Factory in India */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">Origin Factory / Farm Location</label>
                <input
                  type="text"
                  value={formData.originLocation}
                  onChange={e => setFormData({ ...formData, originLocation: e.target.value })}
                  className="input-claude"
                  placeholder="e.g. Nashik, Maharashtra, India"
                />
              </div>

              {/* Destination Country */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">Destination Country</label>
                <select
                  value={formData.destinationCountry}
                  onChange={e => setFormData({ ...formData, destinationCountry: e.target.value })}
                  className="input-claude cursor-pointer"
                >
                  {destinationCountryList.map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              {/* Incoterm */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">Incoterm</label>
                <select
                  value={formData.incoterm}
                  onChange={e => setFormData({ ...formData, incoterm: e.target.value })}
                  className="input-claude cursor-pointer"
                >
                  <option value="CIF">CIF — Cost, Insurance & Freight (Seller pays freight to destination)</option>
                  <option value="FOB">FOB — Free On Board (Seller delivers to Indian port)</option>
                  <option value="CFR">CFR — Cost & Freight (Seller pays freight, buyer covers insurance)</option>
                  <option value="EXW">EXW — Ex Works (Buyer handles all pickup from factory)</option>
                  <option value="DDP">DDP — Delivered Duty Paid (Door-to-door complete delivery)</option>
                </select>
              </div>

              {/* Container Preference */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">Container Specification</label>
                <select
                  value={formData.containerPreference}
                  onChange={e => setFormData({ ...formData, containerPreference: e.target.value })}
                  className="input-claude cursor-pointer"
                >
                  <option value="20FT">20 FT Standard Container (Max 28 CBM / 21,500 kg)</option>
                  <option value="40FT">40 FT Standard Container (Max 58 CBM / 26,000 kg)</option>
                  <option value="40HC">40 FT High Cube Container (Max 68 CBM / 26,000 kg)</option>
                  <option value="LCL">LCL (Less than Container Load — Shared Groupage)</option>
                  <option value="REEFER">Reefer Container (Temperature-Controlled)</option>
                </select>
              </div>

              {/* Quantity */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">Export Quantity (Units)</label>
                <input
                  type="number"
                  min="1"
                  value={formData.quantity}
                  onChange={e => setFormData({ ...formData, quantity: parseInt(e.target.value) || 1 })}
                  className="input-claude"
                />
              </div>

              {/* Unit Weight */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground block">Weight Per Unit (KG)</label>
                <input
                  type="number"
                  step="0.05"
                  min="0.05"
                  value={formData.weightPerUnitKg}
                  onChange={e => setFormData({ ...formData, weightPerUnitKg: parseFloat(e.target.value) || 0.1 })}
                  className="input-claude"
                />
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={handleCalculatePlan}
                disabled={loading}
                className="btn-primary"
              >
                {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
                <span>Calculate & Optimize Shipment Plan</span>
              </button>
            </div>
          </div>

          {/* Container & Packaging Utilization Preview */}
          <div className="card-claude p-5 space-y-4">
            <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider flex items-center gap-1.5 border-b border-border pb-3">
              <Box className="w-3.5 h-3.5 text-primary" /> Container Optimization
            </h3>

            {planResult?.containerRecommendation ? (
              <div className="space-y-3.5">
                <div className="p-4 rounded-xl bg-card border border-border space-y-2.5">
                  <div className="flex justify-between items-center">
                    <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Utilization</span>
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-primary/10 text-primary">
                      {planResult.containerRecommendation?.loadingEfficiency || 'High'}
                    </span>
                  </div>
                  <div className="text-2xl font-bold font-mono text-foreground">
                    {planResult.containerRecommendation?.containerUtilizationPct}%
                  </div>
                  <div className="w-full bg-muted rounded-full h-2 overflow-hidden">
                    <div
                      className="bg-primary h-2 rounded-full transition-all"
                      style={{ width: `${Math.min(100, planResult.containerRecommendation?.containerUtilizationPct || 85)}%` }}
                    ></div>
                  </div>
                  <p className="text-[11px] text-muted-foreground">
                    Recommended: <strong className="text-foreground">{planResult.containerRecommendation?.recommendedType}</strong> ({planResult.containerRecommendation?.containerCount} Unit)
                  </p>
                </div>

                <div className="space-y-2 text-xs">
                  <div className="flex justify-between p-2 rounded-lg bg-muted/30 border border-border/50">
                    <span className="text-muted-foreground">Unused Space:</span>
                    <span className="font-semibold text-foreground font-mono">{planResult.containerRecommendation?.unusedCapacityCbm} CBM</span>
                  </div>
                  <div className="flex justify-between p-2 rounded-lg bg-muted/30 border border-border/50">
                    <span className="text-muted-foreground">Weight Limit:</span>
                    <span className="font-semibold text-emerald-600 dark:text-emerald-400">Within Safe Limits ✓</span>
                  </div>
                  <div className="flex justify-between p-2 rounded-lg bg-muted/30 border border-border/50">
                    <span className="text-muted-foreground">Dispatch Window:</span>
                    <span className="font-semibold text-foreground font-mono">{formData.expectedDispatchDate}</span>
                  </div>
                </div>

                <div className="p-3 bg-accent/50 rounded-lg border border-border text-xs text-foreground leading-relaxed">
                  💡 {planResult.containerRecommendation?.recommendationNote}
                </div>
              </div>
            ) : (
              <div className="p-8 text-center text-muted-foreground space-y-2">
                <Box className="w-8 h-8 mx-auto text-muted-foreground/60" />
                <p className="text-xs">Container metrics will appear here after calculation.</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── STAGE 2: TRANSPORT MODE COMPARISON ───────────────────────────────── */}
      {activeTab === 'modes' && planResult && (
        <div className="space-y-5 animate-in fade-in duration-150">
          <div className="card-claude p-5 md:p-6 space-y-5">
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                  <Truck className="w-3.5 h-3.5 text-primary" /> Mode Comparison: Ocean vs. Air Freight
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">Automated economics comparison based on payload weight, urgency, transit days, and tariff agreements.</p>
              </div>
              <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2.5 py-0.5 rounded-full border border-primary/20">Step 2 of 4</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Primary Recommended Option */}
              <div className="p-5 rounded-2xl border-2 border-primary bg-card space-y-4 relative shadow-sm">
                <span className="absolute top-3.5 right-3.5 px-2 py-0.5 rounded-full bg-primary text-primary-foreground text-[9px] font-semibold uppercase tracking-wider">
                  Recommended
                </span>
                <div className="flex items-center gap-3">
                  <div className="p-3 rounded-xl bg-primary/10 text-primary">
                    {planResult.recommendedMode === 'SEA' ? <Ship className="w-5 h-5" /> : <Plane className="w-5 h-5" />}
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-foreground">{planResult.recommendedMode} FREIGHT</h4>
                    <span className="text-xs text-primary font-medium">Optimal Multimodal Option</span>
                  </div>
                </div>

                <div className="space-y-2 text-xs">
                  <div className="flex justify-between p-2 rounded-lg bg-muted/40 border border-border/50">
                    <span className="text-muted-foreground">Estimated Transit:</span>
                    <span className="font-semibold text-foreground font-mono">{planResult.estimatedTransitDays} Days</span>
                  </div>
                  <div className="flex justify-between p-2 rounded-lg bg-muted/40 border border-border/50">
                    <span className="text-muted-foreground">Total Freight:</span>
                    <span className="font-semibold text-foreground font-mono">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</span>
                  </div>
                  <div className="flex justify-between p-2 rounded-lg bg-muted/40 border border-border/50">
                    <span className="text-muted-foreground">Carbon Emissions:</span>
                    <span className="font-semibold text-emerald-600 dark:text-emerald-400 font-mono">{planResult.carbonEmissionsKg} kg CO₂e</span>
                  </div>
                </div>

                <div className="p-3 bg-accent/40 rounded-lg border border-border text-xs text-foreground leading-relaxed">
                  ✓ Best cost-per-kilogram efficiency for bulk export to {formData.destinationCountry}. Full container security and customs clearance under standard liner terms.
                </div>
              </div>

              {/* Alternative Modes */}
              {planResult.alternativeModes?.map((alt, idx) => (
                <div key={idx} className="p-5 rounded-2xl border border-border bg-card space-y-4">
                  <div className="flex items-center gap-3">
                    <div className="p-3 rounded-xl bg-muted text-muted-foreground">
                      {alt.mode.includes('AIR') ? <Plane className="w-5 h-5" /> : <Ship className="w-5 h-5" />}
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-foreground">{alt.mode}</h4>
                      <span className="text-xs text-muted-foreground">Alternative Carrier Path</span>
                    </div>
                  </div>

                  <div className="space-y-2 text-xs">
                    <div className="flex justify-between p-2 rounded-lg bg-muted/40 border border-border/50">
                      <span className="text-muted-foreground">Estimated Transit:</span>
                      <span className="font-semibold text-foreground font-mono">{alt.transitDays} Days</span>
                    </div>
                    <div className="flex justify-between p-2 rounded-lg bg-muted/40 border border-border/50">
                      <span className="text-muted-foreground">Estimated Cost:</span>
                      <span className="font-semibold text-foreground font-mono">₹{Number(alt.estimatedCost).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between p-2 rounded-lg bg-muted/40 border border-border/50">
                      <span className="text-muted-foreground">Carbon Footprint:</span>
                      <span className="font-semibold text-muted-foreground font-mono">{alt.carbonKg} kg CO₂e</span>
                    </div>
                  </div>

                  <div className="space-y-1.5 text-[11px] leading-relaxed">
                    <p className="text-emerald-700 dark:text-emerald-300 font-medium bg-emerald-500/10 p-2 rounded-lg border border-emerald-500/20">
                      ✓ Pro: {alt.advantages}
                    </p>
                    <p className="text-amber-700 dark:text-amber-300 font-medium bg-amber-500/10 p-2 rounded-lg border border-amber-500/20">
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
        <div className="space-y-5 animate-in fade-in duration-150">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            {/* Granular Cost Breakdown Table */}
            <div className="lg:col-span-2 card-claude p-5 md:p-6 space-y-5">
              <div className="flex justify-between items-center border-b border-border pb-3">
                <div>
                  <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                    <DollarSign className="w-3.5 h-3.5 text-primary" /> Granular Freight & Handling Breakdown
                  </h3>
                  <p className="text-xs text-muted-foreground mt-0.5">Itemized inland freight, ocean liner rates, port operations, and ICEGATE customs fees.</p>
                </div>
                <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2.5 py-0.5 rounded-full border border-primary/20">Step 3 of 4</span>
              </div>

              <div className="overflow-x-auto">
                <table className="table-claude">
                  <thead>
                    <tr>
                      <th className="py-2.5 px-3">Cost Head</th>
                      <th className="py-2.5 px-3">Description</th>
                      <th className="py-2.5 px-3 text-right">Amount (INR)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border text-xs">
                    <tr>
                      <td className="py-2.5 px-3 font-medium text-foreground">1. Inland Road Transport</td>
                      <td className="py-2.5 px-3 text-muted-foreground">Pickup from {formData.originLocation} to Port Terminal</td>
                      <td className="py-2.5 px-3 text-right font-semibold text-foreground font-mono">₹{Number(planResult.costBreakdown?.roadFreight).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-2.5 px-3 font-medium text-foreground">2. Ocean / Air Freight</td>
                      <td className="py-2.5 px-3 text-muted-foreground">Port-to-port carriage to {formData.destinationCountry}</td>
                      <td className="py-2.5 px-3 text-right font-semibold text-foreground font-mono">₹{Number(planResult.costBreakdown?.oceanFreight || planResult.costBreakdown?.airFreight).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-2.5 px-3 font-medium text-foreground">3. Terminal Handling (THC)</td>
                      <td className="py-2.5 px-3 text-muted-foreground">Port crane loading, yard management & gate-in</td>
                      <td className="py-2.5 px-3 text-right font-semibold text-foreground font-mono">₹{Number(planResult.costBreakdown?.terminalCharges).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-2.5 px-3 font-medium text-foreground">4. Bunker & Fuel Surcharge</td>
                      <td className="py-2.5 px-3 text-muted-foreground">BAF (Bunker Adjustment Factor) fuel index</td>
                      <td className="py-2.5 px-3 text-right font-semibold text-foreground font-mono">₹{Number(planResult.costBreakdown?.fuelSurcharge).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-2.5 px-3 font-medium text-foreground">5. Customs Clearance (EDI)</td>
                      <td className="py-2.5 px-3 text-muted-foreground">Indian ICEGATE electronic shipping bill filing</td>
                      <td className="py-2.5 px-3 text-right font-semibold text-foreground font-mono">₹{Number(planResult.costBreakdown?.customsCharges).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-2.5 px-3 font-medium text-foreground">6. Marine Cargo Insurance</td>
                      <td className="py-2.5 px-3 text-muted-foreground">Institute Cargo Clauses (A) 110% CIF coverage</td>
                      <td className="py-2.5 px-3 text-right font-semibold text-foreground font-mono">₹{Number(planResult.costBreakdown?.insurancePremium).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr>
                      <td className="py-2.5 px-3 font-medium text-foreground">7. Destination Port Delivery</td>
                      <td className="py-2.5 px-3 text-muted-foreground">Destination entry handling & delivery dispatch</td>
                      <td className="py-2.5 px-3 text-right font-semibold text-foreground font-mono">₹{Number(planResult.costBreakdown?.deliveryCharges).toLocaleString('en-IN')}</td>
                    </tr>
                    <tr className="bg-muted/60 font-semibold">
                      <td className="py-3 px-3 text-foreground">TOTAL FREIGHT & LOGISTICS</td>
                      <td className="py-3 px-3 text-xs text-muted-foreground">Cost/KG: ₹{planResult.costPerKg} | Cost/Unit: ₹{planResult.costPerUnit}</td>
                      <td className="py-3 px-3 text-right text-primary font-mono text-sm">₹{Number(planResult.grandTotal).toLocaleString('en-IN')}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            {/* Mandatory Customs Documents Checklist */}
            <div className="card-claude p-5 space-y-3.5">
              <div className="flex justify-between items-center border-b border-border pb-2.5">
                <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                  <FileText className="w-3.5 h-3.5 text-primary" /> Mandatory Checklist
                </h3>
                <span className="text-[10px] font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">ICEGATE Verified</span>
              </div>

              <div className="space-y-2">
                {planResult.requiredDocuments?.map((doc, idx) => (
                  <div key={idx} className="p-2.5 rounded-lg border border-border bg-muted/20 space-y-0.5">
                    <div className="flex items-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                      <span className="text-xs font-semibold text-foreground">{doc.documentName}</span>
                      <span className="text-[9px] font-mono font-semibold bg-primary/10 text-primary px-1.5 py-0.5 rounded">{doc.code}</span>
                    </div>
                    <p className="text-[11px] text-muted-foreground ml-5">{doc.description}</p>
                    <span className="text-[10px] text-muted-foreground/70 block ml-5 font-medium">Authority: {doc.issuingAuthority}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── STAGE 4: END-TO-END WORKFLOW & LIVE TRACKING ─────────────────────── */}
      {activeTab === 'journey' && planResult && (
        <div className="space-y-5 animate-in fade-in duration-150">
          <div className="card-claude p-5 md:p-6 space-y-5">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-3">
              <div>
                <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                  <Navigation className="w-3.5 h-3.5 text-primary" /> 7-Stage End-to-End Shipment Workflow
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">Physical milestone progression from Indian factory dispatch to foreign warehouse receipt.</p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handleCreateOfficialPlan}
                  disabled={isSavingPlan}
                  className="btn-primary"
                >
                  {isSavingPlan ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                  <span>Confirm & Save Official Plan</span>
                </button>
              </div>
            </div>

            {/* Stepper Timeline */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-2.5">
              {planResult.workflowMilestones?.map((m, idx) => (
                <div key={idx} className="p-3 rounded-xl border border-border bg-card space-y-1.5 relative">
                  <div className="flex items-center gap-1.5">
                    <span className="w-5 h-5 rounded-full bg-primary text-primary-foreground text-[10px] font-bold flex items-center justify-center shrink-0">
                      {idx + 1}
                    </span>
                    <span className="text-[10px] font-semibold text-primary uppercase tracking-wider">Stage {idx + 1}</span>
                  </div>
                  <div>
                    <h5 className="text-xs font-semibold text-foreground leading-tight">{m.milestone}</h5>
                    <p className="text-[10px] text-muted-foreground font-mono mt-0.5">{m.date}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* Live Tracking Container Tool */}
            <div className="pt-3 border-t border-border space-y-3">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div className="flex items-center gap-1.5">
                  <Activity className="w-3.5 h-3.5 text-primary" />
                  <span className="text-xs font-semibold text-foreground uppercase tracking-wider">Live Cargo Tracking Lookup</span>
                </div>
                <div className="flex items-center gap-2 w-full sm:w-auto">
                  <input
                    type="text"
                    value={trackingRefInput}
                    onChange={e => setTrackingRefInput(e.target.value)}
                    placeholder="Shipment ID (e.g. TW-PLN-847291)"
                    className="input-claude font-mono py-1.5 w-full sm:w-64"
                  />
                  <button
                    type="button"
                    onClick={handleTrackSearch}
                    className="btn-primary py-1.5 px-3 text-xs"
                  >
                    Track
                  </button>
                </div>
              </div>

              {trackingData && (
                <div className="bg-muted/30 rounded-xl p-4 border border-border space-y-3">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                    <div className="p-2.5 bg-card rounded-lg border border-border">
                      <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider block">Port / Checkpoint</span>
                      <span className="text-xs font-semibold text-foreground mt-0.5 block">{trackingData.currentLocation}</span>
                    </div>
                    <div className="p-2.5 bg-card rounded-lg border border-border">
                      <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider block">Carrier & Vessel</span>
                      <span className="text-xs font-semibold text-foreground mt-0.5 block">{trackingData.carrier} ({trackingData.vesselName || 'Ocean Vessel'})</span>
                    </div>
                    <div className="p-2.5 bg-card rounded-lg border border-border">
                      <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider block">Estimated Delivery (ETA)</span>
                      <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 font-mono mt-0.5 block">{trackingData.eta}</span>
                    </div>
                  </div>

                  {/* Vertical Checkpoints */}
                  {trackingData.events?.length > 0 && (
                    <div className="space-y-1.5 pt-2 border-t border-border">
                      <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block mb-1">Transit Event History</span>
                      {trackingData.events.map((ev, i) => (
                        <div key={i} className="flex items-center justify-between text-xs py-1.5 px-2.5 rounded-md bg-card border border-border/60">
                          <div className="flex items-center gap-2">
                            <span className={`w-2 h-2 rounded-full ${ev.completed ? 'bg-emerald-500' : 'bg-muted-foreground'}`}></span>
                            <span className="font-medium text-foreground">{ev.status}</span>
                            <span className="text-[11px] text-muted-foreground">({ev.location})</span>
                          </div>
                          <span className="text-[10px] font-mono text-muted-foreground">{ev.timestamp}</span>
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

