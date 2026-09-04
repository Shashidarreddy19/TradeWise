import React from 'react';
import { Eye, X, Check } from 'lucide-react';

/**
 * OrdersView — Export orders table, order details modal, and shipment timeline modal.
 */
export default function OrdersView({
  orders, shipments, orderFilter, setOrderFilter,
  selectedOrder, setSelectedOrder, selectedShipment, setSelectedShipment,
  assigningPartner, setAssigningPartner,
  handleAcceptOrder, handleRejectOrder, handleAssignLogistics, addToast,
}) {
  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Export Orders</h1>
        <p className="text-xs text-slate-500 mt-1 font-medium">Manage incoming contracts, tracking logs, and freight assignments.</p>
      </div>

      {/* Filter buttons */}
      <div className="flex gap-2 border-b border-slate-100 text-xs font-bold text-slate-450 mb-4 pb-1">
        {['All', 'Pending', 'Accepted', 'Shipped', 'Rejected'].map((status) => (
          <button
            key={status}
            onClick={() => setOrderFilter(status)}
            className={`pb-2 px-1 cursor-pointer transition-colors relative ${orderFilter === status ? 'text-sky-655 font-extrabold' : 'hover:text-slate-850'}`}
          >
            {status} {status === 'Pending' && `(${orders.filter(o => o.status === 'Pending').length})`}
          </button>
        ))}
      </div>

      {/* Orders Table */}
      <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-455 uppercase tracking-widest">
                <th className="py-4 px-5">Order ID</th>
                <th className="py-4 px-5">Product</th>
                <th className="py-4 px-5">Destination Country</th>
                <th className="py-4 px-5">Quantity</th>
                <th className="py-4 px-5">Value</th>
                <th className="py-4 px-5">Logistics Partner</th>
                <th className="py-4 px-5">Status</th>
                <th className="py-4 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
              {orders
                .filter(o => orderFilter === 'All' || o.status === orderFilter)
                .map(o => {
                  const shipment = shipments.find(s => s.orderId === o.id);
                  const partner = shipment ? shipment.logistics : (o.logisticsPartner || 'TBD');
                  return (
                    <tr key={o.id} className="hover:bg-slate-50/40 transition-colors">
                      <td className="py-4 px-5 font-mono text-slate-900 font-bold">#{o.id}</td>
                      <td className="py-4 px-5 text-slate-800">{o.product}</td>
                      <td className="py-4 px-5">
                        <span className="font-bold text-slate-800">{o.country}</span>
                      </td>
                      <td className="py-4 px-5 text-slate-500">{o.qty}</td>
                      <td className="py-4 px-5 text-slate-805">{o.value}</td>
                      <td className="py-4 px-5 text-slate-500 font-semibold">{partner}</td>
                      <td className="py-4 px-5">
                        {o.status === 'Pending' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span> Pending
                          </span>
                        )}
                        {o.status === 'Accepted' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Accepted
                          </span>
                        )}
                        {o.status === 'Rejected' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-red-500"></span> Rejected
                          </span>
                        )}
                        {o.status === 'Shipped' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-sky-655 bg-sky-50 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-sky-500"></span> In Transit
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-5 text-right">
                        <button
                          onClick={() => setSelectedOrder(o)}
                          className="p-1.5 text-slate-400 hover:text-sky-655 rounded-lg hover:bg-slate-50 cursor-pointer"
                          title="Inspect Details"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
            </tbody>
          </table>
        </div>
      </div>

      {/* ORDER DETAILS MODAL */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedOrder(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-6">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Order Details #{selectedOrder.id}</h3>
              <button onClick={() => setSelectedOrder(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 text-xs font-semibold space-y-2">
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block border-b border-slate-100 pb-1">Importer Info</span>
                <span className="block text-slate-900 font-extrabold">{selectedOrder.importer}</span>
                <span className="block text-slate-550">{selectedOrder.country}</span>
                <span className="block text-slate-400 font-mono mt-2 truncate">{selectedOrder.email}</span>
                <span className="block text-slate-400 font-mono">{selectedOrder.phone}</span>
              </div>
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 text-xs font-semibold space-y-2">
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block border-b border-slate-100 pb-1">Product Details</span>
                <span className="block text-slate-900 font-extrabold">{selectedOrder.product}</span>
                <span className="block text-slate-500 font-mono">HS Code: {selectedOrder.hscode}</span>
                <div className="pt-2 flex justify-between">
                  <span className="text-slate-400">Quantity</span>
                  <span className="text-slate-800">{selectedOrder.qty}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Total Value</span>
                  <span className="text-sky-655 font-bold">{selectedOrder.value}</span>
                </div>
              </div>
            </div>

            <div className="border border-slate-100 rounded-xl p-4 text-xs font-bold text-slate-500 space-y-2">
              <div className="flex justify-between"><span>Order Date</span><span className="text-slate-700">{selectedOrder.date}</span></div>
              <div className="flex justify-between"><span>Expected Delivery</span><span className="text-slate-700">{selectedOrder.delivery}</span></div>
            </div>

            <div className="flex flex-col sm:flex-row gap-2 pt-2">
              {selectedOrder.status === 'Pending' ? (
                <>
                  <button onClick={() => handleAcceptOrder(selectedOrder.id)} className="flex-grow py-3 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow-md transition-all cursor-pointer">Accept Order</button>
                  <button onClick={() => handleRejectOrder(selectedOrder.id)} className="px-4 py-3 text-xs font-bold text-red-655 border border-red-200 hover:bg-red-50 rounded-xl transition-all cursor-pointer">Reject Order</button>
                </>
              ) : selectedOrder.status === 'Accepted' && (!selectedOrder.logisticsPartner || selectedOrder.logisticsPartner === 'TBD') ? (
                <div className="w-full space-y-2">
                  <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block">Assign Logistics Partner</span>
                  <div className="flex gap-2 items-center">
                    <select value={assigningPartner} onChange={(e) => setAssigningPartner(e.target.value)} className="flex-grow px-3.5 py-2.5 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer">
                      <option value="">Select Partner...</option>
                      <option>FastCargo Logistics</option>
                      <option>GlobalShip Inc.</option>
                      <option>TransWorld Logistics</option>
                    </select>
                    <button onClick={() => handleAssignLogistics(selectedOrder.id, assigningPartner)} className="px-4 py-2.5 bg-sky-500 hover:bg-sky-400 text-white text-xs font-bold rounded-xl shadow transition-all cursor-pointer">Assign</button>
                  </div>
                </div>
              ) : (
                <button onClick={() => { setSelectedShipment(shipments.find(s => s.orderId === selectedOrder.id) || null); setSelectedOrder(null); }} className="w-full py-3 text-xs font-bold text-slate-700 border border-slate-200 hover:bg-slate-50 rounded-xl transition-all cursor-pointer">
                  {selectedOrder.logisticsPartner ? `Track with ${selectedOrder.logisticsPartner}` : 'Track Shipment'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* SHIPMENT TIMELINE MODAL */}
      {selectedShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedShipment(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-md bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Shipment Timeline #{selectedShipment.id}</h3>
              <button onClick={() => setSelectedShipment(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="border border-slate-100 rounded-xl p-4 text-xs font-semibold text-slate-500 space-y-2 bg-slate-50/20">
              <div className="flex justify-between"><span>Logistics Partner</span><span className="text-slate-800 font-extrabold">{selectedShipment.logistics}</span></div>
              <div className="flex justify-between"><span>Shipping Method</span><span className="text-slate-750">{selectedShipment.method}</span></div>
              <div className="flex justify-between"><span>Tracking ID</span><span className="text-slate-750 font-mono">{selectedShipment.tracking}</span></div>
              <div className="flex justify-between"><span>ETA</span><span className="text-slate-800">{selectedShipment.eta}</span></div>
            </div>

            <div className="space-y-4">
              <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest">Status Progress</h4>
              <div className="relative pl-6 space-y-6">
                <div className="absolute left-[7px] top-2.5 bottom-2.5 w-0.5 bg-slate-100 z-0"></div>
                <div className="relative z-10 text-xs">
                  <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-emerald-500 shadow-sm flex items-center justify-center"><Check className="w-2 h-2 text-white" /></div>
                  <span className="block font-bold text-emerald-600">Order Confirmed</span>
                  <span className="block text-[10px] text-slate-400">Payment cleared & product packed</span>
                </div>
                <div className="relative z-10 text-xs">
                  <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-emerald-500 shadow-sm flex items-center justify-center"><Check className="w-2 h-2 text-white" /></div>
                  <span className="block font-bold text-emerald-600">Cargo Picked Up</span>
                  <span className="block text-[10px] text-slate-400">Loaded into transport fleet</span>
                </div>
                <div className="relative z-10 text-xs">
                  <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-sky-500 shadow-sm flex items-center justify-center animate-pulse"><span className="w-1.5 h-1.5 rounded-full bg-white"></span></div>
                  <span className="block font-bold text-sky-655">In Transit (Current)</span>
                  <span className="block text-[10px] text-slate-450 mt-0.5">{selectedShipment.currentLoc}</span>
                </div>
                <div className="relative z-10 text-xs">
                  <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-slate-200 bg-white shadow-sm"></div>
                  <span className="block font-semibold text-slate-400">Customs Clearance — Pending</span>
                  <span className="block text-[10px] text-slate-400">Audit of import certifications</span>
                </div>
                <div className="relative z-10 text-xs">
                  <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-slate-200 bg-white shadow-sm"></div>
                  <span className="block font-semibold text-slate-400">Delivered — Estimated {selectedShipment.eta}</span>
                  <span className="block text-[10px] text-slate-400">Sign-off by buyer</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
