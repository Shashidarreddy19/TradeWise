import React, { useState } from 'react';
import { Eye, X, Loader2, Package, MapPin, Calendar, Truck } from 'lucide-react';
import { countryFlag } from './utils';

export default function AssignedOrdersView({ orders, onAccept, onReject, addToast }) {
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  const handleAccept = async (order) => {
    setActionLoading(true);
    try {
      await onAccept(order);
      setSelectedOrder(null);
    } catch (err) {
      addToast(err.message || 'Failed to accept', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async (orderId) => {
    setActionLoading(true);
    try {
      await onReject(orderId);
      setSelectedOrder(null);
    } catch (err) {
      addToast(err.message || 'Failed to reject', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Available Export Assignments</h1>
        <p className="text-xs text-slate-500 mt-1 font-medium">Review incoming shipment requests from exporters and accept to take ownership.</p>
      </div>

      <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-450 uppercase tracking-widest">
                <th className="py-4 px-5">Order ID</th>
                <th className="py-4 px-5">Exporter</th>
                <th className="py-4 px-5">Product</th>
                <th className="py-4 px-5">HS Code</th>
                <th className="py-4 px-5">Quantity</th>
                <th className="py-4 px-5">Destination</th>
                <th className="py-4 px-5">Mode</th>
                <th className="py-4 px-5">Status</th>
                <th className="py-4 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
              {orders.length === 0 ? (
                <tr><td colSpan="9" className="py-14 text-center">
                  <Package className="w-10 h-10 text-slate-200 mx-auto mb-3" />
                  <span className="text-sm font-bold text-slate-400 block">No pending requests</span>
                  <span className="text-[10px] text-slate-400 mt-1 block">Exporters haven't placed any shipment requests yet.</span>
                </td></tr>
              ) : orders.map(o => (
                <tr key={o.id} className="hover:bg-slate-50/40 transition-colors">
                  <td className="py-4 px-5 font-mono font-bold text-slate-900">#{o.id}</td>
                  <td className="py-4 px-5">
                    <span className="block font-bold text-slate-800">{o.exporterName || 'N/A'}</span>
                    <span className="block text-[10px] text-slate-400">{o.exporterCompany || ''}</span>
                  </td>
                  <td className="py-4 px-5 text-slate-800 font-bold">{o.product}</td>
                  <td className="py-4 px-5 font-mono text-slate-500">{o.hscode || '—'}</td>
                  <td className="py-4 px-5 text-slate-500">{o.qty}</td>
                  <td className="py-4 px-5">
                    <div className="flex items-center gap-1.5">
                      <span className="text-base">{countryFlag(o.country)}</span>
                      <span className="font-bold text-slate-800">{o.country}</span>
                    </div>
                  </td>
                  <td className="py-4 px-5 text-slate-500">{o.shippingRequirements || 'Sea Freight'}</td>
                  <td className="py-4 px-5">
                    {o.rawStatus === 'PENDING_LOGISTICS' ? (
                      <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span> Pending
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Accepted
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                    <button onClick={() => setSelectedOrder(o)} className="px-3 py-1.5 text-[10px] font-bold text-slate-600 bg-slate-50 hover:bg-slate-100 rounded-lg cursor-pointer transition-all border border-slate-200">
                      <Eye className="w-3.5 h-3.5 inline mr-1" />Details
                    </button>
                    {o.rawStatus === 'PENDING_LOGISTICS' && (
                      <>
                        <button onClick={() => handleAccept(o)} className="px-3 py-1.5 text-[10px] font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-lg cursor-pointer transition-all">Accept</button>
                        <button onClick={() => handleReject(o.id)} className="px-3 py-1.5 text-[10px] font-bold text-red-600 border border-red-100 hover:bg-red-50 rounded-lg cursor-pointer transition-all">Reject</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Order Detail Modal */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedOrder(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
            {/* Header */}
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Assignment Details — #{selectedOrder.id}</h3>
              <button onClick={() => setSelectedOrder(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700"><X className="w-4 h-4" /></button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Exporter */}
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 text-xs space-y-2">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block border-b border-slate-100 pb-1">Exporter</span>
                <span className="block text-slate-900 font-extrabold">{selectedOrder.exporterName || 'N/A'}</span>
                <span className="block text-slate-500">{selectedOrder.exporterCompany || ''}</span>
              </div>
              {/* Product */}
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 text-xs space-y-2">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block border-b border-slate-100 pb-1">Product Specs</span>
                <span className="block text-slate-900 font-extrabold">{selectedOrder.product}</span>
                <span className="block text-slate-500 font-mono">HS: {selectedOrder.hscode || '—'}</span>
                <span className="block text-slate-500">Qty: {selectedOrder.qty} · Value: {selectedOrder.value}</span>
              </div>
            </div>

            {/* Logistics info */}
            <div className="grid grid-cols-2 gap-3 border border-slate-100 rounded-xl p-4 text-xs bg-slate-50/10">
              <div>
                <span className="text-[9px] font-black text-slate-400 uppercase block mb-1 flex items-center gap-1"><MapPin className="w-3 h-3" />Pickup Location</span>
                <span className="text-slate-700 font-semibold">{selectedOrder.pickupLocation || 'Mumbai, Maharashtra'}</span>
              </div>
              <div>
                <span className="text-[9px] font-black text-slate-400 uppercase block mb-1 flex items-center gap-1"><Truck className="w-3 h-3" />Shipping Mode</span>
                <span className="text-slate-700 font-semibold">{selectedOrder.shippingRequirements || 'Sea Freight'}</span>
              </div>
              <div>
                <span className="text-[9px] font-black text-slate-400 uppercase block mb-1">Destination</span>
                <span className="text-slate-700 font-semibold">{countryFlag(selectedOrder.country)} {selectedOrder.country}</span>
              </div>
              <div>
                <span className="text-[9px] font-black text-slate-400 uppercase block mb-1 flex items-center gap-1"><Calendar className="w-3 h-3" />Created</span>
                <span className="text-slate-500">{selectedOrder.createdAt ? new Date(selectedOrder.createdAt).toLocaleDateString('en-IN') : '—'}</span>
              </div>
              {selectedOrder.specialInstructions && (
                <div className="col-span-2">
                  <span className="text-[9px] font-black text-slate-400 uppercase block mb-1">Special Instructions</span>
                  <span className="text-slate-600 italic">{selectedOrder.specialInstructions}</span>
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex gap-2 pt-1">
              {selectedOrder.rawStatus === 'PENDING_LOGISTICS' ? (
                <>
                  <button onClick={() => handleAccept(selectedOrder)} disabled={actionLoading}
                    className="flex-grow py-3 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center gap-1.5">
                    {actionLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : null} Accept Shipment
                  </button>
                  <button onClick={() => handleReject(selectedOrder.id)} disabled={actionLoading}
                    className="px-5 py-3 text-xs font-bold text-red-600 border border-red-200 hover:bg-red-50 rounded-xl transition-all cursor-pointer disabled:opacity-50">
                    Reject
                  </button>
                </>
              ) : (
                <div className="w-full text-center py-2.5 bg-emerald-50 text-emerald-600 text-xs font-extrabold rounded-xl uppercase tracking-wider">
                  Assignment Locked — Shipment Created
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
