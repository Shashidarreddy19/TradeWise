import React from 'react';
import { Briefcase, Truck, Check, Clock, ArrowUpRight, Package } from 'lucide-react';
import { getUser } from '../../services';
import { countryFlag, formatShipmentStatus, statusToBadgeClass } from './utils';

export default function OverviewView({ orders, shipments, dashboardStats, setActiveView }) {
  const user = getUser();
  const pending     = dashboardStats?.availableRequests   ?? orders.length;
  const active      = dashboardStats?.activeShipments     ?? shipments.filter(s => s.rawStatus !== 'DELIVERED').length;
  const delivered   = dashboardStats?.completedDeliveries ?? shipments.filter(s => s.rawStatus === 'DELIVERED').length;
  const accepted    = dashboardStats?.acceptedShipments   ?? shipments.length;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good Morning,' : hour < 17 ? 'Good Afternoon,' : 'Good Evening,';

  return (
    <div className="space-y-8 animate-in fade-in duration-300">

      {/* Greeting */}
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">{greeting} {user?.name || 'Partner'}</h1>
        <p className="text-xs text-slate-500 mt-1 font-medium">Here is your logistics operations overview.</p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div onClick={() => setActiveView('assigned')} className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group cursor-pointer hover:border-sky-200 transition-all">
          <div className="absolute top-0 left-0 w-1 h-full bg-sky-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-sky-50 text-sky-600 group-hover:scale-110 transition-transform"><Briefcase className="w-5 h-5" /></div>
            <span className="text-[9px] font-black text-sky-600 bg-sky-50 px-2 py-0.5 rounded-full uppercase">Awaiting You</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{pending}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Available Requests</span>
          </div>
        </div>

        <div onClick={() => setActiveView('shipments')} className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group cursor-pointer hover:border-indigo-200 transition-all">
          <div className="absolute top-0 left-0 w-1 h-full bg-indigo-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 group-hover:scale-110 transition-transform"><Truck className="w-5 h-5" /></div>
            <span className="text-[9px] font-black text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-full uppercase">In Transit</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{active}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Active Shipments</span>
          </div>
        </div>

        <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-emerald-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600"><Check className="w-5 h-5" /></div>
            <span className="text-[9px] font-black text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full uppercase">Completed</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{delivered}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Delivered Shipments</span>
          </div>
        </div>

        <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1 h-full bg-amber-500"></div>
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-xl bg-amber-50 text-amber-600"><Clock className="w-5 h-5" /></div>
            <span className="text-[9px] font-black text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full uppercase">My Cargo</span>
          </div>
          <div className="mt-4">
            <span className="block text-2xl font-black text-slate-900">{accepted}</span>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Accepted (Total)</span>
          </div>
        </div>
      </div>

      {/* Quick Actions + Recent Shipments */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

        {/* Quick Actions */}
        <div className="lg:col-span-5 bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
          <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest mb-4">Quick Actions</h3>
          <div className="flex flex-col gap-3">
            <button onClick={() => setActiveView('assigned')} className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group">
              <div className="flex items-center gap-3">
                <Package className="w-5 h-5 text-sky-500" />
                <div>
                  <span className="block text-xs font-bold text-slate-850">Review Pending Assignments</span>
                  <span className="block text-[10px] text-slate-400 mt-0.5">{pending} request{pending !== 1 ? 's' : ''} waiting for your response</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>
            <button onClick={() => setActiveView('shipments')} className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group">
              <div className="flex items-center gap-3">
                <Truck className="w-5 h-5 text-indigo-500 group-hover:translate-x-1 transition-transform" />
                <div>
                  <span className="block text-xs font-bold text-slate-850">Update Shipment Milestones</span>
                  <span className="block text-[10px] text-slate-400 mt-0.5">{active} active shipment{active !== 1 ? 's' : ''} need status updates</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>
            <button onClick={() => setActiveView('planner')} className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group">
              <div className="flex items-center gap-3">
                <Check className="w-5 h-5 text-emerald-500" />
                <div>
                  <span className="block text-xs font-bold text-slate-850">Logistics Planner</span>
                  <span className="block text-[10px] text-slate-400 mt-0.5">Routes, documents &amp; freight cost estimator</span>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </button>
          </div>
        </div>

        {/* Recent Shipments */}
        <div className="lg:col-span-7 bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
          <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest mb-4">Recent Shipments</h3>
          {shipments.length === 0 ? (
            <div className="py-8 text-center">
              <span className="text-xxs text-slate-400 font-bold block">No shipments yet.</span>
              <p className="text-[10px] text-slate-400 mt-1">Accept a pending order to start your first shipment.</p>
            </div>
          ) : (
            <div className="space-y-2.5">
              {shipments.slice(0, 5).map(s => (
                <div key={s.id} className="flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:bg-slate-50/40 transition-colors">
                  <div className="flex items-center gap-3">
                    <span className="text-xl">{countryFlag(s.dest)}</span>
                    <div>
                      <span className="block text-xs font-bold text-slate-800">{s.product}</span>
                      <span className="block text-[10px] text-slate-400 mt-0.5">{s.dest} · {s.qty}</span>
                    </div>
                  </div>
                  <span className={`text-[9px] font-black px-2.5 py-0.5 rounded-full ${statusToBadgeClass(s.status)}`}>{s.status}</span>
                </div>
              ))}
              {shipments.length > 5 && (
                <button onClick={() => setActiveView('shipments')} className="w-full text-center py-2 text-xxs font-bold text-indigo-600 hover:underline cursor-pointer">View all {shipments.length} shipments →</button>
              )}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
