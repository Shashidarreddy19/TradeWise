import React from 'react';
import { Briefcase, ShoppingCart, Truck, Activity, Plus, Globe, ArrowUpRight } from 'lucide-react';
import { getUser } from '../../services';

/**
 * OverviewView — Exporter Dashboard overview tab.
 * Shows stats cards, quick actions, recent activity, and product performance.
 */
export default function OverviewView({
  products, orders, dashboardStats,
  setActiveView, setAnalysisSubView, setOrderFilter,
  setErrors, setEditingProductId, setNewProdName, setNewProdHscode,
  setNewProdDesc, setNewProdPrice, setNewProdWeight, setShowAddDrawer,
  setSelectedAnalysisProduct,
}) {
  return (
    <div className="space-y-8 animate-in fade-in duration-300">

      {/* Dynamic Greeting */}
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">
          {(() => {
            const hour = new Date().getHours();
            if (hour < 12) return 'Good Morning,';
            if (hour < 17) return 'Good Afternoon,';
            return 'Good Evening,';
          })()}
          {' '}{getUser()?.name || 'Exporter'}
        </h1>
        <p className="text-xs text-slate-500 mt-1 font-medium">
          Here is your export intelligence dashboard overview.
        </p>
      </div>

      {/* Grid of 4 Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">

        {/* Products Card */}
        <div
          onClick={() => setActiveView('products')}
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm hover:shadow-md hover:border-sky-200 cursor-pointer transition-all duration-300 relative group overflow-hidden"
        >
          <div className="absolute top-0 left-0 w-1 h-full bg-sky-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-sky-50 text-sky-600 group-hover:scale-110 transition-transform">
              <Briefcase className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{dashboardStats ? dashboardStats.totalProducts : products.length}</span>
            <span className="text-xxs font-bold text-slate-450 uppercase tracking-widest mt-1 block">Catalog Products</span>
          </div>
        </div>

        {/* Analyses Card */}
        <div
          onClick={() => { setActiveView('analysis'); setAnalysisSubView('select'); }}
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm hover:shadow-md hover:border-emerald-250 cursor-pointer transition-all duration-300 relative group overflow-hidden"
        >
          <div className="absolute top-0 left-0 w-1 h-full bg-emerald-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600 group-hover:scale-110 transition-transform">
              <Activity className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{dashboardStats ? dashboardStats.acceptedByLogistics : 0}</span>
            <span className="text-xxs font-bold text-slate-450 uppercase tracking-widest mt-1 block">Accepted by Logistics</span>
          </div>
        </div>

        {/* Pending Orders */}
        <div
          onClick={() => setActiveView('orders')}
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm hover:shadow-md hover:border-amber-250 cursor-pointer transition-all duration-300 relative group overflow-hidden"
        >
          <div className="absolute top-0 left-0 w-1 h-full bg-amber-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-amber-50 text-amber-600 group-hover:scale-110 transition-transform">
              <ShoppingCart className="w-5 h-5" />
            </div>
            <span className="text-[10px] font-extrabold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">{dashboardStats ? dashboardStats.pendingLogisticsRequests : 0} Action</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{dashboardStats ? dashboardStats.pendingLogisticsRequests : 0}</span>
            <span className="text-xxs font-bold text-slate-450 uppercase tracking-widest mt-1 block">Pending Requests</span>
          </div>
        </div>

        {/* Active Shipments */}
        <div
          onClick={() => { setActiveView('orders'); setOrderFilter('Shipped'); }}
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm hover:shadow-md hover:border-indigo-200 cursor-pointer transition-all duration-300 relative group overflow-hidden"
        >
          <div className="absolute top-0 left-0 w-1 h-full bg-indigo-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 group-hover:scale-110 transition-transform">
              <Truck className="w-5 h-5" />
            </div>
            <span className="text-[10px] font-extrabold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-full">{dashboardStats ? dashboardStats.inTransit : 0} In Transit</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{dashboardStats ? dashboardStats.delivered : 0}</span>
            <span className="text-xxs font-bold text-slate-450 uppercase tracking-widest mt-1 block">Delivered</span>
          </div>
        </div>

      </div>

      {/* Quick Actions & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

        {/* Quick Actions (7 Cols) */}
        <div className="lg:col-span-7 bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
          <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest mb-4">Quick Actions</h3>
          <div className="flex flex-col gap-3">
            <button
              onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); setActiveView('products'); }}
              className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <Plus className="w-5 h-5 text-sky-500 group-hover:rotate-90 transition-transform" />
                <div>
                  <span className="block text-xs font-bold text-slate-850">Add Product Specs</span>
                  <span className="block text-[10px] text-slate-400 mt-0.5">Define HS codes and price models</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>

            <button
              onClick={() => { setActiveView('analysis'); setAnalysisSubView('select'); }}
              className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <Globe className="w-5 h-5 text-emerald-500 group-hover:animate-pulse" />
                <div>
                  <span className="block text-xs font-bold text-slate-850">Start Market Intelligence Analysis</span>
                  <span className="block text-[10px] text-slate-400 mt-0.5">Explore high margins and import complexity</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>

            <button
              onClick={() => setActiveView('orders')}
              className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <ShoppingCart className="w-5 h-5 text-amber-500" />
                <div>
                  <span className="block text-xs font-bold text-slate-850">Inspect Orders</span>
                  <span className="block text-[10px] text-slate-400 mt-0.5">Clear pending buyer bids and accept contract rates</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>

            <button
              onClick={() => { setActiveView('orders'); setOrderFilter('All'); }}
              className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <Truck className="w-5 h-5 text-indigo-500 group-hover:translate-x-1 transition-transform" />
                <div>
                  <span className="block text-xs font-bold text-slate-855">Track Shipment Status</span>
                  <span className="block text-[10px] text-slate-400 mt-0.5">Customs updates and ETA timings</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>
          </div>
        </div>

        {/* Recent Activity (5 Cols) */}
        <div className="lg:col-span-5 bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest mb-4">Recent Activity</h3>
            <div className="relative pl-6 space-y-4">
              <div className="absolute left-[7px] top-2 bottom-2 w-0.5 bg-slate-100 z-0"></div>
              {(() => {
                const activities = [];
                orders.slice(0, 5).forEach(o => {
                  const statusColors = { 'Pending': 'bg-amber-500', 'Accepted': 'bg-sky-500', 'In Transit': 'bg-indigo-500', 'Shipped': 'bg-indigo-500', 'Delivered': 'bg-emerald-500' };
                  activities.push({ id: `order-${o.id}`, title: `Export Order #${o.id} — ${o.status}`, desc: `${o.product} · ${o.qty} to ${o.country}`, time: o.date || 'Recently', color: statusColors[o.status] || 'bg-slate-400' });
                });
                products.slice(-3).reverse().forEach(p => {
                  activities.push({ id: `product-${p.id}`, title: 'Product added to catalog', desc: `${p.name} (HS ${p.hscode || 'pending'})`, time: 'Recently', color: 'bg-sky-500' });
                });
                const display = activities.slice(0, 4);
                if (display.length === 0) {
                  return (
                    <div className="py-6 text-center">
                      <span className="text-xxs text-slate-400 font-bold">No recent activity yet.</span>
                      <p className="text-[10px] text-slate-400 mt-1">Create a product or start a market analysis to see activity here.</p>
                    </div>
                  );
                }
                return display.map(a => (
                  <div key={a.id} className="relative z-10">
                    <div className={`absolute -left-[23px] top-1.5 w-3.5 h-3.5 rounded-full border-2 border-white ${a.color}`}></div>
                    <div className="text-xxs">
                      <span className="block font-bold text-slate-800">{a.title}</span>
                      <span className="block text-slate-450 mt-0.5">{a.desc}</span>
                      <span className="block text-[10px] text-slate-400 mt-1 font-semibold">{a.time}</span>
                    </div>
                  </div>
                ));
              })()}
            </div>
          </div>
          <button
            onClick={() => setActiveView('orders')}
            className="w-full text-center py-2.5 mt-4 text-xxs font-bold text-indigo-600 hover:text-indigo-700 transition-colors border border-slate-100 hover:border-slate-200 rounded-xl cursor-pointer"
          >
            View All Activity
          </button>
        </div>

      </div>

      {/* Product Performance Overview */}
      <div className="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
        <h3 className="text-sm font-black text-slate-805 uppercase tracking-widest mb-4">Product Performance Overview</h3>
        {products.length > 0 ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {products.slice(0, 4).map(p => {
              const productOrders = orders.filter(o => o.product === p.name);
              const deliveredCount = productOrders.filter(o => o.status === 'Delivered').length;
              const totalOrders = productOrders.length;
              const index = totalOrders > 0 ? Math.min(99, Math.round((deliveredCount / totalOrders) * 100) + 50) : 0;
              const markets = [...new Set(productOrders.map(o => o.country).filter(Boolean))];
              const indexColor = index >= 70 ? 'bg-emerald-50 text-emerald-600' : index >= 40 ? 'bg-amber-50 text-amber-600' : 'bg-slate-50 text-slate-500';
              return (
                <div key={p.id} className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 text-center cursor-pointer hover:border-sky-200 transition-all" onClick={() => { setSelectedAnalysisProduct(p.name); setActiveView('analysis'); setAnalysisSubView('select'); }}>
                  <span className="block text-xs font-black text-slate-800 truncate">{p.name}</span>
                  <span className="block text-[10px] text-slate-450 mt-0.5">{markets.length > 0 ? `${markets.length} market${markets.length > 1 ? 's' : ''}` : 'No orders yet'}</span>
                  <div className={`mt-3 flex items-center justify-center gap-1 ${indexColor} text-xxs font-black py-1 px-2.5 rounded-full w-max mx-auto`}>
                    <span>{index > 0 ? `Index: ${index}%` : 'Analyze'}</span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="py-8 text-center">
            <span className="text-xxs text-slate-400 font-bold block">No products yet.</span>
            <p className="text-[10px] text-slate-400 mt-1">Add your first product to see performance metrics here.</p>
            <button onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); setActiveView('products'); }} className="mt-3 px-4 py-2 text-xxs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl cursor-pointer transition-all">Add Product</button>
          </div>
        )}
      </div>
    </div>
  );
}
