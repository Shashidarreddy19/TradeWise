import React, { useState } from 'react';
import {
  Eye, X, Check, Truck, Package, Clock, ShieldCheck,
  AlertCircle, Send, CheckCircle2, MapPin, Calendar, DollarSign, Loader2
} from 'lucide-react';
import { proposalApi, shipmentsApi } from '../../services';

export default function OrdersView({
  orders = [],
  shipments = [],
  orderFilter,
  setOrderFilter,
  selectedOrder,
  setSelectedOrder,
  selectedShipment,
  setSelectedShipment,
  addToast,
  fetchOrders,
  fetchShipments,
}) {
  // Proposals Modal State
  const [viewingProposalsOrder, setViewingProposalsOrder] = useState(null);
  const [orderProposals, setOrderProposals] = useState([]);
  const [loadingProposals, setLoadingProposals] = useState(false);
  const [actioningProposalId, setActioningProposalId] = useState(null);

  // Open proposals modal for an order
  const handleOpenProposals = async (order) => {
    setViewingProposalsOrder(order);
    setLoadingProposals(true);
    try {
      const res = await proposalApi.getProposalsForOrder(order.id);
      setOrderProposals(res.data || []);
    } catch (err) {
      addToast(err.message || 'Failed to load proposals', 'error');
      setOrderProposals([]);
    } finally {
      setLoadingProposals(false);
    }
  };

  // Accept a proposal
  const handleAcceptProposal = async (proposalId) => {
    setActioningProposalId(proposalId);
    try {
      await proposalApi.acceptProposal(proposalId);
      addToast('Logistics proposal accepted! Shipment has been scheduled.', 'success');
      setViewingProposalsOrder(null);
      if (fetchOrders) await fetchOrders();
      if (fetchShipments) await fetchShipments();
    } catch (err) {
      addToast(err.message || 'Failed to accept proposal', 'error');
    } finally {
      setActioningProposalId(null);
    }
  };

  // Reject a proposal
  const handleRejectProposal = async (proposalId) => {
    setActioningProposalId(proposalId);
    try {
      await proposalApi.rejectProposal(proposalId);
      addToast('Proposal rejected.', 'info');
      // Refresh modal list
      if (viewingProposalsOrder) {
        const res = await proposalApi.getProposalsForOrder(viewingProposalsOrder.id);
        setOrderProposals(res.data || []);
      }
    } catch (err) {
      addToast(err.message || 'Failed to reject proposal', 'error');
    } finally {
      setActioningProposalId(null);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Export Orders & Freight Contracts</h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">
            Review export shipments, evaluate competing logistics proposals, and track multimodal milestones.
          </p>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 border-b border-slate-200/80 text-xs font-bold text-slate-400 pb-1">
        {['All', 'Pending', 'Accepted', 'Shipped', 'Delivered'].map((status) => (
          <button
            key={status}
            onClick={() => setOrderFilter(status)}
            className={`pb-2 px-2 cursor-pointer transition-colors relative ${
              orderFilter === status ? 'text-sky-600 font-extrabold' : 'hover:text-slate-700'
            }`}
          >
            {status}
            {orderFilter === status && (
              <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div>
            )}
          </button>
        ))}
      </div>

      {/* Orders Table */}
      <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/70 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                <th className="py-4 px-5">Order ID</th>
                <th className="py-4 px-5">Commodity / HS Code</th>
                <th className="py-4 px-5">Destination</th>
                <th className="py-4 px-5">Quantity</th>
                <th className="py-4 px-5">Order Value</th>
                <th className="py-4 px-5">Logistics Carrier</th>
                <th className="py-4 px-5">Order Status</th>
                <th className="py-4 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
              {orders
                .filter(o => orderFilter === 'All' || o.status === orderFilter)
                .map(o => {
                  const shipment = shipments.find(s => s.orderId === o.id);
                  const partner = shipment
                    ? (shipment.logisticsCompany || shipment.logistics || 'Assigned Partner')
                    : (o.logisticsPartner !== 'TBD' ? o.logisticsPartner : 'Awaiting Quote');

                  return (
                    <tr key={o.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-4 px-5">
                        <span className="font-mono text-slate-900 font-bold bg-slate-100 px-2 py-0.5 rounded text-[11px]">
                          #{o.id}
                        </span>
                        <span className="block text-[9px] text-slate-400 mt-0.5">{o.date || 'Recent'}</span>
                      </td>

                      <td className="py-4 px-5">
                        <span className="text-slate-900 font-bold block">{o.product}</span>
                        <span className="font-mono text-[10px] text-slate-400">HS: {o.hscode || '—'}</span>
                      </td>

                      <td className="py-4 px-5">
                        <span className="font-bold text-slate-800 block">{o.country}</span>
                        <span className="text-[9px] text-slate-400 block">{o.shippingRequirements || 'Sea Freight'}</span>
                      </td>

                      <td className="py-4 px-5 text-slate-600">{o.qty}</td>

                      <td className="py-4 px-5 text-slate-900 font-bold font-mono">
                        {o.value.startsWith('₹') ? o.value : `₹${o.value}`}
                      </td>

                      <td className="py-4 px-5">
                        <span className="text-slate-800 font-bold block">{partner}</span>
                        {shipment?.tracking && (
                          <span className="text-[10px] font-mono text-sky-600 font-semibold block">
                            {shipment.tracking}
                          </span>
                        )}
                      </td>

                      <td className="py-4 px-5">
                        {o.status === 'Pending' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full">
                            <Clock className="w-3 h-3 text-amber-500 animate-pulse" /> Awaiting Logistics
                          </span>
                        )}
                        {o.status === 'Accepted' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
                            <Check className="w-3 h-3 text-emerald-500" /> Carrier Confirmed
                          </span>
                        )}
                        {o.status === 'Shipped' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-sky-700 bg-sky-50 border border-sky-200 px-2 py-0.5 rounded-full">
                            <Truck className="w-3 h-3 text-sky-500" /> In Transit
                          </span>
                        )}
                        {o.status === 'Delivered' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-purple-700 bg-purple-50 border border-purple-200 px-2 py-0.5 rounded-full">
                            <CheckCircle2 className="w-3 h-3 text-purple-500" /> Delivered
                          </span>
                        )}
                        {o.status === 'Rejected' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-red-600 bg-red-50 border border-red-200 px-2 py-0.5 rounded-full">
                            <AlertCircle className="w-3 h-3 text-red-500" /> Declined
                          </span>
                        )}
                      </td>

                      <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                        <button
                          onClick={() => setSelectedOrder(o)}
                          className="px-2.5 py-1.5 text-xxs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg cursor-pointer transition-all border border-slate-200"
                        >
                          <Eye className="w-3 h-3 inline mr-1" /> Specs
                        </button>

                        {/* Proposal Review Button for unassigned orders */}
                        {o.rawStatus === 'PENDING_LOGISTICS' && (
                          <button
                            onClick={() => handleOpenProposals(o)}
                            className="px-3 py-1.5 text-xxs font-bold text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 rounded-lg shadow-xs cursor-pointer transition-all inline-flex items-center gap-1"
                          >
                            <Send className="w-3 h-3" />
                            Review Proposals
                          </button>
                        )}

                        {/* Track button for assigned shipments */}
                        {shipment && (
                          <button
                            onClick={() => setSelectedShipment(shipment)}
                            className="px-3 py-1.5 text-xxs font-bold text-sky-700 bg-sky-50 hover:bg-sky-100 border border-sky-200 rounded-lg cursor-pointer transition-all inline-flex items-center gap-1"
                          >
                            <Truck className="w-3 h-3" />
                            Live Tracking
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── PROPOSALS REVIEW MODAL ── */}
      {viewingProposalsOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            onClick={() => setViewingProposalsOrder(null)}
            className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs"
          ></div>
          <div className="relative w-full max-w-2xl bg-white border border-slate-200 rounded-3xl p-6 sm:p-7 shadow-2xl animate-in scale-in duration-200 z-10 max-h-[90vh] overflow-y-auto space-y-5">
            
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-widest block">
                  Logistics Carrier Proposals
                </span>
                <h3 className="text-base font-black text-slate-900">
                  Competing Bids for Order #{viewingProposalsOrder.id} · {viewingProposalsOrder.product}
                </h3>
              </div>
              <button
                onClick={() => setViewingProposalsOrder(null)}
                className="p-1.5 hover:bg-slate-100 rounded-full cursor-pointer text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {loadingProposals ? (
              <div className="py-16 text-center">
                <Loader2 className="w-8 h-8 text-sky-500 animate-spin mx-auto mb-2" />
                <span className="text-xs font-bold text-slate-500">Loading submitted carrier proposals...</span>
              </div>
            ) : orderProposals.length === 0 ? (
              <div className="py-16 text-center bg-slate-50 rounded-2xl border border-dashed border-slate-200 p-6">
                <Clock className="w-10 h-10 text-amber-500 mx-auto mb-2 animate-pulse" />
                <h4 className="text-sm font-bold text-slate-800">No Carrier Proposals Yet</h4>
                <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                  Your order is actively listed in the verified logistics open pool. Carriers are reviewing cargo requirements and will submit rate quotes shortly.
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                <p className="text-xs text-slate-500 font-medium">
                  Found <strong className="text-slate-800">{orderProposals.length}</strong> quote(s). Accepting a proposal will automatically generate a tracking waybill and dispatch instructions.
                </p>

                {orderProposals.map(p => (
                  <div
                    key={p.id}
                    className={`p-5 rounded-2xl border transition-all ${
                      p.status === 'ACCEPTED'
                        ? 'border-emerald-300 bg-emerald-50/40 shadow-xs'
                        : p.status === 'REJECTED'
                        ? 'border-slate-200 bg-slate-50/50 opacity-60'
                        : 'border-slate-200 bg-white hover:border-sky-300 hover:shadow-md'
                    }`}
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3 mb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-black text-slate-900">
                            {p.logisticsPartnerCompany || p.carrierCompany || p.logisticsPartnerName || p.carrierName || 'Freight Carrier'}
                          </span>
                          <span className="text-[10px] font-bold text-sky-600 bg-sky-50 px-2 py-0.5 rounded-full">
                            Carrier #{p.logisticsPartnerId || p.carrierId}
                          </span>
                        </div>
                        <span className="text-xs text-slate-500 block mt-0.5">
                          Contact: {p.logisticsPartnerName || p.carrierName} ({p.logisticsPartnerEmail || p.carrierEmail || 'email on file'}) · {p.logisticsPartnerPhone || p.carrierPhone || 'Phone available upon acceptance'}
                        </span>
                      </div>
                      <div className="text-right">
                        <span className="text-lg font-black text-slate-900 font-mono">
                          {p.currency} {Number(p.totalAmount ?? p.estimatedCost ?? p.proposedCost ?? 0).toLocaleString()}
                        </span>
                        <span className="block text-[10px] font-bold text-slate-400">Total All-In Quote</span>
                      </div>
                    </div>

                    {/* Proposal Details */}
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs mb-3">
                      <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                        <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Estimated Transit</span>
                        <span className="font-bold text-slate-800">{p.estimatedTransitDays ? `${p.estimatedTransitDays} Days` : 'TBD'}</span>
                      </div>
                      <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                        <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Pickup Date</span>
                        <span className="font-bold text-slate-800">
                          {p.estimatedPickupDate || p.pickupDate ? new Date(p.estimatedPickupDate || p.pickupDate).toLocaleDateString('en-IN') : 'Scheduled upon booking'}
                        </span>
                      </div>
                      <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                        <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Est. Delivery</span>
                        <span className="font-bold text-slate-800">
                          {p.estimatedDeliveryDate || p.expectedDeliveryDate ? new Date(p.estimatedDeliveryDate || p.expectedDeliveryDate).toLocaleDateString('en-IN') : 'Per transit duration'}
                        </span>
                      </div>
                    </div>

                    {/* Offered Services */}
                    {(p.services || p.offeredServices) && (p.services || p.offeredServices).length > 0 && (
                      <div className="mb-3">
                        <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-1.5">
                          Included Freight & Customs Services:
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                          {(p.services || p.offeredServices).map((svc, sIdx) => (
                            <span key={sIdx} className="px-2 py-0.5 rounded-md bg-sky-50 text-sky-700 text-[10px] font-bold border border-sky-100">
                              ✓ {svc}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Scope Notes */}
                    {p.notes && (
                      <p className="text-xs text-slate-600 bg-slate-50 p-2.5 rounded-xl border border-slate-100 italic mb-4">
                        "{p.notes}"
                      </p>
                    )}

                    {/* Action buttons */}
                    <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                      {p.status === 'ACCEPTED' ? (
                        <span className="inline-flex items-center gap-1.5 text-xs font-black text-emerald-700 bg-emerald-100 px-3 py-1.5 rounded-xl">
                          <CheckCircle2 className="w-4 h-4" /> Proposal Accepted & Contracted
                        </span>
                      ) : p.status === 'REJECTED' ? (
                        <span className="text-xs font-bold text-slate-400">Declined</span>
                      ) : (
                        <>
                          <button
                            type="button"
                            onClick={() => handleRejectProposal(p.id)}
                            disabled={actioningProposalId === p.id}
                            className="px-3.5 py-2 text-xs font-bold text-red-600 border border-red-200 hover:bg-red-50 rounded-xl transition-all cursor-pointer disabled:opacity-50"
                          >
                            Decline
                          </button>
                          <button
                            type="button"
                            onClick={() => handleAcceptProposal(p.id)}
                            disabled={actioningProposalId === p.id}
                            className="px-5 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-500 rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
                          >
                            {actioningProposalId === p.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                            Accept & Create Shipment
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

          </div>
        </div>
      )}

      {/* ── ORDER DETAILS MODAL ── */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedOrder(null)} className="absolute inset-0 bg-slate-900/20 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-3xl p-6 sm:p-7 shadow-2xl animate-in scale-in duration-200 z-10 space-y-5">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[10px] font-black text-sky-600 uppercase tracking-widest block">Order Overview</span>
                <h3 className="text-base font-black text-slate-900">Order #{selectedOrder.id}</h3>
              </div>
              <button
                onClick={() => setSelectedOrder(null)}
                className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs bg-slate-50 p-4 rounded-2xl border border-slate-100">
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Product</span>
                <span className="font-bold text-slate-800">{selectedOrder.product}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">HS Code</span>
                <span className="font-mono text-slate-700 font-bold">{selectedOrder.hscode || '—'}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Order Quantity</span>
                <span className="font-bold text-slate-800">{selectedOrder.qty}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Total Value</span>
                <span className="font-bold text-slate-900 font-mono">{selectedOrder.value}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Pickup Address</span>
                <span className="font-semibold text-slate-700">{selectedOrder.pickupLocation || 'Domestic Hub'}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Destination</span>
                <span className="font-semibold text-slate-700">{selectedOrder.country}</span>
              </div>
            </div>

            {selectedOrder.specialInstructions && (
              <div className="p-3 rounded-xl bg-amber-50 border border-amber-100 text-xs">
                <span className="text-[9px] font-black text-amber-700 uppercase tracking-wider block mb-0.5">Special Instructions</span>
                <p className="text-slate-700 italic">{selectedOrder.specialInstructions}</p>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                onClick={() => setSelectedOrder(null)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Close
              </button>
              {selectedOrder.rawStatus === 'PENDING_LOGISTICS' && (
                <button
                  onClick={() => {
                    const o = selectedOrder;
                    setSelectedOrder(null);
                    handleOpenProposals(o);
                  }}
                  className="px-5 py-2 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow-xs"
                >
                  View Carrier Proposals
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── SHIPMENT LIVE TRACKING TIMELINE MODAL ── */}
      {selectedShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedShipment(null)} className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-2xl bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-2xl animate-in scale-in duration-200 z-10 max-h-[90vh] overflow-y-auto space-y-6">
            
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[10px] font-mono font-bold text-sky-600 uppercase tracking-widest block">
                  Live Cargo Waybill
                </span>
                <h3 className="text-base sm:text-lg font-black text-slate-900 font-mono">
                  {selectedShipment.tracking || `SHP-${selectedShipment.id}`}
                </h3>
              </div>
              <button
                onClick={() => setSelectedShipment(null)}
                className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Carrier & Route Card */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50 border border-slate-200/80 rounded-2xl p-4 text-xs">
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Assigned Carrier</span>
                <span className="font-bold text-slate-800 block">{selectedShipment.logisticsCompany || selectedShipment.logistics || 'Carrier'}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Freight Route</span>
                <span className="font-bold text-slate-800 block">
                  {selectedShipment.origin || 'India'} → {selectedShipment.dest}
                </span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Status</span>
                <span className="font-black text-sky-600 block">{selectedShipment.status}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">ETA</span>
                <span className="font-bold text-slate-800 block">{selectedShipment.eta || 'TBD'}</span>
              </div>
            </div>

            {/* Tracking History Events */}
            <div>
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block mb-4">
                Milestone Tracking History
              </span>

              {(!selectedShipment.trackingHistory || selectedShipment.trackingHistory.length === 0) ? (
                <div className="p-4 rounded-xl bg-slate-50 border border-slate-100 text-center text-xs text-slate-400">
                  Shipment registered. Carrier is preparing cargo pickup and export documentation.
                </div>
              ) : (
                <div className="relative pl-6 space-y-4 border-l-2 border-slate-100 ml-3">
                  {selectedShipment.trackingHistory.map((evt, idx) => (
                    <div key={evt.id || idx} className="relative">
                      <div className="absolute -left-[31px] top-1 w-3 h-3 rounded-full bg-emerald-500 ring-4 ring-white"></div>
                      <div className="bg-slate-50/80 p-3.5 rounded-2xl border border-slate-100 text-xs space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-extrabold text-slate-900">
                            {evt.status ? evt.status.replace(/_/g, ' ') : 'STATUS UPDATE'}
                          </span>
                          <span className="text-[10px] font-semibold text-slate-400">
                            {evt.timestamp ? new Date(evt.timestamp).toLocaleString('en-IN') : 'Recent'}
                          </span>
                        </div>
                        {evt.location && (
                          <div className="flex items-center gap-1 text-[11px] text-slate-600">
                            <MapPin className="w-3.5 h-3.5 text-sky-500 shrink-0" />
                            <span>{evt.location}</span>
                          </div>
                        )}
                        {evt.description && (
                          <p className="text-[11px] text-slate-500 mt-1 italic">"{evt.description}"</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
