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
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Active Shipments & Fleet Tracking</h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">
            Log dispatch milestones, update GPS checkpoints, and provide verified cargo visibility to exporters.
          </p>
        </div>

        {/* Tab Filter */}
        <div className="flex gap-1.5 bg-white border border-slate-200/80 p-1 rounded-xl shadow-xs self-start sm:self-auto">
          {['All', 'Active', 'Delivered'].map(f => (
            <button
              key={f}
              onClick={() => setFilterStatus(f)}
              className={`px-3 py-1.5 text-xxs font-bold rounded-lg cursor-pointer transition-all ${
                filterStatus === f
                  ? 'bg-sky-500 text-white shadow-xs'
                  : 'text-slate-500 hover:text-slate-800'
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
      <div className="bg-white border border-slate-200/80 rounded-2xl p-3.5 shadow-sm">
        <div className="relative max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by Tracking ID, Order #, Product, Exporter..."
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:bg-white transition-all"
          />
        </div>
      </div>

      {/* Shipments Table */}
      <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/70 text-[10px] font-black text-slate-400 uppercase tracking-widest">
                <th className="py-4 px-5">Shipment #</th>
                <th className="py-4 px-5">Order #</th>
                <th className="py-4 px-5">Exporter</th>
                <th className="py-4 px-5">Product Cargo</th>
                <th className="py-4 px-5">Destination</th>
                <th className="py-4 px-5">Tracking Number</th>
                <th className="py-4 px-5">Current Milestone</th>
                <th className="py-4 px-5">ETA</th>
                <th className="py-4 px-5 text-right">Operations</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan="9" className="py-16 text-center">
                    <Truck className="w-10 h-10 text-slate-200 mx-auto mb-3" />
                    <span className="text-sm font-bold text-slate-500 block">No shipments found</span>
                    <span className="text-[10px] text-slate-400 mt-1 block">
                      Accepted export proposals automatically initialize tracking here.
                    </span>
                  </td>
                </tr>
              ) : (
                filtered.map(s => (
                  <tr key={s.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-4 px-5">
                      <span className="font-mono font-bold text-slate-900 bg-slate-100 px-2 py-0.5 rounded text-[11px]">
                        #{s.id}
                      </span>
                    </td>
                    <td className="py-4 px-5 font-mono text-slate-400 text-[11px]">#{s.orderId}</td>
                    <td className="py-4 px-5">
                      <span className="block font-bold text-slate-800">{s.exporter || 'Exporter'}</span>
                      <span className="block text-[10px] text-slate-400">{s.exporterCompany || ''}</span>
                    </td>
                    <td className="py-4 px-5">
                      <span className="text-slate-800 font-bold block">{s.product}</span>
                      <span className="text-[10px] text-slate-400 block">{s.qty}</span>
                    </td>
                    <td className="py-4 px-5">
                      <div className="flex items-center gap-1.5">
                        <span className="text-lg">{countryFlag(s.dest)}</span>
                        <span className="font-bold text-slate-800">{s.dest}</span>
                      </div>
                    </td>
                    <td className="py-4 px-5">
                      <span className="font-mono text-[11px] font-bold text-sky-700 bg-sky-50 px-2.5 py-1 rounded-md border border-sky-100 block max-w-fit">
                        {s.tracking || 'TRK-PENDING'}
                      </span>
                    </td>
                    <td className="py-4 px-5">
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[9px] font-black uppercase ${statusToBadgeClass(s.status)}`}>
                        {s.status}
                      </span>
                    </td>
                    <td className="py-4 px-5 text-slate-500 text-[11px]">
                      {s.eta ? new Date(s.eta).toLocaleDateString('en-IN') : 'TBD'}
                    </td>
                    <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                      <button
                        onClick={() => handleOpenManageModal(s)}
                        className="px-2.5 py-1.5 text-xxs font-bold text-indigo-600 border border-slate-200 hover:bg-indigo-50 rounded-lg cursor-pointer transition-all"
                      >
                        Update Milestone
                      </button>
                      <button
                        onClick={() => setTrackingShipment(s)}
                        className="px-3 py-1.5 text-xxs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-lg cursor-pointer transition-all shadow-xs"
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
          <div onClick={() => !updating && setSelectedShipment(null)} className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-lg bg-white border border-slate-200 rounded-3xl p-6 sm:p-7 shadow-2xl animate-in scale-in duration-200 z-10 space-y-5">
            
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-widest block">Cargo Dispatch Log</span>
                <h3 className="text-base font-black text-slate-900">
                  Update Shipment #{selectedShipment.id}
                </h3>
              </div>
              <button
                onClick={() => setSelectedShipment(null)}
                className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Quick Specs */}
            <div className="grid grid-cols-2 gap-3 text-xs bg-slate-50 p-3.5 rounded-2xl border border-slate-100">
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Product & Qty</span>
                <span className="font-bold text-slate-800">{selectedShipment.product} ({selectedShipment.qty})</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Tracking Number</span>
                <span className="font-mono text-sky-700 font-bold">{selectedShipment.tracking}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Origin Hub</span>
                <span className="text-slate-700 font-semibold">{selectedShipment.origin || 'Mumbai'}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Destination Port</span>
                <span className="text-slate-700 font-semibold">{selectedShipment.dest}</span>
              </div>
            </div>

            {/* Update Form */}
            <form onSubmit={handleUpdateStatus} className="space-y-4">
              <div className="space-y-1">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  Milestone Progress Stage *
                </label>
                <select
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer"
                >
                  {SHIPMENT_STEPS.map(s => (
                    <option key={s.key} value={s.key}>{s.label}</option>
                  ))}
                </select>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  Checkpoint Location *
                </label>
                <div className="relative">
                  <MapPin className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    required
                    value={milestoneLocation}
                    onChange={(e) => setMilestoneLocation(e.target.value)}
                    placeholder="e.g. JNPT Terminal, Mumbai, India"
                    className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-xl focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">
                  Activity Description / Remarks
                </label>
                <textarea
                  rows="2"
                  value={milestoneDescription}
                  onChange={(e) => setMilestoneDescription(e.target.value)}
                  placeholder="e.g. Container cleared customs, loaded onto feeder vessel for transshipment."
                  className="w-full px-3.5 py-2 text-xs border border-slate-200 rounded-xl focus:outline-none focus:border-sky-500"
                ></textarea>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setSelectedShipment(null)}
                  disabled={updating}
                  className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updating}
                  className="px-5 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-500 rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
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
          <div onClick={() => setTrackingShipment(null)} className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-2xl bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-2xl animate-in scale-in duration-200 z-10 max-h-[90vh] overflow-y-auto space-y-6">
            
            {/* Header */}
            <div className="flex justify-between items-center border-b border-slate-100 pb-4">
              <div>
                <span className="text-[10px] font-mono font-bold text-sky-600 uppercase tracking-widest block">
                  Cargo Waybill & Milestone Log
                </span>
                <h3 className="text-base sm:text-lg font-black text-slate-900 font-mono">
                  {trackingShipment.tracking || `SHP-${trackingShipment.id}`}
                </h3>
              </div>
              <button
                onClick={() => setTrackingShipment(null)}
                className="p-1.5 hover:bg-slate-100 rounded-full cursor-pointer text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Overview Card */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50 border border-slate-200/80 rounded-2xl p-4 text-xs">
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Product</span>
                <span className="font-bold text-slate-800">{trackingShipment.product}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Quantity</span>
                <span className="font-bold text-slate-800">{trackingShipment.qty}</span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Origin → Dest</span>
                <span className="font-bold text-slate-800 truncate block">
                  {countryFlag(trackingShipment.dest)} {trackingShipment.dest}
                </span>
              </div>
              <div>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Current Status</span>
                <span className="font-black text-sky-600 block">{trackingShipment.status}</span>
              </div>
            </div>

            {/* Horizontal Milestone Progress Bar */}
            <div>
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block mb-4">
                Milestone Pipeline
              </span>
              <div className="relative px-2">
                <div className="absolute top-3.5 left-6 right-6 h-1 bg-slate-100 rounded-full z-0"></div>
                {(() => {
                  const currentIdx = SHIPMENT_STEPS.findIndex(s => s.key === trackingShipment.rawStatus);
                  const pct = currentIdx < 0 ? 0 : (currentIdx / (SHIPMENT_STEPS.length - 1)) * 100;
                  return (
                    <div
                      className="absolute top-3.5 left-6 h-1 bg-emerald-500 rounded-full z-0 transition-all duration-500"
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
                              ? 'bg-sky-500 border-sky-400 text-white ring-4 ring-sky-500/20'
                              : isDone
                              ? 'bg-emerald-500 border-emerald-500 text-white'
                              : 'bg-white border-slate-200 text-slate-400'
                          }`}
                        >
                          {isDone && !isCurrent ? (
                            <CheckCircle2 className="w-3.5 h-3.5" />
                          ) : (
                            <span className="text-[9px] font-black">{idx + 1}</span>
                          )}
                        </div>
                        <span
                          className={`text-[8px] font-bold leading-tight ${
                            isCurrent ? 'text-sky-600 font-extrabold' : isDone ? 'text-slate-700' : 'text-slate-400'
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
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block mb-3">
                Tracking History Log ({trackingShipment.trackingHistory ? trackingShipment.trackingHistory.length : 0} events)
              </span>

              {(!trackingShipment.trackingHistory || trackingShipment.trackingHistory.length === 0) ? (
                <div className="p-4 rounded-xl bg-slate-50 border border-slate-100 text-center text-xs text-slate-400">
                  Initial booking completed. No milestone updates posted yet.
                </div>
              ) : (
                <div className="relative pl-6 space-y-4 border-l-2 border-slate-100 ml-3">
                  {trackingShipment.trackingHistory.map((evt, i) => (
                    <div key={evt.id || i} className="relative">
                      <div className="absolute -left-[31px] top-1 w-3 h-3 rounded-full bg-sky-500 ring-4 ring-white"></div>
                      <div className="bg-slate-50/80 p-3 rounded-xl border border-slate-100 text-xs space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-slate-800">
                            {evt.status ? evt.status.replace(/_/g, ' ') : 'UPDATE'}
                          </span>
                          <span className="text-[10px] text-slate-400">
                            {evt.timestamp ? new Date(evt.timestamp).toLocaleString('en-IN') : 'Recent'}
                          </span>
                        </div>
                        {evt.location && (
                          <div className="flex items-center gap-1 text-[11px] text-slate-600">
                            <MapPin className="w-3 h-3 text-sky-500 shrink-0" />
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
