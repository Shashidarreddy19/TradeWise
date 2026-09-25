/**
 * Logistics.jsx — Logistics Partner Dashboard (Orchestrator).
 * Redesigned with Claude-inspired shadcn/ui design system.
 */

import React, { useState, useEffect } from 'react';
import { Bell, LogOut, ChevronDown, User as UserIcon, Truck, Package, Clock, ShieldCheck } from 'lucide-react';
import {
  ordersApi, shipmentsApi, dashboardApi, authApi, proposalApi,
  clearAuth, getUser, isAuthenticated,
} from '../services';
import {
  OverviewView, AssignedOrdersView, ShipmentsView, ProfileView,
} from './logistics/index';
import { formatShipmentStatus, userInitials } from './logistics/utils';
import ThemeToggle from '../components/ThemeToggle';

export default function Logistics({ onNavigate, onLogout }) {
  // ── Active view ───────────────────────────────────────────────────────────
  const [activeView, setActiveView] = useState('overview');

  // ── UI state ──────────────────────────────────────────────────────────────
  const [showProfileMenu, setShowProfileMenu]     = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [toasts, setToasts]                       = useState([]);
  const [notifications, setNotifications]         = useState(() => {
    const saved = localStorage.getItem('tradewise_logistics_notifs');
    return saved ? JSON.parse(saved) : [];
  });

  // ── Data state ────────────────────────────────────────────────────────────
  const [orders, setOrders]               = useState([]);
  const [shipments, setShipments]         = useState([]);
  const [myProposals, setMyProposals]     = useState([]);
  const [dashboardStats, setDashboardStats] = useState(null);
  const [loading, setLoading]             = useState(true);

  // ── Toast helper ──────────────────────────────────────────────────────────
  const addToast = (message, type = 'success') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 5000);
  };

  const pushNotification = (text) => {
    const n = { id: Date.now(), text, time: 'Just now', read: false };
    setNotifications(prev => {
      const updated = [n, ...prev].slice(0, 20);
      localStorage.setItem('tradewise_logistics_notifs', JSON.stringify(updated));
      return updated;
    });
  };

  // ── Auth guard + initial load ─────────────────────────────────────────────
  useEffect(() => {
    if (!isAuthenticated()) { onNavigate('/login'); return; }
    fetchAll();
    const handler = () => { setShowProfileMenu(false); setShowNotifications(false); };
    window.addEventListener('click', handler);
    return () => window.removeEventListener('click', handler);
  }, []);

  const fetchAll = async () => {
    setLoading(true);
    await Promise.allSettled([fetchOrders(), fetchShipments(), fetchProposals(), fetchDashboard()]);
    setLoading(false);
  };

  const fetchOrders = async () => {
    try {
      const res = await ordersApi.getPending();
      setOrders((res.data || []).map(o => ({
        id: o.id,
        product: o.productName,
        hscode: o.productHsCode || '',
        country: o.destinationCountryName,
        qty: `${o.quantity} units`,
        rawQuantity: o.quantity,
        value: `₹${Number(o.totalPrice || 0).toLocaleString('en-IN')}`,
        rawValue: o.totalPrice,
        status: o.status === 'PENDING_LOGISTICS' ? 'Pending' : 'Accepted',
        rawStatus: o.status,
        exporterName: o.exporterName || '',
        exporterCompany: o.exporterCompany || '',
        pickupLocation: o.pickupLocation || '',
        shippingRequirements: o.shippingRequirements || 'Sea Freight',
        specialInstructions: o.specialInstructions || '',
        productWeightPerUnit: o.productWeightPerUnit,
        createdAt: o.createdAt,
      })));
    } catch (err) {
      addToast(err.message || 'Failed to load orders', 'error');
    }
  };

  const fetchProposals = async () => {
    try {
      const res = await proposalApi.getMyProposals();
      setMyProposals(res.data || []);
    } catch {
      /* non-fatal */
    }
  };

  const fetchShipments = async () => {
    try {
      const res = await shipmentsApi.getAll();
      setShipments((res.data || []).map(s => ({
        id: s.id,
        orderId: s.orderId,
        exporter: s.exporterName || '',
        exporterCompany: s.exporterCompany || '',
        dest: s.orderDestinationCountry,
        product: s.orderProductName,
        qty: `${s.orderQuantity} units`,
        tracking: s.trackingNumber || '',
        status: formatShipmentStatus(s.shipmentStatus),
        rawStatus: s.shipmentStatus,
        eta: s.estimatedDelivery || 'TBD',
        origin: s.origin || 'Mumbai, India',
        destination: s.destination || '',
        cost: s.cost,
        currency: s.currency,
        services: s.services,
        pickupDate: s.pickupDate,
        trackingHistory: s.trackingHistory || [],
        createdAt: s.createdAt,
      })));
    } catch (err) {
      addToast(err.message || 'Failed to load shipments', 'error');
    }
  };

  const fetchDashboard = async () => {
    try {
      const res = await dashboardApi.getLogisticsDashboard();
      setDashboardStats(res.data);
    } catch { /* non-critical */ }
  };

  // ── Order handlers ────────────────────────────────────────────────────────
  const handleAccept = async (order) => {
    await ordersApi.accept(order.id);
    pushNotification(`Accepted shipment for Order #${order.id} — ${order.product} → ${order.country}`);
    addToast(`Order #${order.id} accepted. Shipment created!`, 'success');
    await fetchAll();
    setActiveView('shipments');
  };

  const handleReject = async (orderId) => {
    await ordersApi.reject(orderId);
    pushNotification(`Declined shipment assignment for Order #${orderId}`);
    addToast('Order rejected.', 'success');
    await fetchOrders();
  };

  // ── Auth ──────────────────────────────────────────────────────────────────
  const user = getUser();
  const handleLogout = () => {
    clearAuth();
    localStorage.removeItem('tradewise_logistics_notifs');
    if (onLogout) onLogout();
    else onNavigate('/');
  };

  // ── Navbar active style ───────────────────────────────────────────────────
  const navLink = (view) =>
    `transition-colors h-full px-1 flex items-center font-medium text-xs cursor-pointer relative ${
      activeView === view ? 'text-primary font-semibold' : 'text-muted-foreground hover:text-foreground'
    }`;
  const navUnderline = (view) =>
    activeView === view ? <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t-full"></div> : null;

  const unreadCount = notifications.filter(n => !n.read).length;
  const markAllRead = () => {
    const updated = notifications.map(n => ({ ...n, read: true }));
    setNotifications(updated);
    localStorage.setItem('tradewise_logistics_notifs', JSON.stringify(updated));
  };

  // ─────────────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col antialiased">

      {/* ── TOAST STACK ── */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3">
        {toasts.map(t => (
          <div key={t.id} className="flex items-center gap-3 px-4 py-3 rounded-lg border border-border bg-card/95 backdrop-blur-md shadow-lg animate-in fade-in slide-in-from-bottom-5 duration-200">
            <div className={`w-2 h-2 rounded-full shrink-0 ${t.type === 'success' ? 'bg-emerald-500' : t.type === 'error' ? 'bg-destructive' : 'bg-primary'}`}></div>
            <span className="text-xs font-medium text-foreground">{t.message}</span>
          </div>
        ))}
      </div>

      {/* ── NAVBAR ── */}
      <nav className="fixed top-0 left-0 right-0 h-16 bg-card/90 backdrop-blur-md border-b border-border z-30 flex items-center justify-between px-4 sm:px-6">
        {/* Logo */}
        <div className="flex items-center gap-2 cursor-pointer" onClick={() => onNavigate('/')}>
          <div className="w-8 h-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center text-primary font-bold text-base">
            T
          </div>
          <span className="text-base font-semibold text-foreground tracking-tight">TradeWise</span>
          <span className="text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded font-medium tracking-wide">
            Logistics
          </span>
        </div>

        {/* Nav Links */}
        <div className="hidden md:flex items-center gap-6 h-full text-xs">
          <button onClick={() => onNavigate('/')} className="text-muted-foreground hover:text-foreground transition-colors h-full px-1 cursor-pointer flex items-center font-medium">Home</button>
          <button onClick={() => setActiveView('overview')} className={navLink('overview')}>Overview{navUnderline('overview')}</button>
          <button onClick={() => setActiveView('assigned')} className={navLink('assigned')}>
            Assigned Orders
            {orders.filter(o => o.rawStatus === 'PENDING_LOGISTICS').length > 0 && (
              <span className="ml-1.5 bg-primary text-primary-foreground text-[10px] font-semibold px-1.5 py-0.5 rounded-full">
                {orders.filter(o => o.rawStatus === 'PENDING_LOGISTICS').length}
              </span>
            )}
            {navUnderline('assigned')}
          </button>
          <button onClick={() => setActiveView('shipments')} className={navLink('shipments')}>Shipments{navUnderline('shipments')}</button>
        </div>

        {/* Right Controls */}
        <div className="flex items-center gap-2.5 relative">
          <ThemeToggle />

          {/* Notifications */}
          <div className="relative">
            <button onClick={(e) => { e.stopPropagation(); setShowNotifications(!showNotifications); setShowProfileMenu(false); markAllRead(); }}
              className="p-2 text-muted-foreground hover:text-foreground rounded-lg hover:bg-muted/60 cursor-pointer relative transition-colors">
              <Bell className="w-4 h-4" />
              {unreadCount > 0 && (
                <span className="absolute top-1.5 right-1.5 w-3.5 h-3.5 bg-primary text-primary-foreground text-[9px] font-semibold rounded-full flex items-center justify-center">
                  {unreadCount}
                </span>
              )}
            </button>
            {showNotifications && (
              <div onClick={e => e.stopPropagation()} className="absolute right-0 mt-2 w-80 bg-popover border border-border rounded-xl p-3 shadow-xl z-50 animate-in fade-in slide-in-from-top-2 duration-150 text-popover-foreground">
                <span className="block text-xs font-semibold text-foreground uppercase tracking-wider border-b border-border pb-2 mb-2">Logistics Alerts</span>
                <div className="space-y-1.5 max-h-60 overflow-y-auto">
                  {notifications.length === 0 ? (
                    <span className="text-center text-xs text-muted-foreground py-4 block">No alerts yet.</span>
                  ) : notifications.map(n => (
                    <div key={n.id} className="text-xs text-foreground p-2 rounded-md border border-border/40 hover:bg-muted/40 transition-colors">
                      <span className="block leading-relaxed">{n.text}</span>
                      <span className="block text-[10px] text-muted-foreground mt-1">{n.time}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Profile Dropdown */}
          <div className="relative">
            <button onClick={(e) => { e.stopPropagation(); setShowProfileMenu(!showProfileMenu); setShowNotifications(false); }}
              className="flex items-center gap-1.5 cursor-pointer p-1 rounded-lg hover:bg-muted/60 transition-colors">
              <div className="w-7 h-7 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-xs font-medium">
                {userInitials(user?.name || '')}
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-muted-foreground hidden sm:block" />
            </button>
            {showProfileMenu && (
              <div onClick={e => e.stopPropagation()} className="absolute right-0 mt-2 w-52 bg-popover border border-border rounded-xl p-2 shadow-xl z-50 animate-in fade-in slide-in-from-top-2 duration-150 text-popover-foreground">
                <div className="px-3 py-2 border-b border-border mb-1">
                  <span className="block text-xs font-semibold text-foreground truncate">{user?.name || 'Logistics Partner'}</span>
                  <span className="block text-[10px] text-muted-foreground truncate mt-0.5">{user?.email || ''}</span>
                </div>
                <button onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-muted text-xs font-medium text-foreground cursor-pointer transition-colors">
                  <UserIcon className="w-3.5 h-3.5 text-muted-foreground" /> Logistics Profile
                </button>
                <button onClick={handleLogout}
                  className="w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-destructive/10 text-xs font-medium text-destructive cursor-pointer transition-colors mt-1 border-t border-border pt-2">
                  <LogOut className="w-3.5 h-3.5" /> Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* ── MAIN CONTENT ── */}
      <main className="flex-grow pt-24 pb-12 px-4 sm:px-6 max-w-7xl mx-auto w-full">
        {loading ? (
          <div className="flex items-center justify-center py-24">
            <div className="flex flex-col items-center gap-3">
              <div className="w-7 h-7 border-2 border-primary/20 border-t-primary rounded-full animate-spin"></div>
              <span className="text-xs text-muted-foreground">Loading logistics data...</span>
            </div>
          </div>
        ) : (
          <>
            {activeView === 'overview' && (
              <OverviewView
                orders={orders}
                shipments={shipments}
                myProposals={myProposals}
                dashboardStats={dashboardStats}
                setActiveView={setActiveView}
              />
            )}
            {activeView === 'assigned' && (
              <AssignedOrdersView
                orders={orders}
                myProposals={myProposals}
                onProposalSubmitted={fetchAll}
                onAccept={handleAccept}
                onReject={handleReject}
                addToast={addToast}
                fetchAll={fetchAll}
              />
            )}
            {activeView === 'shipments' && (
              <ShipmentsView
                shipments={shipments}
                onStatusUpdated={fetchAll}
                addToast={addToast}
              />
            )}
            {activeView === 'profile' && (
              <ProfileView addToast={addToast} />
            )}
          </>
        )}
      </main>
    </div>
  );
}

