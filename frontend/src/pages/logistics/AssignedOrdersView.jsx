import React, { useState, useMemo } from 'react';
import {
  Eye, X, Loader2, Package, MapPin, Calendar, Truck,
  Search, Filter, Send, CheckCircle2, Clock, AlertCircle, ArrowUpDown, ShieldCheck, DollarSign
} from 'lucide-react';
import { countryFlag } from './utils';
import { proposalApi } from '../../services';

const SERVICE_OPTIONS = [
  'Door-to-Door Delivery',
  'Port-to-Port Freight',
  'Customs Clearance',
  'Export Documentation',
  'Cargo Insurance',
  'Cold Chain / Temperature Control',
  'Warehousing & Storage',
  'Express Air Courier'
];

export default function AssignedOrdersView({
  orders = [],
  myProposals = [],
  onProposalSubmitted,
  onAccept,
  onReject,
  addToast,
  fetchAll
}) {
  // ── Search & Filter State ──
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL'); // ALL, PENDING, PROPOSED, ACCEPTED
  const [filterMode, setFilterMode] = useState('ALL'); // ALL, Air, Sea, Road
  const [sortBy, setSortBy] = useState('NEWEST'); // NEWEST, OLDEST, VALUE_HIGH

  // ── Modals State ──
  const [inspectOrder, setInspectOrder] = useState(null);
  const [quotingOrder, setQuotingOrder] = useState(null);
  const [submittingProposal, setSubmittingProposal] = useState(false);

  // ── Proposal Form State ──
  const [proposedCost, setProposedCost] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [transitDays, setTransitDays] = useState('');
  const [pickupDate, setPickupDate] = useState('');
  const [deliveryDate, setDeliveryDate] = useState('');
  const [offeredServices, setOfferedServices] = useState(['Port-to-Port Freight', 'Customs Clearance']);
  const [notes, setNotes] = useState('');

  // ── Map proposals by orderId ──
  const proposalMap = useMemo(() => {
    const map = {};
    (myProposals || []).forEach(p => {
      map[p.orderId] = p;
    });
    return map;
  }, [myProposals]);

  // ── Filtered & Sorted Orders ──
  const filteredOrders = useMemo(() => {
    return orders.filter(o => {
      // Search
      const q = searchQuery.toLowerCase().trim();
      const matchSearch = !q ||
        o.id.toString().includes(q) ||
        (o.product || '').toLowerCase().includes(q) ||
        (o.country || '').toLowerCase().includes(q) ||
        (o.exporterName || '').toLowerCase().includes(q) ||
        (o.exporterCompany || '').toLowerCase().includes(q);

      if (!matchSearch) return false;

      // Status Filter
      const proposal = proposalMap[o.id];
      if (filterStatus === 'PENDING' && proposal) return false;
      if (filterStatus === 'PROPOSED' && (!proposal || proposal.status !== 'PENDING')) return false;
      if (filterStatus === 'ACCEPTED' && (!proposal || proposal.status !== 'ACCEPTED')) return false;

      // Mode Filter
      if (filterMode !== 'ALL') {
        const req = (o.shippingRequirements || '').toLowerCase();
        if (filterMode === 'AIR' && !req.includes('air')) return false;
        if (filterMode === 'SEA' && !req.includes('sea')) return false;
        if (filterMode === 'ROAD' && !req.includes('road')) return false;
      }

      return true;
    }).sort((a, b) => {
      if (sortBy === 'NEWEST') return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
      if (sortBy === 'OLDEST') return new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
      if (sortBy === 'VALUE_HIGH') return (b.rawValue || 0) - (a.rawValue || 0);
      return 0;
    });
  }, [orders, searchQuery, filterStatus, filterMode, sortBy, proposalMap]);

  // ── Open Quote Modal ──
  const handleOpenQuoteModal = (order) => {
    const existing = proposalMap[order.id];
    setQuotingOrder(order);
    if (existing) {
      setProposedCost(existing.estimatedCost || existing.proposedCost || '');
      setCurrency(existing.currency || 'INR');
      setTransitDays(existing.estimatedTransitDays || '');
      setPickupDate(existing.pickupDate ? existing.pickupDate.split('T')[0] : (existing.estimatedPickupDate ? existing.estimatedPickupDate.split('T')[0] : ''));
      setDeliveryDate(existing.expectedDeliveryDate ? existing.expectedDeliveryDate.split('T')[0] : (existing.estimatedDeliveryDate ? existing.estimatedDeliveryDate.split('T')[0] : ''));
      setOfferedServices(existing.services || existing.offeredServices || ['Port-to-Port Freight', 'Customs Clearance']);
      setNotes(existing.notes || '');
    } else {
      setProposedCost('');
      setCurrency('INR');
      setTransitDays('14');
      setPickupDate(new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0]);
      setDeliveryDate(new Date(Date.now() + 86400000 * 16).toISOString().split('T')[0]);
      setOfferedServices(['Port-to-Port Freight', 'Customs Clearance']);
      setNotes('');
    }
  };

  const toggleService = (svc) => {
    setOfferedServices(prev =>
      prev.includes(svc) ? prev.filter(s => s !== svc) : [...prev, svc]
    );
  };

  // ── Submit Proposal ──
  const handleSubmitProposal = async (e) => {
    e.preventDefault();
    if (!quotingOrder) return;

    if (!proposedCost || Number(proposedCost) <= 0) {
      addToast('Please specify a valid proposal cost.', 'error');
      return;
    }
    if (offeredServices.length === 0) {
      addToast('Please select at least one offered service.', 'error');
      return;
    }

    setSubmittingProposal(true);
    try {
      await proposalApi.submitProposal({
        orderId: quotingOrder.id,
        services: offeredServices,
        offeredServices: offeredServices,
        estimatedCost: Number(proposedCost),
        proposedCost: Number(proposedCost),
        currency,
        estimatedTransitDays: transitDays ? parseInt(transitDays, 10) : null,
        pickupDate: pickupDate || null,
        expectedDeliveryDate: deliveryDate || null,
        notes: notes || '',
      });

      addToast(`Proposal for Order #${quotingOrder.id} successfully submitted!`, 'success');
      setQuotingOrder(null);
      if (onProposalSubmitted) onProposalSubmitted();
      else if (fetchAll) fetchAll();
    } catch (err) {
      addToast(err.message || 'Failed to submit proposal', 'error');
    } finally {
      setSubmittingProposal(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Available Export Assignments</h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">
            Review international export orders, calculate freight logistics, and issue competitive service proposals.
          </p>
        </div>
        <div className="text-xs font-bold text-slate-500 bg-white border border-slate-200/80 px-4 py-2 rounded-xl shadow-xs">
          Showing <span className="text-sky-600 font-black">{filteredOrders.length}</span> of {orders.length} orders
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-sm flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
        
        {/* Search */}
        <div className="relative flex-grow max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by Order ID, Product, Country, or Exporter..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:bg-white transition-all"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Status Filter */}
          <div className="flex items-center gap-1 bg-slate-50 p-1 rounded-xl border border-slate-200">
            {[
              { id: 'ALL', label: 'All' },
              { id: 'PENDING', label: 'Unquoted' },
              { id: 'PROPOSED', label: 'Quoted' },
              { id: 'ACCEPTED', label: 'Won' },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setFilterStatus(tab.id)}
                className={`px-2.5 py-1 rounded-lg text-xxs font-bold transition-all cursor-pointer ${
                  filterStatus === tab.id
                    ? 'bg-white text-sky-600 shadow-xs'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Mode Selector */}
          <select
            value={filterMode}
            onChange={(e) => setFilterMode(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xxs font-bold text-slate-700 focus:outline-none focus:border-sky-500 cursor-pointer"
          >
            <option value="ALL">All Modes</option>
            <option value="SEA">Sea Freight</option>
            <option value="AIR">Air Freight</option>
            <option value="ROAD">Road Transport</option>
          </select>

          {/* Sort Selector */}
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xxs font-bold text-slate-700 focus:outline-none focus:border-sky-500 cursor-pointer"
          >
            <option value="NEWEST">Newest First</option>
            <option value="OLDEST">Oldest First</option>
            <option value="VALUE_HIGH">Highest Order Value</option>
          </select>
        </div>

      </div>

      {/* Orders Table */}
      <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/70 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                <th className="py-4 px-5">Order ID</th>
                <th className="py-4 px-5">Exporter</th>
                <th className="py-4 px-5">Product Details</th>
                <th className="py-4 px-5">Quantity</th>
                <th className="py-4 px-5">Destination</th>
                <th className="py-4 px-5">Preferred Mode</th>
                <th className="py-4 px-5">Proposal Status</th>
                <th className="py-4 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
              {filteredOrders.length === 0 ? (
                <tr>
                  <td colSpan="8" className="py-16 text-center">
                    <Package className="w-10 h-10 text-slate-200 mx-auto mb-3" />
                    <span className="text-sm font-bold text-slate-500 block">No matching export orders found</span>
                    <span className="text-[10px] text-slate-400 mt-1 block">
                      Try clearing search filters or check back shortly for new export bookings.
                    </span>
                  </td>
                </tr>
              ) : (
                filteredOrders.map(o => {
                  const proposal = proposalMap[o.id];
                  return (
                    <tr key={o.id} className="hover:bg-slate-50/50 transition-colors">
                      {/* Order ID */}
                      <td className="py-4 px-5">
                        <span className="font-mono font-bold text-slate-900 bg-slate-100 px-2 py-1 rounded-md text-[11px]">
                          #{o.id}
                        </span>
                        <span className="block text-[9px] text-slate-400 mt-1">
                          {o.createdAt ? new Date(o.createdAt).toLocaleDateString('en-IN') : 'Recent'}
                        </span>
                      </td>

                      {/* Exporter */}
                      <td className="py-4 px-5">
                        <span className="block font-bold text-slate-900">{o.exporterName || 'Verified Exporter'}</span>
                        <span className="block text-[10px] text-slate-400 truncate max-w-[150px]">{o.exporterCompany || 'Trading Corp'}</span>
                      </td>

                      {/* Product */}
                      <td className="py-4 px-5">
                        <span className="text-slate-900 font-bold block">{o.product}</span>
                        <span className="font-mono text-[10px] text-slate-400">HS: {o.hscode || 'Unspecified'}</span>
                      </td>

                      {/* Quantity & Value */}
                      <td className="py-4 px-5">
                        <span className="block text-slate-800 font-semibold">{o.qty}</span>
                        <span className="text-[10px] text-slate-400 font-mono">{o.value}</span>
                      </td>

                      {/* Destination */}
                      <td className="py-4 px-5">
                        <div className="flex items-center gap-2">
                          <span className="text-lg">{countryFlag(o.country)}</span>
                          <div>
                            <span className="font-bold text-slate-800 block">{o.country}</span>
                            <span className="text-[9px] text-slate-400 truncate max-w-[120px] block">
                              From: {o.pickupLocation || 'Domestic Hub'}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Mode */}
                      <td className="py-4 px-5">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 text-[10px] font-bold">
                          <Truck className="w-3 h-3 text-sky-500" />
                          {o.shippingRequirements || 'Sea Freight'}
                        </span>
                      </td>

                      {/* Proposal Status */}
                      <td className="py-4 px-5">
                        {proposal ? (
                          proposal.status === 'ACCEPTED' ? (
                            <span className="inline-flex items-center gap-1.5 text-[10px] font-black text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full">
                              <CheckCircle2 className="w-3 h-3 text-emerald-500" /> Proposal Accepted
                            </span>
                          ) : proposal.status === 'REJECTED' ? (
                            <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-red-600 bg-red-50 border border-red-200 px-2.5 py-1 rounded-full">
                              <AlertCircle className="w-3 h-3 text-red-500" /> Proposal Declined
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-full">
                              <Clock className="w-3 h-3 text-amber-500 animate-pulse" />
                              Quoted: {proposal.currency} {Number(proposal.estimatedCost ?? proposal.proposedCost ?? 0).toLocaleString()}
                            </span>
                          )
                        ) : (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-slate-400 bg-slate-50 px-2 py-0.5 rounded-full">
                            No Quote Submitted
                          </span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                        <button
                          onClick={() => setInspectOrder(o)}
                          className="px-2.5 py-1.5 text-xxs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg cursor-pointer transition-all border border-slate-200"
                        >
                          <Eye className="w-3 h-3 inline mr-1" /> Inspect
                        </button>

                        <button
                          onClick={() => handleOpenQuoteModal(o)}
                          className={`px-3 py-1.5 text-xxs font-bold rounded-lg cursor-pointer transition-all shadow-xs inline-flex items-center gap-1 ${
                            proposal
                              ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300'
                              : 'bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white'
                          }`}
                        >
                          <Send className="w-3 h-3" />
                          {proposal ? 'View / Edit Quote' : 'Submit Proposal'}
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── QUOTE PROPOSAL MODAL ── */}
      {quotingOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            onClick={() => !submittingProposal && setQuotingOrder(null)}
            className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs"
          ></div>
          <div className="relative w-full max-w-2xl bg-white border border-slate-200 rounded-3xl p-6 sm:p-7 shadow-2xl animate-in scale-in duration-200 z-10 max-h-[90vh] overflow-y-auto">
            
            {/* Header */}
            <div className="flex justify-between items-center border-b border-slate-100 pb-4 mb-4">
              <div>
                <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-sky-50 text-sky-600 text-[10px] font-bold uppercase tracking-wider mb-1">
                  Freight Proposal Builder
                </div>
                <h3 className="text-base font-black text-slate-900">
                  Quote for Order #{quotingOrder.id} · {quotingOrder.product}
                </h3>
              </div>
              <button
                onClick={() => setQuotingOrder(null)}
                className="p-1.5 hover:bg-slate-100 rounded-full cursor-pointer text-slate-400 hover:text-slate-700 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Cargo Quick Summary Banner */}
            <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4 mb-5 text-xs grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Quantity</span>
                <span className="font-bold text-slate-800">{quotingOrder.qty}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Export Value</span>
                <span className="font-bold text-slate-800 font-mono">{quotingOrder.value}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Origin Port / City</span>
                <span className="font-bold text-slate-800 truncate block">{quotingOrder.pickupLocation || 'Mumbai, IN'}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Destination</span>
                <span className="font-bold text-slate-800 flex items-center gap-1">
                  {countryFlag(quotingOrder.country)} {quotingOrder.country}
                </span>
              </div>
            </div>

            {/* Proposal Form */}
            <form onSubmit={handleSubmitProposal} className="space-y-5">
              
              {/* Service Selection */}
              <div>
                <div className="flex justify-between items-center mb-2.5">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                    Included Logistics Services <span className="text-slate-400 normal-case">(select all offered)</span>
                  </label>
                  <span className="text-[10px] font-bold text-sky-600 bg-sky-50 px-2.5 py-0.5 rounded-full">
                    {offeredServices.length} selected
                  </span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                  {SERVICE_OPTIONS.map(svc => {
                    const active = offeredServices.includes(svc);
                    return (
                      <button
                        type="button"
                        key={svc}
                        onClick={() => toggleService(svc)}
                        className={`p-2.5 rounded-xl text-left transition-all cursor-pointer flex flex-col justify-between min-h-[64px] border ${
                          active
                            ? 'bg-sky-50/90 border-sky-400 text-sky-900 shadow-xs ring-1 ring-sky-400/30'
                            : 'bg-slate-50/60 border-slate-200 text-slate-700 hover:bg-white hover:border-slate-300'
                        }`}
                      >
                        <span className="text-[11px] font-bold leading-tight">{svc}</span>
                        <div className="flex items-center justify-between mt-1 pt-1 border-t border-slate-200/50">
                          <span className={`text-[9px] font-extrabold ${active ? 'text-sky-600' : 'text-slate-400'}`}>
                            {active ? '✓ Selected' : '+ Add Service'}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Price & Currency & Transit Days */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="space-y-1">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                    Proposed Freight Cost *
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      required
                      min="1"
                      step="any"
                      value={proposedCost}
                      onChange={(e) => setProposedCost(e.target.value)}
                      placeholder="e.g. 45000"
                      className="w-full pl-8 pr-3 py-2 text-xs border border-slate-200 rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:border-sky-500"
                    />
                    <DollarSign className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                    Currency
                  </label>
                  <select
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value)}
                    className="w-full px-3 py-2 text-xs border border-slate-200 rounded-xl font-bold text-slate-700 focus:outline-none focus:border-sky-500 cursor-pointer"
                  >
                    <option value="INR">INR (₹)</option>
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="AED">AED (د.إ)</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                    Transit Days (Est.)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="180"
                    value={transitDays}
                    onChange={(e) => setTransitDays(e.target.value)}
                    placeholder="e.g. 14"
                    className="w-full px-3 py-2 text-xs border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              {/* Pickup & Delivery Dates */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                    Estimated Cargo Pickup Date
                  </label>
                  <input
                    type="date"
                    value={pickupDate}
                    onChange={(e) => setPickupDate(e.target.value)}
                    className="w-full px-3 py-2 text-xs border border-slate-200 rounded-xl text-slate-700 focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                    Estimated Delivery ETA
                  </label>
                  <input
                    type="date"
                    value={deliveryDate}
                    onChange={(e) => setDeliveryDate(e.target.value)}
                    className="w-full px-3 py-2 text-xs border border-slate-200 rounded-xl text-slate-700 focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              {/* Notes & Special Terms */}
              <div className="space-y-1">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  Quote Terms & Scope Notes
                </label>
                <textarea
                  rows="2"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="e.g. Rate includes ocean freight, THC origin, export documentation, and cargo GPS milestone tracking."
                  className="w-full px-3.5 py-2.5 text-xs border border-slate-200 rounded-xl text-slate-700 focus:outline-none focus:border-sky-500"
                ></textarea>
              </div>

              {/* Live Proposal Summary Card */}
              <div className="p-4 rounded-2xl bg-gradient-to-r from-sky-50 to-indigo-50 border border-sky-100 flex items-center justify-between text-xs">
                <div>
                  <span className="text-[9px] font-bold text-sky-700 uppercase tracking-wider block">Proposal Total</span>
                  <span className="text-lg font-black text-slate-900 font-mono">
                    {currency} {proposedCost ? Number(proposedCost).toLocaleString() : '0.00'}
                  </span>
                  <span className="block text-[10px] text-slate-500 mt-0.5">
                    Est. Transit: {transitDays || '—'} days · {offeredServices.length} service components
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-indigo-500" />
                  <span className="text-[10px] font-semibold text-slate-600 max-w-[140px] leading-tight">
                    Secured by Trade platform contract terms
                  </span>
                </div>
              </div>

              {/* Submit Buttons */}
              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setQuotingOrder(null)}
                  disabled={submittingProposal}
                  className="px-4 py-2.5 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingProposal}
                  className="px-6 py-2.5 text-xs font-bold text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-50 flex items-center gap-2"
                >
                  {submittingProposal ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                  Submit Proposal to Exporter
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

      {/* ── INSPECT ORDER MODAL ── */}
      {inspectOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setInspectOrder(null)} className="absolute inset-0 bg-slate-900/30 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-200 z-10 space-y-5">
            
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[10px] font-black text-sky-600 uppercase tracking-widest block">Cargo Specification</span>
                <h3 className="text-base font-black text-slate-900">Order #{inspectOrder.id}</h3>
              </div>
              <button
                onClick={() => setInspectOrder(null)}
                className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 border border-slate-100 rounded-xl bg-slate-50/50 space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Exporter Contact</span>
                <span className="font-extrabold text-slate-900 block">{inspectOrder.exporterName || 'Verified Exporter'}</span>
                <span className="text-slate-500 block">{inspectOrder.exporterCompany}</span>
              </div>

              <div className="p-3.5 border border-slate-100 rounded-xl bg-slate-50/50 space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Commodity Specs</span>
                <span className="font-extrabold text-slate-900 block">{inspectOrder.product}</span>
                <span className="font-mono text-slate-500 block text-[10px]">HS Code: {inspectOrder.hscode || '—'}</span>
                <span className="text-slate-500 block">Unit Weight: {inspectOrder.productWeightPerUnit ? `${inspectOrder.productWeightPerUnit} kg` : 'N/A'}</span>
              </div>

              <div className="p-3.5 border border-slate-100 rounded-xl bg-slate-50/50 space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Pickup Point</span>
                <span className="font-semibold text-slate-700 block flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-sky-500" />
                  {inspectOrder.pickupLocation || 'Domestic Warehouse / Factory'}
                </span>
              </div>

              <div className="p-3.5 border border-slate-100 rounded-xl bg-slate-50/50 space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Delivery Destination</span>
                <span className="font-semibold text-slate-700 block flex items-center gap-1">
                  <span className="text-base">{countryFlag(inspectOrder.country)}</span>
                  {inspectOrder.country}
                </span>
              </div>
            </div>

            {inspectOrder.specialInstructions && (
              <div className="p-3.5 rounded-xl border border-amber-100 bg-amber-50/50 text-xs">
                <span className="text-[9px] font-black text-amber-700 uppercase tracking-wider block mb-1">
                  Exporter Handling Instructions
                </span>
                <p className="text-slate-700 italic">{inspectOrder.specialInstructions}</p>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                onClick={() => setInspectOrder(null)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Close
              </button>
              <button
                onClick={() => {
                  const o = inspectOrder;
                  setInspectOrder(null);
                  handleOpenQuoteModal(o);
                }}
                className="px-5 py-2 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow-xs"
              >
                Quote This Order
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
