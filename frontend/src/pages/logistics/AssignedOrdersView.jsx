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
    <div className="space-y-6 animate-in fade-in duration-200">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground tracking-tight">Available Export Assignments</h1>
          <p className="text-xs text-muted-foreground mt-1">
            Review international export orders, calculate freight logistics, and issue competitive service proposals.
          </p>
        </div>
        <div className="text-xs font-medium text-muted-foreground card-claude px-3 py-1.5 rounded-lg shadow-sm">
          Showing <span className="text-primary font-semibold">{filteredOrders.length}</span> of {orders.length} orders
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="card-claude p-3.5 flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
        
        {/* Search */}
        <div className="relative flex-grow max-w-md flex items-center">
          <Search className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by Order ID, Product, Country, or Exporter..."
            className="input-claude pl-10 pr-9 py-1.5"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Status Filter */}
          <div className="flex items-center gap-1 bg-muted/40 p-1 rounded-lg border border-border">
            {[
              { id: 'ALL', label: 'All' },
              { id: 'PENDING', label: 'Unquoted' },
              { id: 'PROPOSED', label: 'Quoted' },
              { id: 'ACCEPTED', label: 'Won' },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setFilterStatus(tab.id)}
                className={`px-2.5 py-1 rounded-md text-xs font-medium transition-all cursor-pointer ${
                  filterStatus === tab.id
                    ? 'bg-card text-primary font-semibold shadow-xs'
                    : 'text-muted-foreground hover:text-foreground'
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
            className="input-claude py-1.5 px-3 text-xs cursor-pointer w-auto"
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
            className="input-claude py-1.5 px-3 text-xs cursor-pointer w-auto"
          >
            <option value="NEWEST">Newest First</option>
            <option value="OLDEST">Oldest First</option>
            <option value="VALUE_HIGH">Highest Value</option>
          </select>
        </div>

      </div>

      {/* Orders Table */}
      <div className="card-claude overflow-hidden">
        <div className="overflow-x-auto">
          <table className="table-claude">
            <thead>
              <tr>
                <th className="py-3 px-4">Order ID</th>
                <th className="py-3 px-4">Exporter</th>
                <th className="py-3 px-4">Product Details</th>
                <th className="py-3 px-4">Quantity</th>
                <th className="py-3 px-4">Destination</th>
                <th className="py-3 px-4">Preferred Mode</th>
                <th className="py-3 px-4">Proposal Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border text-xs">
              {filteredOrders.length === 0 ? (
                <tr>
                  <td colSpan="8" className="py-12 text-center">
                    <Package className="w-8 h-8 text-muted-foreground mx-auto mb-2" />
                    <span className="text-xs font-semibold text-foreground block">No matching export orders found</span>
                    <span className="text-[11px] text-muted-foreground mt-1 block">
                      Try clearing search filters or check back shortly for new export bookings.
                    </span>
                  </td>
                </tr>
              ) : (
                filteredOrders.map(o => {
                  const proposal = proposalMap[o.id];
                  return (
                    <tr key={o.id} className="hover:bg-muted/40 transition-colors">
                      {/* Order ID */}
                      <td className="py-3.5 px-4">
                        <span className="font-mono font-semibold text-foreground bg-muted px-2 py-0.5 rounded text-[11px]">
                          #{o.id}
                        </span>
                        <span className="block text-[10px] text-muted-foreground mt-0.5">
                          {o.createdAt ? new Date(o.createdAt).toLocaleDateString('en-IN') : 'Recent'}
                        </span>
                      </td>

                      {/* Exporter */}
                      <td className="py-3.5 px-4">
                        <span className="block font-semibold text-foreground">{o.exporterName || 'Verified Exporter'}</span>
                        <span className="block text-[10px] text-muted-foreground truncate max-w-[140px]">{o.exporterCompany || 'Trading Corp'}</span>
                      </td>

                      {/* Product */}
                      <td className="py-3.5 px-4">
                        <span className="text-foreground font-semibold block">{o.product}</span>
                        <span className="font-mono text-[10px] text-muted-foreground">HS: {o.hscode || 'Unspecified'}</span>
                      </td>

                      {/* Quantity & Value */}
                      <td className="py-3.5 px-4">
                        <span className="block text-foreground font-medium">{o.qty}</span>
                        <span className="text-[10px] text-muted-foreground font-mono">{o.value}</span>
                      </td>

                      {/* Destination */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2">
                          <span className="text-base">{countryFlag(o.country)}</span>
                          <div>
                            <span className="font-semibold text-foreground block">{o.country}</span>
                            <span className="text-[10px] text-muted-foreground truncate max-w-[120px] block">
                              From: {o.pickupLocation || 'Domestic Hub'}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Mode */}
                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-muted text-foreground text-[10px] font-medium border border-border">
                          <Truck className="w-3 h-3 text-primary" />
                          {o.shippingRequirements || 'Sea Freight'}
                        </span>
                      </td>

                      {/* Proposal Status */}
                      <td className="py-3.5 px-4">
                        {proposal ? (
                          proposal.status === 'ACCEPTED' ? (
                            <span className="inline-flex items-center gap-1.5 text-[10px] font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 rounded-full">
                              <CheckCircle2 className="w-3 h-3 text-emerald-500" /> Proposal Accepted
                            </span>
                          ) : proposal.status === 'REJECTED' ? (
                            <span className="inline-flex items-center gap-1.5 text-[10px] font-semibold text-destructive bg-destructive/10 border border-destructive/20 px-2.5 py-0.5 rounded-full">
                              <AlertCircle className="w-3 h-3" /> Declined
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-700 dark:text-amber-300 bg-amber-500/10 border border-amber-500/20 px-2.5 py-0.5 rounded-full">
                              <Clock className="w-3 h-3 text-amber-500" />
                              Quoted: {proposal.currency} {Number(proposal.estimatedCost ?? proposal.proposedCost ?? 0).toLocaleString()}
                            </span>
                          )
                        ) : (
                          <span className="inline-flex items-center gap-1 text-[10px] font-medium text-muted-foreground bg-muted px-2 py-0.5 rounded-full">
                            No Quote
                          </span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-3.5 px-4 text-right space-x-1.5 whitespace-nowrap">
                        <button
                          onClick={() => setInspectOrder(o)}
                          className="btn-secondary py-1 px-2.5 text-xs"
                        >
                          <Eye className="w-3 h-3 inline mr-1" /> Inspect
                        </button>

                        <button
                          onClick={() => handleOpenQuoteModal(o)}
                          className={`btn-primary py-1 px-3 text-xs ${proposal ? 'btn-outline border-border text-foreground hover:bg-muted' : ''}`}
                        >
                          <Send className="w-3 h-3 mr-1" />
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
            className="absolute inset-0 bg-background/80 backdrop-blur-sm"
          ></div>
          <div className="relative w-full max-w-2xl bg-popover text-popover-foreground border border-border rounded-2xl p-6 shadow-xl animate-in scale-in duration-150 z-10 max-h-[90vh] overflow-y-auto space-y-4">
            
            {/* Header */}
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-primary/10 text-primary text-[10px] font-semibold uppercase tracking-wider mb-1">
                  Freight Proposal Builder
                </div>
                <h3 className="text-base font-semibold text-foreground">
                  Quote for Order #{quotingOrder.id} · {quotingOrder.product}
                </h3>
              </div>
              <button
                onClick={() => setQuotingOrder(null)}
                className="p-1.5 hover:bg-muted rounded-md cursor-pointer text-muted-foreground transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Cargo Quick Summary Banner */}
            <div className="bg-muted/30 border border-border rounded-xl p-3.5 text-xs grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Quantity</span>
                <span className="font-semibold text-foreground">{quotingOrder.qty}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Export Value</span>
                <span className="font-semibold text-foreground font-mono">{quotingOrder.value}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Origin Port</span>
                <span className="font-semibold text-foreground truncate block">{quotingOrder.pickupLocation || 'Mumbai, IN'}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Destination</span>
                <span className="font-semibold text-foreground flex items-center gap-1">
                  {countryFlag(quotingOrder.country)} {quotingOrder.country}
                </span>
              </div>
            </div>

            {/* Proposal Form */}
            <form onSubmit={handleSubmitProposal} className="space-y-4">
              
              {/* Service Selection */}
              <div>
                <div className="flex justify-between items-center mb-2">
                  <label className="text-xs font-semibold text-foreground">
                    Included Logistics Services <span className="text-muted-foreground font-normal">(select all offered)</span>
                  </label>
                  <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded-full">
                    {offeredServices.length} selected
                  </span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {SERVICE_OPTIONS.map(svc => {
                    const active = offeredServices.includes(svc);
                    return (
                      <button
                        type="button"
                        key={svc}
                        onClick={() => toggleService(svc)}
                        className={`p-2.5 rounded-lg text-left transition-all cursor-pointer flex flex-col justify-between min-h-[58px] border ${
                          active
                            ? 'bg-primary/10 border-primary text-primary font-semibold'
                            : 'bg-card border-border text-foreground hover:bg-muted'
                        }`}
                      >
                        <span className="text-xs leading-tight">{svc}</span>
                        <span className={`text-[9px] font-medium mt-1 ${active ? 'text-primary' : 'text-muted-foreground'}`}>
                          {active ? '✓ Selected' : '+ Add'}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Price & Currency & Transit Days */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-foreground">
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
                      className="input-claude font-mono font-semibold"
                    />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-foreground">
                    Currency
                  </label>
                  <select
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value)}
                    className="input-claude cursor-pointer font-semibold"
                  >
                    <option value="INR">INR (₹)</option>
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="AED">AED (د.إ)</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-foreground">
                    Transit Days (Est.)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="180"
                    value={transitDays}
                    onChange={(e) => setTransitDays(e.target.value)}
                    placeholder="e.g. 14"
                    className="input-claude"
                  />
                </div>
              </div>

              {/* Pickup & Delivery Dates */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-foreground">
                    Estimated Pickup Date
                  </label>
                  <input
                    type="date"
                    value={pickupDate}
                    onChange={(e) => setPickupDate(e.target.value)}
                    className="input-claude"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-foreground">
                    Estimated Delivery ETA
                  </label>
                  <input
                    type="date"
                    value={deliveryDate}
                    onChange={(e) => setDeliveryDate(e.target.value)}
                    className="input-claude"
                  />
                </div>
              </div>

              {/* Notes & Special Terms */}
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">
                  Quote Terms & Scope Notes
                </label>
                <textarea
                  rows="2"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="e.g. Rate includes ocean freight, THC origin, export documentation, and cargo milestone tracking."
                  className="input-claude"
                ></textarea>
              </div>

              {/* Live Proposal Summary Card */}
              <div className="p-3.5 rounded-xl bg-accent/40 border border-border flex items-center justify-between text-xs">
                <div>
                  <span className="text-[10px] font-semibold text-primary uppercase tracking-wider block">Proposal Total</span>
                  <span className="text-base font-bold text-foreground font-mono">
                    {currency} {proposedCost ? Number(proposedCost).toLocaleString() : '0.00'}
                  </span>
                  <span className="block text-[11px] text-muted-foreground mt-0.5">
                    Est. Transit: {transitDays || '—'} days · {offeredServices.length} service components
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-primary" />
                  <span className="text-[11px] text-muted-foreground max-w-[140px] leading-tight">
                    Secured by Trade platform contract terms
                  </span>
                </div>
              </div>

              {/* Submit Buttons */}
              <div className="flex justify-end gap-2 pt-2 border-t border-border">
                <button
                  type="button"
                  onClick={() => setQuotingOrder(null)}
                  disabled={submittingProposal}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingProposal}
                  className="btn-primary"
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
          <div onClick={() => setInspectOrder(null)} className="absolute inset-0 bg-background/80 backdrop-blur-sm"></div>
          <div className="relative w-full max-w-lg bg-popover text-popover-foreground border border-border rounded-2xl p-6 shadow-xl animate-in scale-in duration-150 z-10 space-y-4">
            
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <span className="text-[10px] font-semibold text-primary uppercase tracking-wider block">Cargo Specification</span>
                <h3 className="text-base font-semibold text-foreground">Order #{inspectOrder.id}</h3>
              </div>
              <button
                onClick={() => setInspectOrder(null)}
                className="p-1.5 hover:bg-muted rounded-md cursor-pointer text-muted-foreground transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 border border-border rounded-xl bg-muted/20 space-y-0.5">
                <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider block">Exporter Contact</span>
                <span className="font-semibold text-foreground block">{inspectOrder.exporterName || 'Verified Exporter'}</span>
                <span className="text-muted-foreground block">{inspectOrder.exporterCompany}</span>
              </div>

              <div className="p-3 border border-border rounded-xl bg-muted/20 space-y-0.5">
                <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider block">Commodity Specs</span>
                <span className="font-semibold text-foreground block">{inspectOrder.product}</span>
                <span className="font-mono text-muted-foreground block text-[10px]">HS Code: {inspectOrder.hscode || '—'}</span>
                <span className="text-muted-foreground block">Unit Weight: {inspectOrder.productWeightPerUnit ? `${inspectOrder.productWeightPerUnit} kg` : 'N/A'}</span>
              </div>

              <div className="p-3 border border-border rounded-xl bg-muted/20 space-y-0.5">
                <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider block">Pickup Point</span>
                <span className="font-medium text-foreground block flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-primary" />
                  {inspectOrder.pickupLocation || 'Domestic Warehouse / Factory'}
                </span>
              </div>

              <div className="p-3 border border-border rounded-xl bg-muted/20 space-y-0.5">
                <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider block">Delivery Destination</span>
                <span className="font-medium text-foreground block flex items-center gap-1">
                  <span className="text-base">{countryFlag(inspectOrder.country)}</span>
                  {inspectOrder.country}
                </span>
              </div>
            </div>

            {inspectOrder.specialInstructions && (
              <div className="p-3 rounded-xl border border-amber-500/20 bg-amber-500/10 text-xs text-foreground">
                <span className="text-[10px] font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wider block mb-0.5">
                  Exporter Handling Instructions
                </span>
                <p className="italic">{inspectOrder.specialInstructions}</p>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-border">
              <button
                onClick={() => setInspectOrder(null)}
                className="btn-secondary"
              >
                Close
              </button>
              <button
                onClick={() => {
                  const o = inspectOrder;
                  setInspectOrder(null);
                  handleOpenQuoteModal(o);
                }}
                className="btn-primary"
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

