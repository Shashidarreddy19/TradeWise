import React from 'react';
import { Briefcase, ShoppingCart, Truck, Activity, Plus, Globe, ArrowUpRight } from 'lucide-react';
import { getUser } from '../../services';

/**
 * OverviewView — Exporter Dashboard overview tab.
 * Redesigned with Claude-inspired shadcn/ui design system.
 */
export default function OverviewView({
  products, orders, dashboardStats,
  setActiveView, setAnalysisSubView, setOrderFilter,
  setErrors, setEditingProductId, setNewProdName, setNewProdHscode,
  setNewProdDesc, setNewProdPrice, setNewProdWeight, setShowAddDrawer,
  setSelectedAnalysisProduct,
}) {
  return (
    <div className="space-y-6 animate-in fade-in duration-200">

      {/* Dynamic Greeting */}
      <div>
        <h1 className="text-2xl font-bold text-foreground tracking-tight">
          {(() => {
            const hour = new Date().getHours();
            if (hour < 12) return 'Good morning,';
            if (hour < 17) return 'Good afternoon,';
            return 'Good evening,';
          })()}
          {' '}{getUser()?.name || 'Exporter'}
        </h1>
        <p className="text-xs text-muted-foreground mt-1">
          Here is your export intelligence overview and operations summary.
        </p>
      </div>

      {/* Grid of 4 Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">

        {/* Products Card */}
        <div
          onClick={() => setActiveView('products')}
          className="card-claude p-4 cursor-pointer hover:border-primary/40 transition-all relative group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-primary/10 text-primary group-hover:scale-105 transition-transform">
              <Briefcase className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{dashboardStats ? dashboardStats.totalProducts : products.length}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Catalog Products</span>
          </div>
        </div>

        {/* Analyses Card */}
        <div
          onClick={() => { setActiveView('analysis'); setAnalysisSubView('select'); }}
          className="card-claude p-4 cursor-pointer hover:border-primary/40 transition-all relative group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 group-hover:scale-105 transition-transform">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{dashboardStats ? dashboardStats.acceptedByLogistics : 0}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Accepted by Logistics</span>
          </div>
        </div>

        {/* Pending Orders */}
        <div
          onClick={() => setActiveView('orders')}
          className="card-claude p-4 cursor-pointer hover:border-primary/40 transition-all relative group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 group-hover:scale-105 transition-transform">
              <ShoppingCart className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-amber-700 dark:text-amber-300 bg-amber-500/10 px-2 py-0.5 rounded-full">{dashboardStats ? dashboardStats.pendingLogisticsRequests : 0} Action</span>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{dashboardStats ? dashboardStats.pendingLogisticsRequests : 0}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Pending Requests</span>
          </div>
        </div>

        {/* Active Shipments */}
        <div
          onClick={() => { setActiveView('orders'); setOrderFilter('Shipped'); }}
          className="card-claude p-4 cursor-pointer hover:border-primary/40 transition-all relative group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-primary/10 text-primary group-hover:scale-105 transition-transform">
              <Truck className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded-full">{dashboardStats ? dashboardStats.inTransit : 0} In Transit</span>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{dashboardStats ? dashboardStats.delivered : 0}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Delivered</span>
          </div>
        </div>

      </div>

      {/* Quick Actions & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">

        {/* Quick Actions (7 Cols) */}
        <div className="lg:col-span-7 card-claude p-5">
          <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider mb-3">Quick Actions</h3>
          <div className="flex flex-col gap-2.5">
            <button
              onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); setActiveView('products'); }}
              className="w-full text-left flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/30 hover:bg-muted/70 hover:border-border cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  <Plus className="w-4 h-4 group-hover:rotate-90 transition-transform" />
                </div>
                <div>
                  <span className="block text-xs font-semibold text-foreground">Add Product Specs</span>
                  <span className="block text-[11px] text-muted-foreground mt-0.5">Define HS codes and price models</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>

            <button
              onClick={() => { setActiveView('analysis'); setAnalysisSubView('select'); }}
              className="w-full text-left flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/30 hover:bg-muted/70 hover:border-border cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                  <Globe className="w-4 h-4 group-hover:scale-110 transition-transform" />
                </div>
                <div>
                  <span className="block text-xs font-semibold text-foreground">Start Market Intelligence Analysis</span>
                  <span className="block text-[11px] text-muted-foreground mt-0.5">Explore high margins and import complexity</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>

            <button
              onClick={() => setActiveView('orders')}
              className="w-full text-left flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/30 hover:bg-muted/70 hover:border-border cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 flex items-center justify-center shrink-0">
                  <ShoppingCart className="w-4 h-4" />
                </div>
                <div>
                  <span className="block text-xs font-semibold text-foreground">Inspect Orders</span>
                  <span className="block text-[11px] text-muted-foreground mt-0.5">Clear pending buyer bids and accept contract rates</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>

            <button
              onClick={() => { setActiveView('orders'); setOrderFilter('All'); }}
              className="w-full text-left flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/30 hover:bg-muted/70 hover:border-border cursor-pointer transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  <Truck className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
                </div>
                <div>
                  <span className="block text-xs font-semibold text-foreground">Track Shipment Status</span>
                  <span className="block text-[11px] text-muted-foreground mt-0.5">Customs updates and ETA timings</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>
          </div>
        </div>

        {/* Recent Activity (5 Cols) */}
        <div className="lg:col-span-5 card-claude p-5 flex flex-col justify-between">
          <div>
            <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider mb-3">Recent Activity</h3>
            <div className="relative pl-5 space-y-3.5">
              <div className="absolute left-[5px] top-2 bottom-2 w-[1px] bg-border z-0"></div>
              {(() => {
                const activities = [];
                orders.slice(0, 5).forEach(o => {
                  const statusColors = { 'Pending': 'bg-amber-500', 'Accepted': 'bg-primary', 'In Transit': 'bg-primary', 'Shipped': 'bg-primary', 'Delivered': 'bg-emerald-500' };
                  activities.push({ id: `order-${o.id}`, title: `Export Order #${o.id} — ${o.status}`, desc: `${o.product} · ${o.qty} to ${o.country}`, time: o.date || 'Recently', color: statusColors[o.status] || 'bg-muted-foreground' });
                });
                products.slice(-3).reverse().forEach(p => {
                  activities.push({ id: `product-${p.id}`, title: 'Product added to catalog', desc: `${p.name} (HS ${p.hscode || 'pending'})`, time: 'Recently', color: 'bg-primary' });
                });
                const display = activities.slice(0, 4);
                if (display.length === 0) {
                  return (
                    <div className="py-6 text-center">
                      <span className="text-xs text-muted-foreground font-medium">No recent activity yet.</span>
                      <p className="text-[11px] text-muted-foreground mt-1">Create a product or start a market analysis to see activity here.</p>
                    </div>
                  );
                }
                return display.map(a => (
                  <div key={a.id} className="relative z-10">
                    <div className={`absolute -left-[19px] top-1.5 w-2.5 h-2.5 rounded-full border-2 border-card ${a.color}`}></div>
                    <div className="text-xs">
                      <span className="block font-medium text-foreground">{a.title}</span>
                      <span className="block text-muted-foreground text-[11px] mt-0.5">{a.desc}</span>
                      <span className="block text-[10px] text-muted-foreground/70 mt-0.5">{a.time}</span>
                    </div>
                  </div>
                ));
              })()}
            </div>
          </div>
          <button
            onClick={() => setActiveView('orders')}
            className="w-full text-center py-2 mt-4 text-xs font-medium text-primary hover:text-primary/80 transition-colors border border-border hover:bg-muted/40 rounded-lg cursor-pointer"
          >
            View All Activity
          </button>
        </div>

      </div>

      {/* Product Performance Overview */}
      <div className="card-claude p-5">
        <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider mb-3">Product Performance Overview</h3>
        {products.length > 0 ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {products.slice(0, 4).map(p => {
              const productOrders = orders.filter(o => o.product === p.name);
              const deliveredCount = productOrders.filter(o => o.status === 'Delivered').length;
              const totalOrders = productOrders.length;
              const index = totalOrders > 0 ? Math.min(99, Math.round((deliveredCount / totalOrders) * 100) + 50) : 0;
              const markets = [...new Set(productOrders.map(o => o.country).filter(Boolean))];
              const indexColor = index >= 70 ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20' : index >= 40 ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20' : 'bg-muted text-muted-foreground border border-border';
              return (
                <div key={p.id} className="p-3.5 rounded-lg border border-border bg-card text-center cursor-pointer hover:border-primary/40 transition-all" onClick={() => { setSelectedAnalysisProduct(p.name); setActiveView('analysis'); setAnalysisSubView('select'); }}>
                  <span className="block text-xs font-semibold text-foreground truncate">{p.name}</span>
                  <span className="block text-[11px] text-muted-foreground mt-0.5">{markets.length > 0 ? `${markets.length} market${markets.length > 1 ? 's' : ''}` : 'No orders yet'}</span>
                  <div className={`mt-2.5 flex items-center justify-center gap-1 ${indexColor} text-[10px] font-semibold py-0.5 px-2 rounded-md w-max mx-auto`}>
                    <span>{index > 0 ? `Index: ${index}%` : 'Analyze'}</span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="py-8 text-center">
            <span className="text-xs text-muted-foreground font-medium block">No products yet.</span>
            <p className="text-[11px] text-muted-foreground mt-1">Add your first product to see performance metrics here.</p>
            <button onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); setActiveView('products'); }} className="mt-3 px-3.5 py-1.5 text-xs font-medium text-primary-foreground bg-primary hover:bg-primary/90 rounded-lg cursor-pointer transition-all shadow-sm">Add Product</button>
          </div>
        )}
      </div>
    </div>
  );
}

