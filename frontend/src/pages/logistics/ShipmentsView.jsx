import React, { useState, useMemo } from 'react';
import {
  X, Loader2, Truck, Search, MapPin, Calendar, CheckCircle2,
  Clock, AlertCircle, FileText, ArrowRight, ShieldCheck, DollarSign
} from 'lucide-react';
import { countryFlag, SHIPMENT_STEPS, statusToBadgeClass } from './utils';
import { shipmentsApi } from '../../services';

export default function ShipmentsView({ shipments = [], onStatusUpdated, addToast }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState('All'); // All, Active, Delivered
  const [selectedShipment, setSelectedShipment] = useState(null);
  const [trackingShipment, setTrackingShipment] = useState(null);

  // Status Update Modal state
  const [newStatus, setNewStatus] = useState('');
  const [milestoneLocation, setMilestoneLocation] = useState('');
  const [milestoneDescription, setMilestoneDescription] = useState('');
  const [updating, setUpdating] = useState(false);

  // Filtered shipments
  const filtered = useMemo(() => {
    return shipments.filter(s => {
      const q = searchQuery.toLowerCase().trim();
      const matchSearch = !q ||
        (s.id && s.id.toString().includes(q)) ||
        (s.orderId && s.orderId.toString().includes(q)) ||
        (s.tracking && s.tracking.toLowerCase().includes(q)) ||
        (s.product && s.product.toLowerCase().includes(q)) ||
        (s.dest && s.dest.toLowerCase().includes(q)) ||
        (s.exporter && s.exporter.toLowerCase().includes(q));

      if (!matchSearch) return false;

      if (filterStatus === 'Active') return s.rawStatus !== 'DELIVERED';
      if (filterStatus === 'Delivered') return s.rawStatus === 'DELIVERED';
      return true;
    });
  }, [shipments, searchQuery, filterStatus]);

  const handleOpenManageModal = (s) => {
    setSelectedShipment(s);
    setNewStatus(s.rawStatus);
    setMilestoneLocation(s.origin || '');
    setMilestoneDescription('');
  };

  const handleUpdateStatus = async (e) => {
    e.preventDefault();
    if (!newStatus || !selectedShipment) return;

    setUpdating(true);
    try {
      await shipmentsApi.updateStatus(selectedShipment.id, {
        shipmentStatus: newStatus,
        location: milestoneLocation || selectedShipment.dest || 'Transit Checkpoint',
        description: milestoneDescription || `Shipment transitioned to ${newStatus.replace(/_/g, ' ')}`,
      });

      const stepObj = SHIPMENT_STEPS.find(st => st.key === newStatus);
      addToast(`Shipment #${selectedShipment.id} milestone updated to "${stepObj ? stepObj.label : newStatus}"`, 'success');
      
      if (onStatusUpdated) await onStatusUpdated();
      setSelectedShipment(null);
      setNewStatus('');
      setMilestoneLocation('');
      setMilestoneDescription('');
    } catch (err) {
      addToast(err.message || 'Status update failed', 'error');
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground tracking-tight">Active Shipments & Fleet Tracking</h1>
          <p className="text-xs text-muted-foreground mt-1">
            Log dispatch milestones, update GPS checkpoints, and provide verified cargo visibility to exporters.
          </p>
        </div>

        {/* Tab Filter */}
        <div className="flex gap-1.5 bg-card border border-border p-1 rounded-xl shadow-xs self-start sm:self-auto">
          {['All', 'Active', 'Delivered'].map(f => (
            <button
              key={f}
              onClick={() => setFilterStatus(f)}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg cursor-pointer transition-all ${
                filterStatus === f
                  ? 'bg-primary text-primary-foreground shadow-xs'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              {f} ({
                f === 'All' ? shipments.length :
                f === 'Active' ? shipments.filter(s => s.rawStatus !== 'DELIVERED').length :
                shipments.filter(s => s.rawStatus === 'DELIVERED').length
              })
            </button>
          ))}
        </div>
      </div>

      {/* Search Toolbar */}
      <div className="card-claude p-3.5">
        <div className="relative max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by Tracking ID, Order #, Product, Exporter..."
            className="input-claude pl-10"
          />
        </div>
      </div>

      {/* Shipments Table */}
      <div className="card-claude overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="table-claude w-full text-left">
            <thead>
              <tr className="border-b border-border bg-muted/40 text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
                <th className="py-3.5 px-5">Shipment #</th>
                <th className="py-3.5 px-5">Order #</th>
                <th className="py-3.5 px-5">Exporter</th>
                <th className="py-3.5 px-5">Product Cargo</th>
                <th className="py-3.5 px-5">Destination</th>
                <th className="py-3.5 px-5">Tracking Number</th>
                <th className="py-3.5 px-5">Current Milestone</th>
                <th className="py-3.5 px-5">ETA</th>
                <th className="py-3.5 px-5 text-right">Operations</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border text-xs font-medium text-foreground">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan="9" className="py-16 text-center">
                    <Truck className="w-10 h-10 text-muted-foreground/40 mx-auto mb-3" />
                    <span className="text-sm font-semibold text-foreground block">No shipments found</span>
                    <span className="text-xs text-muted-foreground mt-1 block">
                      Accepted export proposals automatically initialize tracking here.
                    </span>
                  </td>
                </tr>
              ) : (
                filtered.map(s => (
                  <tr key={s.id} className="hover:bg-muted/30 transition-colors">
                    <td className="py-3.5 px-5">
                      <span className="font-mono font-bold text-foreground bg-muted px-2 py-0.5 rounded text-[11px]">
                        #{s.id}
                      </span>
                    </td>
                    <td className="py-3.5 px-5 font-mono text-muted-foreground text-[11px]">#{s.orderId}</td>
                    <td className="py-3.5 px-5">
                      <span className="block font-semibold text-foreground">{s.exporter || 'Exporter'}</span>
                      <span className="block text-[10px] text-muted-foreground">{s.exporterCompany || ''}</span>
                    </td>
                    <td className="py-3.5 px-5">
                      <span className="text-foreground font-semibold block">{s.product}</span>
                      <span className="text-[10px] text-muted-foreground block">{s.qty}</span>
                    </td>
                    <td className="py-3.5 px-5">
                      <div className="flex items-center gap-1.5">
                        <span className="text-lg">{countryFlag(s.dest)}</span>
                        <span className="font-semibold text-foreground">{s.dest}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-5">
                      <span className="font-mono text-[11px] font-bold text-primary bg-primary/10 px-2.5 py-1 rounded-md border border-primary/20 block max-w-fit">
                        {s.tracking || 'TRK-PENDING'}
                      </span>
                    </td>
                    <td className="py-3.5 px-5">
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase ${statusToBadgeClass(s.status)}`}>
                        {s.status}
                      </span>
                    </td>
                    <td className="py-3.5 px-5 text-muted-foreground text-[11px]">
                      {s.eta ? new Date(s.eta).toLocaleDateString('en-IN') : 'TBD'}
                    </td>
                    <td className="py-3.5 px-5 text-right space-x-1.5 whitespace-nowrap">
                      <button
                        onClick={() => handleOpenManageModal(s)}
                        className="btn-outline text-xs px-2.5 py-1"
                      >
                        Update Milestone
                      </button>
                      <button
                        onClick={() => setTrackingShipment(s)}
                        className="btn-primary text-xs px-3 py-1"
                      >
                        Live Tracking
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── MANAGE MILESTONE MODAL ── */}
      {selectedShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => !updating && setSelectedShipment(null)} className="absolute inset-0 bg-background/80 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-lg card-claude p-6 sm:p-7 shadow-2xl animate-in scale-in duration-200 z-10 space-y-5 bg-card border-border">
            
            <div className="flex justify-between items-center border-b border-border pb-3">
              <div>
                <span className="text-[10px] font-bold text-primary uppercase tracking-widest block">Cargo Dispatch Log</span>
                <h3 className="text-base font-bold text-foreground">
                  Update Shipment #{selectedShipment.id}
                </h3>
              </div>
              <button
                onClick={() => setSelectedShipment(null)}
                className="p-1.5 hover:bg-muted rounded-full cursor-pointer text-muted-foreground hover:text-foreground"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Quick Specs */}
            <div className="grid grid-cols-2 gap-3 text-xs bg-muted/40 p-3.5 rounded-2xl border border-border">
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Product & Qty</span>
                <span className="font-semibold text-foreground">{selectedShipment.product} ({selectedShipment.qty})</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Tracking Number</span>
                <span className="font-mono text-primary font-bold">{selectedShipment.tracking}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Origin Hub</span>
                <span className="text-foreground font-semibold">{selectedShipment.origin || 'Mumbai'}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Destination Port</span>
                <span className="text-foreground font-semibold">{selectedShipment.dest}</span>
              </div>
            </div>

            {/* Update Form */}
            <form onSubmit={handleUpdateStatus} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
                  Milestone Progress Stage *
                </label>
                <select
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value)}
                  className="input-claude cursor-pointer"
                >
                  {SHIPMENT_STEPS.map(s => (
                    <option key={s.key} value={s.key}>{s.label}</option>
                  ))}
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
                  Checkpoint Location *
                </label>
                <div className="relative">
                  <MapPin className="w-3.5 h-3.5 text-muted-foreground absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    required
                    value={milestoneLocation}
                    onChange={(e) => setMilestoneLocation(e.target.value)}
                    placeholder="e.g. JNPT Terminal, Mumbai, India"
                    className="input-claude pl-9"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">
                  Activity Description / Remarks
                </label>
                <textarea
                  rows="2"
                  value={milestoneDescription}
                  onChange={(e) => setMilestoneDescription(e.target.value)}
                  placeholder="e.g. Container cleared customs, loaded onto feeder vessel for transshipment."
                  className="input-claude resize-none"
                ></textarea>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-border">
                <button
                  type="button"
                  onClick={() => setSelectedShipment(null)}
                  disabled={updating}
                  className="btn-ghost text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updating}
                  className="btn-primary text-xs flex items-center gap-1.5"
                >
                  {updating ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                  Commit Milestone Update
                </button>
              </div>
            </form>

          </div>
        </div>
      )}

      {/* ── LIVE TRACKING & HISTORY MODAL ── */}
      {trackingShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setTrackingShipment(null)} className="absolute inset-0 bg-background/80 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-2xl card-claude p-6 sm:p-8 shadow-2xl animate-in scale-in duration-200 z-10 max-h-[90vh] overflow-y-auto space-y-6 bg-card border-border">
            
            {/* Header */}
            <div className="flex justify-between items-center border-b border-border pb-4">
              <div>
                <span className="text-[10px] font-mono font-bold text-primary uppercase tracking-widest block">
                  Cargo Waybill & Milestone Log
                </span>
                <h3 className="text-base sm:text-lg font-bold text-foreground font-mono">
                  {trackingShipment.tracking || `SHP-${trackingShipment.id}`}
                </h3>
              </div>
              <button
                onClick={() => setTrackingShipment(null)}
                className="p-1.5 hover:bg-muted rounded-full cursor-pointer text-muted-foreground hover:text-foreground"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Overview Card */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-muted/40 border border-border rounded-2xl p-4 text-xs">
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Product</span>
                <span className="font-semibold text-foreground">{trackingShipment.product}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Quantity</span>
                <span className="font-semibold text-foreground">{trackingShipment.qty}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Origin → Dest</span>
                <span className="font-semibold text-foreground truncate block">
                  {countryFlag(trackingShipment.dest)} {trackingShipment.dest}
                </span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-muted-foreground uppercase tracking-wider block">Current Status</span>
                <span className="font-bold text-primary block">{trackingShipment.status}</span>
              </div>
            </div>

            {/* Horizontal Milestone Progress Bar */}
            <div>
              <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest block mb-4">
                Milestone Pipeline
              </span>
              <div className="relative px-2">
                <div className="absolute top-3.5 left-6 right-6 h-1 bg-border rounded-full z-0"></div>
                {(() => {
                  const currentIdx = SHIPMENT_STEPS.findIndex(s => s.key === trackingShipment.rawStatus);
                  const pct = currentIdx < 0 ? 0 : (currentIdx / (SHIPMENT_STEPS.length - 1)) * 100;
                  return (
                    <div
                      className="absolute top-3.5 left-6 h-1 bg-primary rounded-full z-0 transition-all duration-500"
                      style={{ width: `calc(${pct}% - 36px)` }}
                    ></div>
                  );
                })()}

                <div className="flex justify-between relative z-10">
                  {SHIPMENT_STEPS.map((step, idx) => {
                    const currentIdx = SHIPMENT_STEPS.findIndex(s => s.key === trackingShipment.rawStatus);
                    const isDone = idx <= currentIdx;
                    const isCurrent = idx === currentIdx;
                    return (
                      <div key={step.key} className="flex flex-col items-center text-center space-y-1.5 max-w-[65px]">
                        <div
                          className={`w-7 h-7 rounded-full border-2 flex items-center justify-center transition-all ${
                            isCurrent
                              ? 'bg-primary border-primary text-primary-foreground ring-4 ring-primary/20'
                              : isDone
                              ? 'bg-primary border-primary text-primary-foreground'
                              : 'bg-card border-border text-muted-foreground'
                          }`}
                        >
                          {isDone && !isCurrent ? (
                            <CheckCircle2 className="w-3.5 h-3.5" />
                          ) : (
                            <span className="text-[9px] font-bold">{idx + 1}</span>
                          )}
                        </div>
                        <span
                          className={`text-[9px] font-medium leading-tight ${
                            isCurrent ? 'text-primary font-bold' : isDone ? 'text-foreground' : 'text-muted-foreground'
                          }`}
                        >
                          {step.short}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Detailed Tracking Event Log */}
            <div>
              <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest block mb-3">
                Tracking History Log ({trackingShipment.trackingHistory ? trackingShipment.trackingHistory.length : 0} events)
              </span>

              {(!trackingShipment.trackingHistory || trackingShipment.trackingHistory.length === 0) ? (
                <div className="p-4 rounded-xl bg-muted/30 border border-border text-center text-xs text-muted-foreground">
                  Initial booking completed. No milestone updates posted yet.
                </div>
              ) : (
                <div className="relative pl-6 space-y-4 border-l-2 border-border ml-3">
                  {trackingShipment.trackingHistory.map((evt, i) => (
                    <div key={evt.id || i} className="relative">
                      <div className="absolute -left-[31px] top-1 w-3 h-3 rounded-full bg-primary ring-4 ring-card"></div>
                      <div className="bg-muted/30 p-3 rounded-xl border border-border text-xs space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-foreground">
                            {evt.status ? evt.status.replace(/_/g, ' ') : 'UPDATE'}
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

