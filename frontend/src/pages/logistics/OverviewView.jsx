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
  const greeting = hour < 12 ? 'Good morning,' : hour < 17 ? 'Good afternoon,' : 'Good evening,';

  return (
    <div className="space-y-6 animate-in fade-in duration-200">

      {/* Greeting Banner */}
      <div className="card-claude bg-gradient-to-br from-card via-card to-accent/30 p-6 sm:p-7 rounded-2xl border border-border shadow-sm relative overflow-hidden flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="relative z-10">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary text-[10px] font-semibold tracking-wide mb-2">
            <span className="w-1.5 h-1.5 rounded-full bg-primary"></span>
            Verified Freight Carrier Network
          </div>
          <h1 className="text-xl sm:text-2xl font-bold text-foreground tracking-tight">{greeting} {user?.name || 'Logistics Partner'}</h1>
          <p className="text-xs text-muted-foreground mt-1 max-w-xl">
            Monitor incoming trade orders, submit competitive freight proposals, and streamline door-to-door delivery milestones.
          </p>
        </div>
        <div className="relative z-10 flex sm:flex-col gap-2">
          <button
            onClick={() => setActiveView('assigned')}
            className="btn-primary"
          >
            <Send className="w-3.5 h-3.5" />
            Explore Orders ({totalAvailable})
          </button>
          <button
            onClick={() => setActiveView('shipments')}
            className="btn-secondary"
          >
            <Truck className="w-3.5 h-3.5" />
            Manage Shipments
          </button>
        </div>
      </div>

      {/* 5-Column Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3.5">
        
        {/* Available Orders */}
        <div
          onClick={() => setActiveView('assigned')}
          className="card-claude p-4 cursor-pointer hover:border-primary/40 transition-all group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-primary/10 text-primary group-hover:scale-105 transition-transform">
              <Package className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded-full">Open Market</span>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{totalAvailable}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Available Orders</span>
          </div>
        </div>

        {/* Pending Quotes */}
        <div
          onClick={() => setActiveView('assigned')}
          className="card-claude p-4 cursor-pointer hover:border-primary/40 transition-all group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 group-hover:scale-105 transition-transform">
              <Clock className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-amber-700 dark:text-amber-300 bg-amber-500/10 px-2 py-0.5 rounded-full">In Review</span>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{pendingQuotes}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Pending Quotes</span>
          </div>
        </div>

        {/* Accepted Quotes */}
        <div
          className="card-claude p-4 hover:border-primary/40 transition-all group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 group-hover:scale-105 transition-transform">
              <CheckCircle2 className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-500/10 px-2 py-0.5 rounded-full">Won Quotes</span>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{acceptedQuotes}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Accepted Bids</span>
          </div>
        </div>

        {/* Active Shipments */}
        <div
          onClick={() => setActiveView('shipments')}
          className="card-claude p-4 cursor-pointer hover:border-primary/40 transition-all group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-primary/10 text-primary group-hover:scale-105 transition-transform">
              <Truck className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded-full">On Route</span>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{activeShipments}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Active Shipments</span>
          </div>
        </div>

        {/* Completed Deliveries */}
        <div
          className="card-claude p-4 hover:border-primary/40 transition-all group"
        >
          <div className="flex justify-between items-start">
            <div className="p-2 rounded-lg bg-purple-500/10 text-purple-600 dark:text-purple-400 group-hover:scale-105 transition-transform">
              <Check className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-purple-700 dark:text-purple-300 bg-purple-500/10 px-2 py-0.5 rounded-full">Fulfillment</span>
          </div>
          <div className="mt-3">
            <span className="block text-2xl font-bold text-foreground">{completedDeliveries}</span>
            <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider mt-0.5 block">Delivered Cargo</span>
          </div>
        </div>

      </div>

      {/* Main Split: Quick Actions + Recent Shipments */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">

        {/* Quick Actions & Workflow Guide (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="card-claude p-5">
            <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider mb-3">Operations Workflow</h3>
            <div className="flex flex-col gap-2.5">
              <button
                onClick={() => setActiveView('assigned')}
                className="w-full text-left flex items-center justify-between p-3 rounded-lg border border-border/60 bg-muted/30 hover:bg-muted/70 hover:border-border cursor-pointer transition-all group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-primary/10 text-primary group-hover:scale-105 transition-transform">
                    <FileText className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block text-xs font-semibold text-foreground">1. Bid on Export Orders</span>
                    <span className="block text-[11px] text-muted-foreground mt-0.5">{totalAvailable} orders ready for quotation</span>
                  </div>
                </div>
                <ArrowUpRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
              </button>

              <button
                onClick={() => setActiveView('shipments')}
                className="w-full text-left flex items-center justify-between p-3 rounded-lg border border-border/60 bg-muted/30 hover:bg-muted/70 hover:border-border cursor-pointer transition-all group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-primary/10 text-primary group-hover:scale-105 transition-transform">
                    <Truck className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block text-xs font-semibold text-foreground">2. Log Tracking Milestones</span>
                    <span className="block text-[11px] text-muted-foreground mt-0.5">{activeShipments} shipments awaiting update</span>
                  </div>
                </div>
                <ArrowUpRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
              </button>

              <button
                onClick={() => setActiveView('profile')}
                className="w-full text-left flex items-center justify-between p-3 rounded-lg border border-border/60 bg-muted/30 hover:bg-muted/70 hover:border-border cursor-pointer transition-all group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 group-hover:scale-105 transition-transform">
                    <Briefcase className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block text-xs font-semibold text-foreground">3. Carrier Profile & Fleet</span>
                    <span className="block text-[11px] text-muted-foreground mt-0.5">Manage certifications, coverage & account</span>
                  </div>
                </div>
                <ArrowUpRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
              </button>
            </div>
          </div>

          {/* Logistics Quality Assurance Box */}
          <div className="p-4 rounded-xl bg-accent/40 border border-border text-xs text-foreground space-y-1.5">
            <span className="font-semibold text-primary text-[11px] uppercase tracking-wider block">Service Level Agreement</span>
            <p className="text-[11px] text-muted-foreground leading-relaxed">
              Providing timely quotes within 24 hours of exporter order placement increases proposal acceptance rate by up to 64%.
            </p>
          </div>
        </div>

        {/* Live Shipments Table (7 cols) */}
        <div className="lg:col-span-7 card-claude p-5">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-semibold text-foreground uppercase tracking-wider">Active Operations & Tracking</h3>
            <button
              onClick={() => setActiveView('shipments')}
              className="text-xs font-medium text-primary hover:text-primary/80 cursor-pointer transition-colors"
            >
              View All ({shipments.length}) →
            </button>
          </div>

          {shipments.length === 0 ? (
            <div className="py-10 text-center bg-muted/30 rounded-xl border border-dashed border-border">
              <Truck className="w-7 h-7 text-muted-foreground mx-auto mb-2" />
              <span className="text-xs font-medium text-foreground block">No active shipments yet</span>
              <p className="text-[11px] text-muted-foreground mt-1 max-w-xs mx-auto">
                Submit proposals for pending export orders. When an exporter accepts your quote, the shipment will appear here automatically.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {shipments.slice(0, 5).map(s => (
                <div
                  key={s.id}
                  onClick={() => setActiveView('shipments')}
                  className="flex items-center justify-between p-3 rounded-lg border border-border bg-card hover:bg-muted/40 transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-3">
                    <span className="text-xl p-1 bg-muted rounded-md">{countryFlag(s.dest)}</span>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-[10px] font-semibold text-muted-foreground">#{s.id}</span>
                        <span className="text-xs font-semibold text-foreground">{s.product}</span>
                      </div>
                      <span className="block text-[11px] text-muted-foreground mt-0.5">
                        {s.dest} · {s.qty} · {s.tracking || 'Tracking ID pending'}
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full inline-block ${statusToBadgeClass(s.status)}`}>
                      {s.status}
                    </span>
                    <span className="block text-[10px] text-muted-foreground mt-1">ETA: {s.eta}</span>
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

