import React, { useState } from 'react';
import { X, Loader2, Truck } from 'lucide-react';
import { countryFlag, SHIPMENT_STEPS, statusToBadgeClass } from './utils';
import { shipmentsApi } from '../../services';

export default function ShipmentsView({ shipments, onStatusUpdated, addToast }) {
  const [selectedShipment, setSelectedShipment] = useState(null);
  const [trackingShipment, setTrackingShipment] = useState(null);
  const [newStatus, setNewStatus] = useState('');
  const [updating, setUpdating] = useState(false);
  const [filterStatus, setFilterStatus] = useState('All');

  const filtered = filterStatus === 'All' ? shipments
    : filterStatus === 'Active' ? shipments.filter(s => s.rawStatus !== 'DELIVERED')
    : shipments.filter(s => s.rawStatus === 'DELIVERED');

  const handleUpdateStatus = async () => {
    if (!newStatus || !selectedShipment) return;
    setUpdating(true);
    try {
      await shipmentsApi.updateStatus(selectedShipment.id, newStatus);
      addToast(`Shipment #${selectedShipment.id} updated to: ${SHIPMENT_STEPS.find(s => s.key === newStatus)?.label}`, 'success');
      await onStatusUpdated();
      setSelectedShipment(null);
      setNewStatus('');
    } catch (err) {
      addToast(err.message || 'Status update failed', 'error');
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Shipment Tracking</h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">Track, update, and manage all assigned shipments.</p>
        </div>
        <div className="flex gap-2">
          {['All', 'Active', 'Delivered'].map(f => (
            <button key={f} onClick={() => setFilterStatus(f)}
              className={`px-3 py-1.5 text-[10px] font-bold rounded-lg cursor-pointer transition-all ${filterStatus === f ? 'bg-sky-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>
              {f}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-450 uppercase tracking-widest">
                <th className="py-4 px-5">Shipment #</th>
                <th className="py-4 px-5">Order</th>
                <th className="py-4 px-5">Exporter</th>
                <th className="py-4 px-5">Product</th>
                <th className="py-4 px-5">Destination</th>
                <th className="py-4 px-5">Tracking</th>
                <th className="py-4 px-5">Status</th>
                <th className="py-4 px-5">ETA</th>
                <th className="py-4 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
              {filtered.length === 0 ? (
                <tr><td colSpan="9" className="py-12 text-center">
                  <Truck className="w-10 h-10 text-slate-200 mx-auto mb-3" />
                  <span className="text-sm font-bold text-slate-400 block">No shipments found</span>
                </td></tr>
              ) : filtered.map(s => (
                <tr key={s.id} className="hover:bg-slate-50/40 transition-colors">
                  <td className="py-4 px-5 font-mono font-bold text-slate-900">#{s.id}</td>
                  <td className="py-4 px-5 font-mono text-slate-400">#{s.orderId}</td>
                  <td className="py-4 px-5">
                    <span className="block font-bold text-slate-800">{s.exporter || '—'}</span>
                    <span className="block text-[10px] text-slate-400">{s.exporterCompany || ''}</span>
                  </td>
                  <td className="py-4 px-5 text-slate-800 font-bold">{s.product}</td>
                  <td className="py-4 px-5">
                    <div className="flex items-center gap-1.5">
                      <span className="text-base">{countryFlag(s.dest)}</span>
                      <span className="font-bold text-slate-800">{s.dest}</span>
                    </div>
                  </td>
                  <td className="py-4 px-5 font-mono text-slate-500 text-[10px]">{s.tracking || '—'}</td>
                  <td className="py-4 px-5">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase ${statusToBadgeClass(s.status)}`}>{s.status}</span>
                  </td>
                  <td className="py-4 px-5 text-slate-500">{s.eta || 'TBD'}</td>
                  <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                    <button onClick={() => { setSelectedShipment(s); setNewStatus(s.rawStatus); }}
                      className="px-3 py-1.5 text-[10px] font-bold text-indigo-600 border border-slate-200 hover:bg-indigo-50 rounded-lg cursor-pointer transition-all">
                      Manage
                    </button>
                    <button onClick={() => setTrackingShipment(s)}
                      className="px-3 py-1.5 text-[10px] font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-lg cursor-pointer transition-all">
                      Track
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Manage Status Modal */}
      {selectedShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedShipment(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Manage Shipment #{selectedShipment.id}</h3>
              <button onClick={() => setSelectedShipment(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400"><X className="w-4 h-4" /></button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 border border-slate-100 rounded-xl bg-slate-50/20 space-y-1.5">
                <span className="text-[9px] font-black text-slate-400 uppercase block">Product</span>
                <span className="font-bold text-slate-900">{selectedShipment.product}</span>
                <span className="text-slate-500 block">{selectedShipment.qty}</span>
                <span className="font-mono text-[10px] text-slate-400 block">{selectedShipment.tracking}</span>
              </div>
              <div className="p-3 border border-slate-100 rounded-xl bg-slate-50/20 space-y-1.5">
                <span className="text-[9px] font-black text-slate-400 uppercase block">Route</span>
                <span className="text-slate-700 block">{selectedShipment.origin || 'Mumbai, India'}</span>
                <span className="text-slate-500 block">→ {countryFlag(selectedShipment.dest)} {selectedShipment.dest}</span>
                <span className="text-slate-400 block text-[10px]">ETA: {selectedShipment.eta || 'TBD'}</span>
              </div>
            </div>

            {/* Vertical timeline */}
            <div>
              <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block mb-3">Milestone Progress</span>
              <div className="relative pl-6 space-y-3">
                <div className="absolute left-[7px] top-2 bottom-2 w-0.5 bg-slate-100"></div>
                {SHIPMENT_STEPS.map((step, idx) => {
                  const currentIdx = SHIPMENT_STEPS.findIndex(s => s.key === selectedShipment.rawStatus);
                  const isDone = idx <= currentIdx;
                  const isCurrent = idx === currentIdx;
                  return (
                    <div key={step.key} className="relative z-10 flex items-center gap-2">
                      <div className={`absolute -left-[23px] top-0.5 w-3.5 h-3.5 rounded-full border-2 border-white flex items-center justify-center ${isCurrent ? 'bg-sky-500 ring-4 ring-sky-500/20' : isDone ? 'bg-emerald-500' : 'bg-slate-100'}`}>
                        {isDone && !isCurrent && <span className="text-[8px] text-white">✓</span>}
                      </div>
                      <span className={`text-xs font-bold ${isCurrent ? 'text-sky-600 font-extrabold' : isDone ? 'text-slate-700' : 'text-slate-400'}`}>{step.label}</span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Update dropdown */}
            {selectedShipment.rawStatus !== 'DELIVERED' && (
              <div className="border-t border-slate-100 pt-4 flex items-end gap-3">
                <div className="flex-grow space-y-1.5">
                  <label className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Update Milestone</label>
                  <select value={newStatus} onChange={e => setNewStatus(e.target.value)}
                    className="w-full px-3 py-2.5 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-indigo-500 cursor-pointer">
                    {SHIPMENT_STEPS.map(s => <option key={s.key} value={s.key}>{s.label}</option>)}
                  </select>
                </div>
                <button onClick={handleUpdateStatus} disabled={updating || newStatus === selectedShipment.rawStatus}
                  className="px-5 py-2.5 bg-indigo-500 hover:bg-indigo-400 text-white text-xs font-bold rounded-xl shadow transition-all cursor-pointer disabled:opacity-40 flex items-center gap-1.5">
                  {updating ? <Loader2 className="w-4 h-4 animate-spin" /> : null} Update
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tracking Modal */}
      {trackingShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setTrackingShipment(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest font-mono">Live Tracking: {trackingShipment.tracking || `SHP-${trackingShipment.id}`}</h3>
              <button onClick={() => setTrackingShipment(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400"><X className="w-4 h-4" /></button>
            </div>

            {/* Info */}
            <div className="grid grid-cols-2 gap-4 border border-slate-100 rounded-xl p-4 text-xs bg-slate-50/20">
              <div><span className="text-[9px] font-black text-slate-400 uppercase block mb-1">Product</span><span className="font-bold text-slate-800">{trackingShipment.product}</span></div>
              <div><span className="text-[9px] font-black text-slate-400 uppercase block mb-1">Destination</span><span className="font-bold text-slate-800">{countryFlag(trackingShipment.dest)} {trackingShipment.dest}</span></div>
              <div><span className="text-[9px] font-black text-slate-400 uppercase block mb-1">Current Status</span><span className="text-indigo-600 font-black">{trackingShipment.status}</span></div>
              <div><span className="text-[9px] font-black text-slate-400 uppercase block mb-1">ETA</span><span className="font-bold text-slate-800">{trackingShipment.eta || 'TBD'}</span></div>
            </div>

            {/* Horizontal progress */}
            <div>
              <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block mb-4">Route Milestones</span>
              <div className="relative">
                <div className="absolute top-3.5 left-3 right-3 h-1 bg-slate-100 rounded-full z-0"></div>
                {(() => {
                  const currentIdx = SHIPMENT_STEPS.findIndex(s => s.key === trackingShipment.rawStatus);
                  const pct = currentIdx < 0 ? 0 : (currentIdx / (SHIPMENT_STEPS.length - 1)) * 100;
                  return <div className="absolute top-3.5 left-3 h-1 bg-emerald-500 rounded-full z-0 transition-all duration-500" style={{ width: `calc(${pct}% - 24px)` }}></div>;
                })()}
                <div className="flex justify-between relative z-10">
                  {SHIPMENT_STEPS.map((step, idx) => {
                    const currentIdx = SHIPMENT_STEPS.findIndex(s => s.key === trackingShipment.rawStatus);
                    const isDone = idx <= currentIdx;
                    const isCurrent = idx === currentIdx;
                    return (
                      <div key={step.key} className="flex flex-col items-center text-center space-y-1.5 max-w-[60px]">
                        <div className={`w-7 h-7 rounded-full border-2 flex items-center justify-center transition-all ${isCurrent ? 'bg-white border-sky-500 text-sky-500 ring-4 ring-sky-500/15' : isDone ? 'bg-emerald-500 border-emerald-500 text-white' : 'bg-white border-slate-200 text-slate-400'}`}>
                          <span className="text-[8px] font-black">{idx + 1}</span>
                        </div>
                        <span className={`text-[8px] font-bold leading-tight ${isCurrent ? 'text-sky-600 font-extrabold' : isDone ? 'text-slate-700' : 'text-slate-400'}`}>{step.short}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
