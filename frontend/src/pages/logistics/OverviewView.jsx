import React from 'react';
import { Briefcase, Truck, Check, Clock, ArrowUpRight, Package, FileText, Send, CheckCircle2 } from 'lucide-react';
import { getUser } from '../../services';
import { countryFlag, formatShipmentStatus, statusToBadgeClass } from './utils';

export default function OverviewView({ orders = [], shipments = [], myProposals = [], dashboardStats, setActiveView }) {
  const user = getUser();

  const totalAvailable = dashboardStats?.totalAvailableOrders ?? dashboardStats?.availableRequests ?? orders.length;
  const pendingQuotes = dashboardStats?.pendingQuotes ?? myProposals.filter(p => p.status === 'PENDING').length;
  const acceptedQuotes = dashboardStats?.acceptedQuotes ?? myProposals.filter(p => p.status === 'ACCEPTED').length;
  const activeShipments = dashboardStats?.activeShipments ?? shipments.filter(s => s.rawStatus !== 'DELIVERED').length;
  const completedDeliveries = dashboardStats?.completedDeliveries ?? shipments.filter(s => s.rawStatus === 'DELIVERED').length;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good Morning,' : hour < 17 ? 'Good Afternoon,' : 'Good Evening,';

  return (
    <div className="space-y-8 animate-in fade-in duration-300">

      {/* Greeting Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 p-6 sm:p-8 rounded-2xl text-white shadow-xl relative overflow-hidden">
        <div className="absolute -right-10 -bottom-10 w-48 h-48 bg-sky-500/10 rounded-full blur-2xl pointer-events-none"></div>
        <div className="relative z-10">
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-sky-500/20 text-sky-300 text-[10px] font-bold uppercase tracking-wider mb-2">
            <span className="w-1.5 h-1.5 rounded-full bg-sky-400 animate-pulse"></span>
            Verified Freight Carrier Network
          </div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight">{greeting} {user?.name || 'Logistics Partner'}</h1>
          <p className="text-xs sm:text-sm text-slate-300 mt-1 font-medium max-w-xl">
            Monitor incoming trade orders, submit competitive freight proposals, and streamline door-to-door delivery milestones.
          </p>
        </div>
        <div className="relative z-10 flex sm:flex-col gap-2">
          <button
            onClick={() => setActiveView('assigned')}
            className="px-4 py-2.5 bg-sky-500 hover:bg-sky-400 text-white text-xs font-bold rounded-xl shadow-lg shadow-sky-500/20 transition-all flex items-center justify-center gap-2 cursor-pointer"
          >
            <Send className="w-3.5 h-3.5" />
            Explore Orders ({totalAvailable})
          </button>
          <button
            onClick={() => setActiveView('shipments')}
            className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white text-xs font-semibold rounded-xl border border-white/10 transition-all flex items-center justify-center gap-2 cursor-pointer"
          >
            <Truck className="w-3.5 h-3.5" />
            Manage Shipments
          </button>
        </div>
      </div>

      {/* 5-Column Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        
        {/* Available Orders */}
        <div
          onClick={() => setActiveView('assigned')}
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group cursor-pointer hover:border-sky-300 hover:shadow-md transition-all"
        >
          <div className="absolute top-0 left-0 w-1.5 h-full bg-sky-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-sky-50 text-sky-600 group-hover:scale-110 transition-transform">
              <Package className="w-5 h-5" />
            </div>
            <span className="text-[9px] font-black text-sky-600 bg-sky-50 px-2 py-0.5 rounded-full uppercase">Open Market</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{totalAvailable}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Available Orders</span>
          </div>
        </div>

        {/* Pending Quotes */}
        <div
          onClick={() => setActiveView('assigned')}
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group cursor-pointer hover:border-amber-300 hover:shadow-md transition-all"
        >
          <div className="absolute top-0 left-0 w-1.5 h-full bg-amber-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-amber-50 text-amber-600 group-hover:scale-110 transition-transform">
              <Clock className="w-5 h-5" />
            </div>
            <span className="text-[9px] font-black text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full uppercase">In Review</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{pendingQuotes}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Pending Quotes</span>
          </div>
        </div>

        {/* Accepted Quotes */}
        <div
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group hover:border-emerald-300 hover:shadow-md transition-all"
        >
          <div className="absolute top-0 left-0 w-1.5 h-full bg-emerald-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600 group-hover:scale-110 transition-transform">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <span className="text-[9px] font-black text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full uppercase">Won Quotes</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{acceptedQuotes}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Accepted Bids</span>
          </div>
        </div>

        {/* Active Shipments */}
        <div
          onClick={() => setActiveView('shipments')}
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group cursor-pointer hover:border-indigo-300 hover:shadow-md transition-all"
        >
          <div className="absolute top-0 left-0 w-1.5 h-full bg-indigo-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 group-hover:scale-110 transition-transform">
              <Truck className="w-5 h-5" />
            </div>
            <span className="text-[9px] font-black text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-full uppercase">On Route</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{activeShipments}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Active Shipments</span>
          </div>
        </div>

        {/* Completed Deliveries */}
        <div
          className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group hover:border-purple-300 hover:shadow-md transition-all"
        >
          <div className="absolute top-0 left-0 w-1.5 h-full bg-purple-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-purple-50 text-purple-600 group-hover:scale-110 transition-transform">
              <Check className="w-5 h-5" />
            </div>
            <span className="text-[9px] font-black text-purple-600 bg-purple-50 px-2 py-0.5 rounded-full uppercase">Fulfillment</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{completedDeliveries}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Delivered Cargo</span>
          </div>
        </div>

      </div>

      {/* Main Split: Quick Actions + Recent Shipments */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

        {/* Quick Actions & Workflow Guide (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
            <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest mb-4">Operations Workflow</h3>
            <div className="flex flex-col gap-3">
              <button
                onClick={() => setActiveView('assigned')}
                className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-sky-50/50 hover:border-sky-200 cursor-pointer transition-all group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-sky-100 text-sky-600 group-hover:scale-105 transition-transform">
                    <FileText className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block text-xs font-bold text-slate-800">1. Bid on Export Orders</span>
                    <span className="block text-[10px] text-slate-400 mt-0.5">{totalAvailable} orders ready for quotation</span>
                  </div>
                </div>
                <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
              </button>

              <button
                onClick={() => setActiveView('shipments')}
                className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-indigo-50/50 hover:border-indigo-200 cursor-pointer transition-all group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-indigo-100 text-indigo-600 group-hover:scale-105 transition-transform">
                    <Truck className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block text-xs font-bold text-slate-800">2. Log Tracking Milestones</span>
                    <span className="block text-[10px] text-slate-400 mt-0.5">{activeShipments} shipments awaiting update</span>
                  </div>
                </div>
                <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
              </button>

              <button
                onClick={() => setActiveView('profile')}
                className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-emerald-50/50 hover:border-emerald-200 cursor-pointer transition-all group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-emerald-100 text-emerald-600 group-hover:scale-105 transition-transform">
                    <Briefcase className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block text-xs font-bold text-slate-800">3. Carrier Profile & Fleet</span>
                    <span className="block text-[10px] text-slate-400 mt-0.5">Manage certifications, service coverage & account</span>
                  </div>
                </div>
                <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
              </button>
            </div>
          </div>

          {/* Logistics Quality Assurance Box */}
          <div className="p-5 rounded-2xl bg-gradient-to-br from-indigo-50/80 to-sky-50/80 border border-indigo-100 text-xs text-slate-600 space-y-2">
            <span className="font-black text-indigo-900 text-xxs uppercase tracking-wider block">Service Level Agreement</span>
            <p className="text-[11px] leading-relaxed">
              Providing timely quotes within 24 hours of exporter order placement increases proposal acceptance rate by up to 64%.
            </p>
          </div>
        </div>

        {/* Live Shipments Table (7 cols) */}
        <div className="lg:col-span-7 bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest">Active Operations & Tracking</h3>
            <button
              onClick={() => setActiveView('shipments')}
              className="text-xs font-bold text-sky-600 hover:text-sky-500 cursor-pointer transition-colors"
            >
              View All ({shipments.length}) →
            </button>
          </div>

          {shipments.length === 0 ? (
            <div className="py-12 text-center bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
              <Truck className="w-8 h-8 text-slate-300 mx-auto mb-2" />
              <span className="text-xs font-bold text-slate-500 block">No active shipments yet</span>
              <p className="text-[10px] text-slate-400 mt-1 max-w-xs mx-auto">
                Submit proposals for pending export orders. When an exporter accepts your quote, the shipment will appear here automatically.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {shipments.slice(0, 5).map(s => (
                <div
                  key={s.id}
                  onClick={() => setActiveView('shipments')}
                  className="flex items-center justify-between p-3.5 rounded-xl border border-slate-100 hover:border-slate-200 hover:bg-slate-50/50 transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-3">
                    <span className="text-2xl p-1 bg-slate-50 rounded-lg">{countryFlag(s.dest)}</span>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-[10px] font-extrabold text-slate-400">#{s.id}</span>
                        <span className="text-xs font-bold text-slate-800">{s.product}</span>
                      </div>
                      <span className="block text-[10px] text-slate-400 mt-0.5">
                        {s.dest} · {s.qty} · {s.tracking || 'Tracking ID pending'}
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={`text-[9px] font-black px-2.5 py-0.5 rounded-full inline-block ${statusToBadgeClass(s.status)}`}>
                      {s.status}
                    </span>
                    <span className="block text-[10px] font-semibold text-slate-400 mt-1">ETA: {s.eta}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
