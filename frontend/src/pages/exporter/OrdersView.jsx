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
    <div className="space-y-6 animate-in fade-in duration-200">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground tracking-tight">Export Orders & Freight Contracts</h1>
          <p className="text-xs text-muted-foreground mt-1">
            Review export shipments, evaluate competing logistics proposals, and track multimodal milestones.
          </p>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 border-b border-border text-xs font-medium pb-1">
        {['All', 'Pending', 'Accepted', 'Shipped', 'Delivered'].map((status) => (
          <button
            key={status}
            onClick={() => setOrderFilter(status)}
            className={`pb-2 px-2 cursor-pointer transition-colors relative ${
              orderFilter === status ? 'text-primary font-semibold' : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            {status}
            {orderFilter === status && (
              <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t-full"></div>
            )}
          </button>
        ))}
      </div>

      {/* Orders Table */}
      <div className="card-claude overflow-hidden">
        <div className="overflow-x-auto">
          <table className="table-claude">
            <thead>
              <tr>
                <th className="py-3 px-4">Order ID</th>
                <th className="py-3 px-4">Commodity / HS Code</th>
                <th className="py-3 px-4">Destination</th>
                <th className="py-3 px-4">Quantity</th>
                <th className="py-3 px-4">Order Value</th>
                <th className="py-3 px-4">Logistics Carrier</th>
                <th className="py-3 px-4">Order Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border text-xs">
              {orders
                .filter(o => orderFilter === 'All' || o.status === orderFilter)
                .map(o => {
                  const shipment = shipments.find(s => s.orderId === o.id);
                  const partner = shipment
                    ? (shipment.logisticsCompany || shipment.logistics || 'Assigned Partner')
                    : (o.logisticsPartner !== 'TBD' ? o.logisticsPartner : 'Awaiting Quote');

                  return (
                    <tr key={o.id} className="hover:bg-muted/40 transition-colors">
                      <td className="py-3.5 px-4">
                        <span className="font-mono text-foreground font-semibold bg-muted px-2 py-0.5 rounded text-[11px]">
                          #{o.id}
                        </span>
                        <span className="block text-[10px] text-muted-foreground mt-0.5">{o.date || 'Recent'}</span>
                      </td>

                      <td className="py-3.5 px-4">
                        <span className="text-foreground font-semibold block">{o.product}</span>
                        <span className="font-mono text-[10px] text-muted-foreground">HS: {o.hscode || '—'}</span>
                      </td>

                      <td className="py-3.5 px-4">
                        <span className="font-semibold text-foreground block">{o.country}</span>
                        <span className="text-[10px] text-muted-foreground block">{o.shippingRequirements || 'Sea Freight'}</span>
                      </td>

                      <td className="py-3.5 px-4 text-muted-foreground">{o.qty}</td>

                      <td className="py-3.5 px-4 text-foreground font-semibold font-mono">
                        {o.value.startsWith('₹') ? o.value : `₹${o.value}`}
                      </td>

                      <td className="py-3.5 px-4">
                        <span className="text-foreground font-medium block">{partner}</span>
                        {shipment?.tracking && (
                          <span className="text-[10px] font-mono text-primary font-semibold block">
                            {shipment.tracking}
                          </span>
                        )}
                      </td>

                      <td className="py-3.5 px-4">
                        {o.status === 'Pending' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-700 dark:text-amber-300 bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-full">
                            <Clock className="w-3 h-3 text-amber-500" /> Awaiting Logistics
                          </span>
                        )}
                        {o.status === 'Accepted' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                            <Check className="w-3 h-3 text-emerald-500" /> Carrier Confirmed
                          </span>
                        )}
                        {o.status === 'Shipped' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-primary bg-primary/10 border border-primary/20 px-2 py-0.5 rounded-full">
                            <Truck className="w-3 h-3 text-primary" /> In Transit
                          </span>
                        )}
                        {o.status === 'Delivered' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-purple-700 dark:text-purple-300 bg-purple-500/10 border border-purple-500/20 px-2 py-0.5 rounded-full">
                            <CheckCircle2 className="w-3 h-3 text-purple-500" /> Delivered
                          </span>
                        )}
                        {o.status === 'Rejected' && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-destructive bg-destructive/10 border border-destructive/20 px-2 py-0.5 rounded-full">
                            <AlertCircle className="w-3 h-3 text-destructive" /> Declined
                          </span>
                        )}
                      </td>

                      <td className="py-3.5 px-4 text-right space-x-1.5 whitespace-nowrap">
                        <button
                          onClick={() => setSelectedOrder(o)}
                          className="btn-secondary py-1 px-2.5 text-xs"
                        >
                          <Eye className="w-3 h-3 inline mr-1" /> Specs
                        </button>

                        {/* Proposal Review Button for unassigned orders */}
                        {o.rawStatus === 'PENDING_LOGISTICS' && (
                          <button
                            onClick={() => handleOpenProposals(o)}
                            className="btn-primary py-1 px-3 text-xs"
                          >
                            <Send className="w-3 h-3 mr-1" />
                            Review Proposals
                          </button>
                        )}

                        {/* Track button for assigned shipments */}
                        {shipment && (
                          <button
                            onClick={() => setSelectedShipment(shipment)}
                            className="btn-outline py-1 px-3 text-xs text-primary border-primary/30 hover:bg-primary/10"
                          >
                            <Truck className="w-3 h-3 mr-1" />
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
            className="absolute inset-0 bg-background/80 backdrop-blur-sm"
          ></div>
          <div className="relative w-full max-w-2xl bg-popover text-popover-foreground border border-border rounded-2xl p-6 shadow-xl animate-in scale-in duration-150 z-10 max-h-[90vh] overflow-y-auto space-y-4">
            
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <span className="text-[10px] font-semibold text-primary uppercase tracking-wider block">
                  Logistics Carrier Proposals
                </span>
                <h3 className="text-base font-semibold text-foreground">
                  Competing Bids for Order #{viewingProposalsOrder.id} · {viewingProposalsOrder.product}
                </h3>
              </div>
              <button
                onClick={() => setViewingProposalsOrder(null)}
                className="p-1.5 hover:bg-muted rounded-md cursor-pointer text-muted-foreground transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {loadingProposals ? (
              <div className="py-12 text-center">
                <Loader2 className="w-7 h-7 text-primary animate-spin mx-auto mb-2" />
                <span className="text-xs text-muted-foreground">Loading submitted carrier proposals...</span>
              </div>
            ) : orderProposals.length === 0 ? (
              <div className="py-12 text-center bg-muted/30 rounded-xl border border-dashed border-border p-6">
                <Clock className="w-8 h-8 text-amber-500 mx-auto mb-2" />
                <h4 className="text-sm font-semibold text-foreground">No Carrier Proposals Yet</h4>
                <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
                  Your order is actively listed in the verified logistics pool. Carriers are reviewing cargo requirements and will submit rate quotes shortly.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                <p className="text-xs text-muted-foreground">
                  Found <strong className="text-foreground">{orderProposals.length}</strong> quote(s). Accepting a proposal will automatically generate a tracking waybill and dispatch instructions.
                </p>

                {orderProposals.map(p => (
                  <div
                    key={p.id}
                    className={`p-4 rounded-xl border transition-all ${
                      p.status === 'ACCEPTED'
                        ? 'border-emerald-500/40 bg-emerald-500/5'
                        : p.status === 'REJECTED'
                        ? 'border-border bg-muted/40 opacity-60'
                        : 'border-border bg-card hover:border-primary/40'
                    }`}
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-border pb-3 mb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-semibold text-foreground">
                            {p.logisticsPartnerCompany || p.carrierCompany || p.logisticsPartnerName || p.carrierName || 'Freight Carrier'}
                          </span>
                          <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded-full">
                            Carrier #{p.logisticsPartnerId || p.carrierId}
                          </span>
                        </div>
                        <span className="text-xs text-muted-foreground block mt-0.5">
                          Contact: {p.logisticsPartnerName || p.carrierName} ({p.logisticsPartnerEmail || p.carrierEmail || 'email on file'}) · {p.logisticsPartnerPhone || p.carrierPhone || 'Phone available upon acceptance'}
                        </span>
                      </div>
                      <div className="text-right">
                        <span className="text-base font-bold text-foreground font-mono">
                          {p.currency} {Number(p.totalAmount ?? p.estimatedCost ?? p.proposedCost ?? 0).toLocaleString()}
                        </span>
                        <span className="block text-[10px] text-muted-foreground">Total All-In Quote</span>
                      </div>
                    </div>

                    {/* Proposal Details */}
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 text-xs mb-3">
                      <div className="p-2 rounded-lg bg-muted/40 border border-border/50">
                        <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Estimated Transit</span>
                        <span className="font-semibold text-foreground">{p.estimatedTransitDays ? `${p.estimatedTransitDays} Days` : 'TBD'}</span>
                      </div>
                      <div className="p-2 rounded-lg bg-muted/40 border border-border/50">
                        <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Pickup Date</span>
                        <span className="font-semibold text-foreground">
                          {p.estimatedPickupDate || p.pickupDate ? new Date(p.estimatedPickupDate || p.pickupDate).toLocaleDateString('en-IN') : 'Scheduled upon booking'}
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-muted/40 border border-border/50">
                        <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Est. Delivery</span>
                        <span className="font-semibold text-foreground">
                          {p.estimatedDeliveryDate || p.expectedDeliveryDate ? new Date(p.estimatedDeliveryDate || p.expectedDeliveryDate).toLocaleDateString('en-IN') : 'Per transit duration'}
                        </span>
                      </div>
                    </div>

                    {/* Offered Services */}
                    {(p.services || p.offeredServices) && (p.services || p.offeredServices).length > 0 && (
                      <div className="mb-3">
                        <span className="text-[9px] font-semibold text-muted-foreground uppercase tracking-wider block mb-1">
                          Included Services:
                        </span>
                        <div className="flex flex-wrap gap-1">
                          {(p.services || p.offeredServices).map((svc, sIdx) => (
                            <span key={sIdx} className="px-2 py-0.5 rounded bg-primary/10 text-primary text-[10px] font-medium border border-primary/20">
                              ✓ {svc}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Scope Notes */}
                    {p.notes && (
                      <p className="text-xs text-foreground bg-muted/30 p-2 rounded-lg border border-border/50 italic mb-3">
                        "{p.notes}"
                      </p>
                    )}

                    {/* Action buttons */}
                    <div className="flex justify-end gap-2 pt-2 border-t border-border">
                      {p.status === 'ACCEPTED' ? (
                        <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-500/10 px-3 py-1.5 rounded-lg border border-emerald-500/20">
                          <CheckCircle2 className="w-3.5 h-3.5" /> Proposal Accepted & Contracted
                        </span>
                      ) : p.status === 'REJECTED' ? (
                        <span className="text-xs text-muted-foreground">Declined</span>
                      ) : (
                        <>
                          <button
                            type="button"
                            onClick={() => handleRejectProposal(p.id)}
                            disabled={actioningProposalId === p.id}
                            className="btn-outline text-destructive hover:bg-destructive/10 border-destructive/30 py-1.5 px-3 text-xs"
                          >
                            Decline
                          </button>
                          <button
                            type="button"
                            onClick={() => handleAcceptProposal(p.id)}
                            disabled={actioningProposalId === p.id}
                            className="btn-primary py-1.5 px-4 text-xs"
                          >
                            {actioningProposalId === p.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
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
          <div onClick={() => setSelectedOrder(null)} className="absolute inset-0 bg-background/80 backdrop-blur-sm"></div>
          <div className="relative w-full max-w-lg bg-popover text-popover-foreground border border-border rounded-2xl p-6 shadow-xl animate-in scale-in duration-150 z-10 space-y-4">
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <span className="text-[10px] font-semibold text-primary uppercase tracking-wider block">Order Overview</span>
                <h3 className="text-base font-semibold text-foreground">Order #{selectedOrder.id}</h3>
              </div>
              <button
                onClick={() => setSelectedOrder(null)}
                className="p-1.5 hover:bg-muted rounded-md cursor-pointer text-muted-foreground transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs bg-muted/40 p-3.5 rounded-xl border border-border">
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Product</span>
                <span className="font-semibold text-foreground">{selectedOrder.product}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">HS Code</span>
                <span className="font-mono text-foreground font-medium">{selectedOrder.hscode || '—'}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Order Quantity</span>
                <span className="font-semibold text-foreground">{selectedOrder.qty}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Total Value</span>
                <span className="font-semibold text-foreground font-mono">{selectedOrder.value}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Pickup Address</span>
                <span className="text-muted-foreground">{selectedOrder.pickupLocation || 'Domestic Hub'}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Destination</span>
                <span className="text-muted-foreground">{selectedOrder.country}</span>
              </div>
            </div>

            {selectedOrder.specialInstructions && (
              <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-xs text-foreground">
                <span className="text-[9px] font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wider block mb-0.5">Special Instructions</span>
                <p className="italic">{selectedOrder.specialInstructions}</p>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-border">
              <button
                onClick={() => setSelectedOrder(null)}
                className="btn-secondary py-1.5 px-3 text-xs"
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
                  className="btn-primary py-1.5 px-3.5 text-xs"
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
          <div onClick={() => setSelectedShipment(null)} className="absolute inset-0 bg-background/80 backdrop-blur-sm"></div>
          <div className="relative w-full max-w-2xl bg-popover text-popover-foreground border border-border rounded-2xl p-6 shadow-xl animate-in scale-in duration-150 z-10 max-h-[90vh] overflow-y-auto space-y-4">
            
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <span className="text-[10px] font-mono font-semibold text-primary uppercase tracking-wider block">
                  Live Cargo Waybill
                </span>
                <h3 className="text-base font-semibold text-foreground font-mono">
                  {selectedShipment.tracking || `SHP-${selectedShipment.id}`}
                </h3>
              </div>
              <button
                onClick={() => setSelectedShipment(null)}
                className="p-1.5 hover:bg-muted rounded-md cursor-pointer text-muted-foreground transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Carrier & Route Card */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 bg-muted/40 border border-border rounded-xl p-3 text-xs">
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Carrier</span>
                <span className="font-semibold text-foreground block">{selectedShipment.logisticsCompany || selectedShipment.logistics || 'Carrier'}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Route</span>
                <span className="font-semibold text-foreground block">
                  {selectedShipment.origin || 'India'} → {selectedShipment.dest}
                </span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">Status</span>
                <span className="font-semibold text-primary block">{selectedShipment.status}</span>
              </div>
              <div>
                <span className="text-[9px] font-medium text-muted-foreground uppercase tracking-wider block">ETA</span>
                <span className="font-semibold text-foreground block">{selectedShipment.eta || 'TBD'}</span>
              </div>
            </div>

            {/* Tracking History Events */}
            <div>
              <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block mb-3">
                Milestone Tracking History
              </span>

              {(!selectedShipment.trackingHistory || selectedShipment.trackingHistory.length === 0) ? (
                <div className="p-4 rounded-xl bg-muted/30 border border-border text-center text-xs text-muted-foreground">
                  Shipment registered. Carrier is preparing cargo pickup and export documentation.
                </div>
              ) : (
                <div className="relative pl-5 space-y-3 border-l border-border ml-2">
                  {selectedShipment.trackingHistory.map((evt, idx) => (
                    <div key={evt.id || idx} className="relative">
                      <div className="absolute -left-[25px] top-1.5 w-2.5 h-2.5 rounded-full bg-primary ring-4 ring-popover"></div>
                      <div className="bg-card p-3 rounded-xl border border-border text-xs space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-foreground">
                            {evt.status ? evt.status.replace(/_/g, ' ') : 'STATUS UPDATE'}
                          </span>
                          <span className="text-[10px] text-muted-foreground">
                            {evt.timestamp ? new Date(evt.timestamp).toLocaleString('en-IN') : 'Recent'}
                          </span>
                        </div>
                        {evt.location && (
                          <div className="flex items-center gap-1 text-[11px] text-muted-foreground">
                            <MapPin className="w-3 h-3 text-primary shrink-0" />
                            <span>{evt.location}</span>
                          </div>
                        )}
                        {evt.description && (
                          <p className="text-[11px] text-muted-foreground mt-1 italic">"{evt.description}"</p>
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

