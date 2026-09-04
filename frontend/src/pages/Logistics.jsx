/**
 * Logistics.jsx — Logistics Partner Dashboard (Orchestrator).
 *
 * Responsibilities:
 *  - Auth guard
 *  - Fetch all data from API on mount (pending orders, shipments, dashboard stats)
 *  - Hold shared state and handlers
 *  - Render navbar, toasts, and active sub-view component
 *
 * Sub-views (pages/logistics/):
 *  OverviewView, AssignedOrdersView, ShipmentsView, LogisticsPlannerView, ProfileView
 */

import React, { useState, useEffect } from 'react';
import { Bell, LogOut, ChevronDown, User as UserIcon } from 'lucide-react';
import {
  ordersApi, shipmentsApi, dashboardApi, authApi,
  clearAuth, getUser, isAuthenticated,
} from '../services';
import {
  OverviewView, AssignedOrdersView, ShipmentsView,
  LogisticsPlannerView, ProfileView,
} from './logistics/index';
import { formatShipmentStatus, userInitials } from './logistics/utils';

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
    await Promise.allSettled([fetchOrders(), fetchShipments(), fetchDashboard()]);
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
        value: `₹${Number(o.totalPrice || 0).toLocaleString('en-IN')}`,
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
    `transition-colors h-full px-1 cursor-pointer relative ${
      activeView === view ? 'text-sky-500 font-extrabold' : 'hover:text-slate-900'
    }`;
  const navUnderline = (view) =>
    activeView === view ? <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div> : null;

  const unreadCount = notifications.filter(n => !n.read).length;
  const markAllRead = () => {
    const updated = notifications.map(n => ({ ...n, read: true }));
    setNotifications(updated);
    localStorage.setItem('tradewise_logistics_notifs', JSON.stringify(updated));
  };

  // ─────────────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-700 flex flex-col antialiased font-sans">

      {/* ── TOAST STACK ── */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3">
        {toasts.map(t => (
          <div key={t.id} className="flex items-center gap-3 px-5 py-4 rounded-xl border border-slate-200 bg-white/95 backdrop-blur-md shadow-2xl animate-in fade-in slide-in-from-bottom-5 duration-300">
            <div className={`w-2 h-2 rounded-full shrink-0 ${t.type === 'success' ? 'bg-emerald-500' : t.type === 'error' ? 'bg-red-500' : 'bg-sky-500'}`}></div>
            <span className="text-xs font-bold text-slate-800">{t.message}</span>
          </div>
        ))}
      </div>

      {/* ── NAVBAR ── */}
      <nav className="fixed top-0 left-0 right-0 h-16 bg-white border-b border-slate-200/80 shadow-sm z-30 flex items-center justify-between px-6">
        {/* Logo */}
        <div className="flex items-center gap-2 cursor-pointer" onClick={() => onNavigate('/')}>
          <span className="text-lg font-black text-slate-900 tracking-tight">Trade</span>
          <span className="text-[9px] bg-sky-50 text-sky-600 px-1.5 py-0.5 rounded font-black uppercase tracking-wider">Logistics</span>
        </div>

        {/* Nav Links */}
        <div className="hidden md:flex items-center gap-6 h-full text-xs font-bold text-slate-500">
          <button onClick={() => onNavigate('/')} className="hover:text-slate-900 transition-colors h-full px-1 cursor-pointer">Home</button>
          <button onClick={() => setActiveView('overview')} className={navLink('overview')}>Overview{navUnderline('overview')}</button>
          <button onClick={() => setActiveView('assigned')} className={navLink('assigned')}>
            Assigned Orders
            {orders.filter(o => o.rawStatus === 'PENDING_LOGISTICS').length > 0 && (
              <span className="ml-1.5 bg-red-500 text-white text-[8px] font-black px-1.5 py-0.5 rounded-full">
                {orders.filter(o => o.rawStatus === 'PENDING_LOGISTICS').length}
              </span>
            )}
            {navUnderline('assigned')}
          </button>
          <button onClick={() => setActiveView('shipments')} className={navLink('shipments')}>Shipments{navUnderline('shipments')}</button>
          <button onClick={() => setActiveView('planner')} className={navLink('planner')}>Logistics Planner{navUnderline('planner')}</button>
        </div>

        {/* Right Controls */}
        <div className="flex items-center gap-3 relative">
          {/* Notifications */}
          <div className="relative">
            <button onClick={(e) => { e.stopPropagation(); setShowNotifications(!showNotifications); setShowProfileMenu(false); markAllRead(); }}
              className="p-2 text-slate-500 hover:text-slate-800 rounded-full hover:bg-slate-50 cursor-pointer relative">
              <Bell className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-red-500 text-white text-[9px] font-black rounded-full flex items-center justify-center animate-pulse">
                  {unreadCount}
                </span>
              )}
            </button>
            {showNotifications && (
              <div onClick={e => e.stopPropagation()} className="absolute right-0 mt-2 w-80 bg-white border border-slate-200 rounded-2xl p-4 shadow-2xl z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                <span className="block text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-2 mb-2.5">Logistics Alerts</span>
                <div className="space-y-2 max-h-60 overflow-y-auto">
                  {notifications.length === 0 ? (
                    <span className="text-center text-xxs text-slate-400 py-4 font-semibold block">No alerts yet.</span>
                  ) : notifications.map(n => (
                    <div key={n.id} className="text-xxs font-semibold text-slate-700 p-2 rounded-lg border border-slate-50 hover:bg-slate-50 transition-colors">
                      <span className="block leading-relaxed">{n.text}</span>
                      <span className="block text-[9px] text-slate-400 mt-1 font-bold">{n.time}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Profile Dropdown */}
          <div className="relative">
            <button onClick={(e) => { e.stopPropagation(); setShowProfileMenu(!showProfileMenu); setShowNotifications(false); }}
              className="flex items-center gap-1.5 cursor-pointer p-1 rounded-full hover:bg-slate-50">
              <div className="w-8 h-8 rounded-full bg-sky-500 text-white flex items-center justify-center text-[11px] font-extrabold shadow-sm">
                {userInitials(user?.name || '')}
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-slate-500 hidden sm:block" />
            </button>
            {showProfileMenu && (
              <div onClick={e => e.stopPropagation()} className="absolute right-0 mt-2 w-52 bg-white border border-slate-200 rounded-2xl p-3 shadow-2xl z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                <div className="px-3 py-2.5 border-b border-slate-100 mb-1.5">
                  <span className="block text-xs font-extrabold text-slate-900 truncate">{user?.name || 'Logistics Partner'}</span>
                  <span className="block text-[10px] text-slate-400 truncate mt-0.5">{user?.email || ''}</span>
                </div>
                <button onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-slate-50 text-xxs font-bold text-slate-600 cursor-pointer transition-colors">
                  <UserIcon className="w-4 h-4 text-slate-400" /> Logistics Profile
                </button>
                <button onClick={handleLogout}
                  className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-red-50 text-xxs font-bold text-red-500 cursor-pointer transition-colors mt-1 border-t border-slate-100 pt-2">
                  <LogOut className="w-4 h-4 text-red-400" /> Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* ── MAIN CONTENT ── */}
      <main className="flex-grow pt-24 pb-12 px-6 max-w-7xl mx-auto w-full">
        {loading ? (
          <div className="flex items-center justify-center py-24">
            <div className="flex flex-col items-center gap-3">
              <div className="w-8 h-8 border-4 border-sky-500/30 border-t-sky-500 rounded-full animate-spin"></div>
              <span className="text-xs font-bold text-slate-400">Loading logistics data...</span>
            </div>
          </div>
        ) : (
          <>
            {activeView === 'overview' && (
              <OverviewView
                orders={orders}
                shipments={shipments}
                dashboardStats={dashboardStats}
                setActiveView={setActiveView}
              />
            )}
            {activeView === 'assigned' && (
              <AssignedOrdersView
                orders={orders}
                onAccept={handleAccept}
                onReject={handleReject}
                addToast={addToast}
              />
            )}
            {activeView === 'shipments' && (
              <ShipmentsView
                shipments={shipments}
                onStatusUpdated={fetchAll}
                addToast={addToast}
              />
            )}
            {activeView === 'planner' && (
              <LogisticsPlannerView
                shipments={shipments}
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
