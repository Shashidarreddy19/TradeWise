import React, { useState, useEffect } from 'react';
import { 
  Bell, User as UserIcon, LogOut, Briefcase, ShoppingCart, 
  Truck, ArrowUpRight, Plus, Eye, X, Check, AlertTriangle, 
  ChevronDown, Activity, Loader2, ArrowLeft, ArrowRight,
  MapPin, Calendar, Mail, Phone, Shield
} from 'lucide-react';
import { ordersApi, shipmentsApi, dashboardApi, authApi, clearAuth, getUser, isAuthenticated } from '../services/api';

export default function Logistics({ onNavigate, onLogout }) {
  const [activeView, setActiveView] = useState('overview');
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  // Sync data with real API
  const [orders, setOrders] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [toasts, setToasts] = useState([]);
  const [loadingData, setLoadingData] = useState(true);
  const [dashboardStats, setDashboardStats] = useState(null);

  // Load from API on mount
  useEffect(() => {
    if (!isAuthenticated()) {
      onNavigate('/login');
      return;
    }
    fetchAllData();
  }, []);

  const fetchAllData = async () => {
    setLoadingData(true);
    await Promise.all([fetchPendingOrders(), fetchShipments(), fetchDashboard(), fetchProfile()]);
    setLoadingData(false);
  };

  const fetchPendingOrders = async () => {
    try {
      const res = await ordersApi.getPending();
      const mapped = (res.data || []).map(o => ({
        id: o.id,
        product: o.productName,
        hscode: o.productHsCode,
        country: o.destinationCountryName,
        qty: `${o.quantity} units`,
        value: `₹${Number(o.totalPrice).toLocaleString('en-IN')}`,
        status: o.status === 'PENDING_LOGISTICS' ? 'Pending' : 'Accepted',
        rawStatus: o.status,
        exporterName: o.exporterName,
        exporterCompany: o.exporterCompany,
        pickupLocation: o.pickupLocation,
        shippingRequirements: o.shippingRequirements || 'Sea Freight',
        specialInstructions: o.specialInstructions,
        productWeightPerUnit: o.productWeightPerUnit,
        createdAt: o.createdAt,
      }));
      setOrders(mapped);
    } catch (err) {
      addToast(err.message || 'Failed to load pending orders', 'error');
    }
  };

  const fetchShipments = async () => {
    try {
      const res = await shipmentsApi.getAll();
      const mapped = (res.data || []).map(s => ({
        id: s.id,
        orderId: s.orderId,
        exporter: s.exporterName,
        exporterCompany: s.exporterCompany,
        dest: s.orderDestinationCountry,
        product: s.orderProductName,
        qty: `${s.orderQuantity} units`,
        method: 'Sea Freight',
        tracking: s.trackingNumber,
        status: formatShipmentStatus(s.shipmentStatus),
        rawStatus: s.shipmentStatus,
        eta: s.estimatedDelivery || 'TBD',
        currentLoc: s.destination || '',
        origin: s.origin,
        createdAt: s.createdAt,
      }));
      setShipments(mapped);
    } catch (err) {
      addToast(err.message || 'Failed to load shipments', 'error');
    }
  };

  const fetchDashboard = async () => {
    try {
      const res = await dashboardApi.getLogisticsDashboard();
      setDashboardStats(res.data);
    } catch (err) {
      // Non-critical
    }
  };

  const formatShipmentStatus = (status) => {
    switch (status) {
      case 'ASSIGNED': return 'Shipment Accepted';
      case 'PICKED_UP': return 'Picked Up';
      case 'AT_EXPORT_CUSTOMS': return 'At Export Customs';
      case 'IN_TRANSIT': return 'In Transit';
      case 'AT_IMPORT_CUSTOMS': return 'At Import Customs';
      case 'OUT_FOR_DELIVERY': return 'Out for Delivery';
      case 'DELIVERED': return 'Delivered';
      default: return status;
    }
  };

  const shipmentStatusToEnum = (displayStatus) => {
    switch (displayStatus) {
      case 'Shipment Accepted': return 'ASSIGNED';
      case 'Picked Up': return 'PICKED_UP';
      case 'At Export Customs': return 'AT_EXPORT_CUSTOMS';
      case 'In Transit': return 'IN_TRANSIT';
      case 'At Import Customs': return 'AT_IMPORT_CUSTOMS';
      case 'Out for Delivery': return 'OUT_FOR_DELIVERY';
      case 'Delivered': return 'DELIVERED';
      default: return displayStatus;
    }
  };

  const addToast = (message, type = 'success') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 5000);
  };

  // Add Notification helper for Exporter/Logistics sync
  const addNotification = (text, type = 'shipment') => {
    const newNotif = {
      id: Date.now(),
      text,
      time: "Just now",
      type,
      read: false
    };
    setNotifications(prev => [newNotif, ...prev]);
  };

  // State hooks for modals & subviews
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedExporter, setSelectedExporter] = useState(null);
  const [selectedShipment, setSelectedShipment] = useState(null);
  const [trackingShipment, setTrackingShipment] = useState(null);
  const [statusToUpdate, setStatusToUpdate] = useState('');
  
  // Profile settings
  const [profileData, setProfileData] = useState(() => {
    const user = getUser();
    return {
      companyName: user?.companyName || "",
      contactPerson: user?.name || "",
      email: user?.email || "",
      phone: user?.phone || "",
      license: "",
      fleetSize: "",
      regions: ""
    };
  });

  // Load profile from API
  const fetchProfile = async () => {
    try {
      const res = await authApi.getProfile();
      const p = res.data;
      setProfileData({
        companyName: p.companyName || "",
        contactPerson: p.name || "",
        email: p.email || "",
        phone: p.phone || "",
        license: "",
        fleetSize: p.fleetSize || "",
        regions: p.serviceArea || ""
      });
    } catch (err) {
      // Non-critical, keep defaults from localStorage user
    }
  };

  const handleUpdateProfile = (e) => {
    e.preventDefault();
    localStorage.setItem('tradewise_logistics_profile', JSON.stringify(profileData));
    addToast("Logistics profile updated successfully.", "success");
  };

  // Logout handler
  const handleLogout = () => {
    clearAuth();
    if (onLogout) onLogout();
    else onNavigate('/');
  };

  // Accept Shipment Assignment
  const handleAcceptShipment = async (order) => {
    try {
      await ordersApi.accept(order.id);
      addNotification(`Accepted shipment assignment for Order #${order.id}.`, 'order');
      addToast(`Shipment for Order #${order.id} accepted. Moved to Shipments tracking.`, 'success');
      setSelectedOrder(null);
      // Refresh all data
      await fetchAllData();
      setActiveView('shipments');
    } catch (err) {
      addToast(err.message || 'Failed to accept shipment', 'error');
    }
  };

  // Reject Shipment Assignment
  const handleRejectShipment = async (orderId) => {
    try {
      await ordersApi.reject(orderId);
      addNotification(`Rejected shipment assignment for Order #${orderId}.`, 'order');
      addToast(`Shipment assignment #${orderId} was declined.`, 'info');
      setSelectedOrder(null);
      await fetchPendingOrders();
    } catch (err) {
      addToast(err.message || 'Failed to reject shipment', 'error');
    }
  };

  // Update Shipment Status
  const handleUpdateStatus = async (shipmentId) => {
    if (!statusToUpdate) return;

    try {
      const enumStatus = shipmentStatusToEnum(statusToUpdate);
      const res = await shipmentsApi.updateStatus(shipmentId, enumStatus);
      
      addNotification(`Shipment #${shipmentId} status updated to: ${statusToUpdate}`, 'shipment');
      addToast(`Shipment status updated to: ${statusToUpdate}`, 'success');

      // Refresh shipments and update the modal
      await fetchShipments();
      await fetchDashboard();

      // Update selected shipment state for modal
      const updated = shipments.find(s => s.id === shipmentId);
      if (updated) {
        setSelectedShipment({ ...updated, status: statusToUpdate });
      }
      setStatusToUpdate('');
    } catch (err) {
      addToast(err.message || 'Failed to update status', 'error');
    }
  };

  // Clears the unread badge locally; notification read-state is not persisted server-side.
  const handleMarkNotificationsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  // Summary counts for dashboard
  const activeShipmentsCount = dashboardStats ? dashboardStats.activeShipments : shipments.filter(s => s.status !== 'Delivered').length;
  const deliveredShipmentsCount = dashboardStats ? dashboardStats.completedDeliveries : shipments.filter(s => s.status === 'Delivered').length;
  const assignedOrdersCount = dashboardStats ? dashboardStats.availableRequests : orders.length;
  const acceptedShipmentsCount = dashboardStats ? dashboardStats.acceptedShipments : shipments.length;

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-700 flex flex-col antialiased font-sans relative">
      
      {/* Toast Notification Stack */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3">
        {toasts.map(t => (
          <div key={t.id} className="flex items-center gap-3 px-5 py-4 rounded-xl border border-slate-200 bg-white/95 backdrop-blur-md shadow-2xl animate-in fade-in slide-in-from-bottom-5 duration-300">
            <div className={`w-2 h-2 rounded-full ${t.type === 'success' ? 'bg-emerald-500' : t.type === 'info' ? 'bg-sky-500' : 'bg-red-500'}`}></div>
            <span className="text-xs font-bold text-slate-800">{t.message}</span>
          </div>
        ))}
      </div>

      {/* TOP NAVIGATION BAR */}
      <nav className="fixed top-0 left-0 right-0 h-16 bg-white border-b border-slate-200/80 backdrop-blur-md shadow-sm z-30 flex items-center justify-between px-6">
        {/* Left Logo */}
        <div className="flex items-center gap-2 cursor-pointer" onClick={() => onNavigate('/')}>
          <span className="text-lg font-black text-slate-900 tracking-tight">Trade</span>
          <span className="text-[9px] bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded font-black uppercase tracking-wider">Logistics</span>
        </div>

        {/* Center Links */}
        <div className="hidden md:flex items-center gap-6 h-full text-xs font-bold text-slate-500">
          <button 
            onClick={() => onNavigate('/')}
            className="hover:text-slate-900 transition-colors h-full px-1 cursor-pointer"
          >
            Home
          </button>
          
          <button 
            onClick={() => setActiveView('overview')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'overview' ? 'text-sky-655 font-extrabold' : 'hover:text-slate-950'}`}
          >
            Overview
            {activeView === 'overview' && <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => setActiveView('assigned')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'assigned' ? 'text-sky-655 font-extrabold' : 'hover:text-slate-950'}`}
          >
            Assigned Orders
            {activeView === 'assigned' && <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => setActiveView('shipments')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'shipments' ? 'text-sky-655 font-extrabold' : 'hover:text-slate-950'}`}
          >
            Shipments
            {activeView === 'shipments' && <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div>}
          </button>
        </div>

        {/* Right Controls */}
        <div className="flex items-center gap-4 relative">
          {/* Notifications */}
          <div className="relative">
            <button 
              onClick={(e) => {
                e.stopPropagation();
                setShowNotifications(!showNotifications);
                setShowProfileMenu(false);
                handleMarkNotificationsRead();
              }}
              className="p-2 text-slate-500 hover:text-slate-800 rounded-full hover:bg-slate-50 cursor-pointer relative"
            >
              <Bell className="w-5 h-5" />
              {notifications.filter(n => !n.read).length > 0 && (
                <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-red-500 text-white text-[9px] font-black rounded-full flex items-center justify-center animate-pulse">
                  {notifications.filter(n => !n.read).length}
                </span>
              )}
            </button>

            {showNotifications && (
              <div className="absolute right-0 mt-2.5 w-80 bg-white border border-slate-200 rounded-2xl p-4 shadow-2xl z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                <span className="block text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-2">Logistics Alerts</span>
                <div className="mt-2.5 space-y-3 max-h-60 overflow-y-auto no-scrollbar">
                  {notifications.length === 0 ? (
                    <span className="block text-center text-xxs text-slate-400 py-4 font-semibold">No notifications active.</span>
                  ) : (
                    notifications.map(n => (
                      <div key={n.id} className="text-xxs font-semibold text-slate-705 hover:bg-slate-50 p-2 rounded-lg border border-slate-50 transition-colors">
                        <span className="block leading-relaxed">{n.text}</span>
                        <span className="block text-[9px] text-slate-400 mt-1 font-bold">{n.time}</span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Profile Dropdown */}
          <div className="relative">
            <button 
              onClick={(e) => {
                e.stopPropagation();
                setShowProfileMenu(!showProfileMenu);
                setShowNotifications(false);
              }}
              className="flex items-center gap-2 cursor-pointer p-1 rounded-full hover:bg-slate-50"
            >
              <div className="w-8 h-8 rounded-full bg-sky-500 text-white flex items-center justify-center text-xs font-extrabold uppercase shadow-sm">
                FC
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-slate-500 hidden sm:block" />
            </button>

            {showProfileMenu && (
              <div className="absolute right-0 mt-2.5 w-52 bg-white border border-slate-200 rounded-2xl p-3 shadow-2xl z-50 animate-in fade-in slide-in-from-top-2 duration-200 flex flex-col gap-1 text-xxs font-bold text-slate-655">
                <div className="px-3.5 py-2.5 border-b border-slate-100 flex flex-col mb-1.5">
                  <span className="text-slate-900 font-extrabold truncate">{profileData.companyName}</span>
                  <span className="text-[10px] text-slate-400 truncate mt-0.5">{profileData.email}</span>
                </div>
                <button 
                  onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-slate-50 hover:text-slate-950 transition-colors text-left cursor-pointer"
                >
                  <UserIcon className="w-4 h-4 text-slate-400" />
                  <span>Logistics Profile</span>
                </button>
                <button 
                  onClick={handleLogout}
                  className="flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-red-50 text-red-500 transition-colors text-left cursor-pointer mt-1.5 border-t border-slate-50 pt-2"
                >
                  <LogOut className="w-4 h-4 text-red-400" />
                  <span>Logout</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* BODY CONTENT CONTAINER */}
      <main className="flex-grow pt-24 pb-12 px-6 max-w-7xl mx-auto w-full z-10">
        
        {/* VIEW: OVERVIEW */}
        {activeView === 'overview' && (
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
                {' '}{getUser()?.name || 'Partner'}
              </h1>
              <p className="text-xs text-slate-500 mt-1 font-medium">
                Here is your logistics operations overview.
              </p>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {/* Assigned Orders */}
              <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden group">
                <div className="absolute top-0 left-0 w-1 h-full bg-sky-500"></div>
                <div className="flex justify-between items-start">
                  <div className="p-2 rounded-xl bg-sky-55/10 text-sky-600">
                    <Briefcase className="w-5 h-5" />
                  </div>
                  <span className="text-[9px] font-black text-sky-600 bg-sky-50 px-2 py-0.5 rounded-full uppercase">Pending Pickup</span>
                </div>
                <div className="mt-4">
                  <span className="block text-2xl font-black text-slate-900">{assignedOrdersCount}</span>
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1 block">Assigned Orders</span>
                </div>
              </div>

              {/* Active Shipments */}
              <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden">
                <div className="absolute top-0 left-0 w-1 h-full bg-indigo-500"></div>
                <div className="flex justify-between items-start">
                  <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600">
                    <Truck className="w-5 h-5" />
                  </div>
                  <span className="text-[9px] font-black text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-full uppercase">In Transit</span>
                </div>
                <div className="mt-4">
                  <span className="block text-2xl font-black text-slate-900">{activeShipmentsCount}</span>
                  <span className="text-[10px] font-bold text-slate-455 uppercase tracking-widest mt-1 block">Active Shipments</span>
                </div>
              </div>

              {/* Delivered Shipments */}
              <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden">
                <div className="absolute top-0 left-0 w-1 h-full bg-emerald-500"></div>
                <div className="flex justify-between items-start">
                  <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600">
                    <Check className="w-5 h-5" />
                  </div>
                  <span className="text-[9px] font-black text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full uppercase">Completed</span>
                </div>
                <div className="mt-4">
                  <span className="block text-2xl font-black text-slate-900">{deliveredShipmentsCount}</span>
                  <span className="text-[10px] font-bold text-slate-450 uppercase tracking-widest mt-1 block">Delivered Shipments</span>
                </div>
              </div>

              {/* Pending Pickups */}
              <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm relative overflow-hidden">
                <div className="absolute top-0 left-0 w-1 h-full bg-amber-500"></div>
                <div className="flex justify-between items-start">
                  <div className="p-2 rounded-xl bg-amber-50 text-amber-600">
                    <Calendar className="w-5 h-5" />
                  </div>
                  <span className="text-[9px] font-black text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full uppercase">Ready</span>
                </div>
                <div className="mt-4">
                  <span className="block text-2xl font-black text-slate-900">{shipments.filter(s => s.rawStatus === 'ASSIGNED').length}</span>
                  <span className="text-[10px] font-bold text-slate-450 uppercase tracking-widest mt-1 block">Pending Pickups</span>
                </div>
              </div>
            </div>

            {/* Quick Actions & Recent Activity grid */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              {/* Quick Actions (5 Cols) */}
              <div className="lg:col-span-5 bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm space-y-4">
                <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest">Quick Actions</h3>
                <div className="flex flex-col gap-3">
                  <button 
                    onClick={() => setActiveView('assigned')}
                    className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group"
                  >
                    <div className="flex items-center gap-3">
                      <Briefcase className="w-5 h-5 text-sky-500" />
                      <div>
                        <span className="block text-xs font-bold text-slate-850">View Assigned Orders</span>
                        <span className="block text-[10px] text-slate-400 mt-0.5">Inspect incoming transport tasks</span>
                      </div>
                    </div>
                    <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                  </button>

                  <button 
                    onClick={() => setActiveView('shipments')}
                    className="w-full text-left flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 cursor-pointer transition-all group"
                  >
                    <div className="flex items-center gap-3">
                      <Activity className="w-5 h-5 text-indigo-500" />
                      <div>
                        <span className="block text-xs font-bold text-slate-850">Update Shipment Status</span>
                        <span className="block text-[10px] text-slate-400 mt-0.5">Set milestones for active routes</span>
                      </div>
                    </div>
                    <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                  </button>
                </div>
              </div>

              {/* Recent Activity: New Assignments (7 Cols) */}
              <div className="lg:col-span-7 bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm space-y-4">
                <h3 className="text-sm font-black text-slate-805 uppercase tracking-widest">Newly Assigned Export Orders</h3>
                <div className="divide-y divide-slate-100">
                  {orders.filter(o => o.status === 'Pending' || o.logisticsPartner === 'TBD').slice(0, 3).map(o => (
                    <div key={o.id} className="py-3.5 first:pt-0 last:pb-0 flex justify-between items-center text-xs">
                      <div>
                        <span className="block font-bold text-slate-850">Order #{o.id}</span>
                        <span className="block text-[10px] text-slate-450 mt-0.5">{o.product} · {o.qty} to {o.country}</span>
                      </div>
                      <button 
                        onClick={() => { setSelectedOrder(o); setActiveView('assigned'); }}
                        className="px-3.5 py-1.5 bg-sky-50 hover:bg-sky-100 text-sky-655 font-bold rounded-xl transition-all cursor-pointer"
                      >
                        Inspect
                      </button>
                    </div>
                  ))}
                  {orders.filter(o => o.status === 'Pending' || o.logisticsPartner === 'TBD').length === 0 && (
                    <div className="py-8 text-center text-xxs text-slate-400 font-bold">
                      No new shipment assignments currently active.
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* VIEW: ASSIGNED ORDERS */}
        {activeView === 'assigned' && (
          <div className="space-y-6 animate-in fade-in duration-300">
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Assigned Export Orders</h1>
              <p className="text-xs text-slate-500 mt-1 font-medium">Verify cargo routes, specifications, and accept assignments to trigger logistics pipelines.</p>
            </div>

            {/* Orders Table */}
            <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-450 uppercase tracking-widest">
                      <th className="py-4 px-5">Order ID</th>
                      <th className="py-4 px-5">Exporter</th>
                      <th className="py-4 px-5">Product</th>
                      <th className="py-4 px-5">Quantity</th>
                      <th className="py-4 px-5">Destination</th>
                      <th className="py-4 px-5">Shipping Mode</th>
                      <th className="py-4 px-5">Status</th>
                      <th className="py-4 px-5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
                    {orders
                      .filter(o => o.status === 'Pending' || o.logisticsPartner === 'TBD' || o.status === 'Accepted')
                      .map(o => (
                        <tr key={o.id} className="hover:bg-slate-50/40 transition-colors">
                          <td className="py-4 px-5 font-mono text-slate-900 font-bold">#{o.id}</td>
                          <td className="py-4 px-5">
                            <button
                              onClick={() => setSelectedExporter({
                                name: "Trade Exporter Ltd",
                                person: "Harini K.",
                                email: "harini@trade.com",
                                phone: "+91-98765-43210"
                              })}
                              className="font-bold text-sky-655 hover:underline text-left cursor-pointer"
                            >
                              Trade Exporter Ltd
                            </button>
                          </td>
                          <td className="py-4 px-5 text-slate-600">{o.product}</td>
                          <td className="py-4 px-5 text-slate-500">{o.qty}</td>
                          <td className="py-4 px-5">
                            <span className="mr-1.5">{o.country === 'Germany' ? '🇩🇪' : o.country === 'UAE' ? '🇦🇪' : '🇸🇬'}</span>
                            <span className="font-bold text-slate-800">{o.country}</span>
                          </td>
                          <td className="py-4 px-5 text-slate-500 capitalize">{o.shippingMode || 'Sea Freight'}</td>
                          <td className="py-4 px-5">
                            {o.status === 'Pending' || o.logisticsPartner === 'TBD' ? (
                              <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
                                <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
                                Pending Acceptance
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                                Accepted
                              </span>
                            )}
                          </td>
                          <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                            <button 
                              onClick={() => setSelectedOrder(o)}
                              className="px-3 py-1.5 text-[10px] font-bold text-slate-600 bg-slate-50 hover:bg-slate-100 rounded-lg cursor-pointer"
                            >
                              Details
                            </button>
                            {o.status !== 'Accepted' && (
                              <>
                                <button 
                                  onClick={() => handleAcceptShipment(o)}
                                  className="px-3 py-1.5 text-[10px] font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-lg cursor-pointer"
                                >
                                  Accept
                                </button>
                                <button 
                                  onClick={() => handleRejectShipment(o.id)}
                                  className="px-3 py-1.5 text-[10px] font-bold text-red-600 border border-red-100 hover:bg-red-50 rounded-lg cursor-pointer"
                                >
                                  Reject
                                </button>
                              </>
                            )}
                          </td>
                        </tr>
                      ))}
                    {orders.filter(o => o.status === 'Pending' || o.logisticsPartner === 'TBD' || o.status === 'Accepted').length === 0 && (
                      <tr>
                        <td colSpan="8" className="py-12 text-center text-xxs text-slate-400 font-bold uppercase tracking-wider">
                          No pending or accepted export order tasks.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* VIEW: SHIPMENTS */}
        {activeView === 'shipments' && (
          <div className="space-y-6 animate-in fade-in duration-300">
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Active & Completed Shipments</h1>
              <p className="text-xs text-slate-500 mt-1 font-medium">Verify customs documentation and update route tracking milestones.</p>
            </div>

            {/* Shipment Table */}
            <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-450 uppercase tracking-widest">
                      <th className="py-4 px-5">Shipment ID</th>
                      <th className="py-4 px-5">Order ID</th>
                      <th className="py-4 px-5">Exporter</th>
                      <th className="py-4 px-5">Destination</th>
                      <th className="py-4 px-5">Method</th>
                      <th className="py-4 px-5">Tracking Number</th>
                      <th className="py-4 px-5">Current Status</th>
                      <th className="py-4 px-5">ETA</th>
                      <th className="py-4 px-5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
                    {shipments.map(s => (
                      <tr key={s.id} className="hover:bg-slate-50/40 transition-colors">
                        <td className="py-4 px-5 font-mono text-slate-900 font-bold">#{s.id}</td>
                        <td className="py-4 px-5 font-mono text-slate-400">#{s.orderId}</td>
                        <td className="py-4 px-5 text-slate-805">{s.exporter}</td>
                        <td className="py-4 px-5">
                          <span className="mr-1.5">{s.dest === 'Germany' ? '🇩🇪' : s.dest === 'UAE' ? '🇦🇪' : '🇸🇬'}</span>
                          <span className="font-bold text-slate-800">{s.dest}</span>
                        </td>
                        <td className="py-4 px-5 text-slate-500">{s.method}</td>
                        <td className="py-4 px-5 font-mono text-slate-455">{s.tracking}</td>
                        <td className="py-4 px-5">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase tracking-wider ${
                            s.status === 'Delivered' 
                              ? 'bg-emerald-50 text-emerald-600'
                              : s.status === 'Shipment Accepted'
                              ? 'bg-sky-50 text-sky-600 border border-sky-100'
                              : 'bg-amber-50 text-amber-600 border border-amber-100 animate-pulse'
                          }`}>
                            {s.status}
                          </span>
                        </td>
                        <td className="py-4 px-5 text-slate-500">{s.eta}</td>
                        <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                          <button
                            onClick={() => { setSelectedShipment(s); setStatusToUpdate(s.status); }}
                            className="px-3.5 py-1.5 text-xxs font-black text-indigo-650 hover:bg-slate-50 border border-slate-100 hover:border-slate-200 rounded-xl transition-all cursor-pointer"
                          >
                            ▷ Manage
                          </button>
                          <button
                            onClick={() => { setTrackingShipment(s); }}
                            className="px-3.5 py-1.5 text-xxs font-black text-white bg-sky-500 hover:bg-sky-400 rounded-xl transition-all cursor-pointer"
                          >
                            Track
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* VIEW: PROFILE */}
        {activeView === 'profile' && (
          <div className="max-w-2xl mx-auto animate-in fade-in duration-300">
            <div className="bg-white border border-slate-200/80 p-6 sm:p-8 rounded-2xl shadow-sm space-y-6">
              <div className="flex items-center gap-4 border-b border-slate-100 pb-5">
                <div className="w-14 h-14 rounded-full bg-sky-50 flex items-center justify-center text-sky-500">
                  <Shield className="w-7 h-7" />
                </div>
                <div>
                  <h2 className="text-lg font-black text-slate-900 tracking-tight">Logistics Profile Settings</h2>
                  <p className="text-xxs text-slate-400 font-bold uppercase tracking-wider mt-0.5">Licensing, fleet configuration, and notifications credentials</p>
                </div>
              </div>

              <form onSubmit={handleUpdateProfile} className="space-y-4 text-xs font-semibold text-slate-655">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-[10px] font-black text-slate-450 uppercase tracking-widest">Company Name</label>
                    <input 
                      type="text" 
                      required
                      value={profileData.companyName}
                      onChange={(e) => setProfileData({ ...profileData, companyName: e.target.value })}
                      className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl focus:outline-none focus:border-sky-500"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-black text-slate-455 uppercase tracking-widest">Contact Operations Person</label>
                    <input 
                      type="text" 
                      required
                      value={profileData.contactPerson}
                      onChange={(e) => setProfileData({ ...profileData, contactPerson: e.target.value })}
                      className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl focus:outline-none focus:border-sky-500"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-black text-slate-455 uppercase tracking-widest">Operating Email</label>
                    <input 
                      type="email" 
                      required
                      value={profileData.email}
                      onChange={(e) => setProfileData({ ...profileData, email: e.target.value })}
                      className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl focus:outline-none focus:border-sky-500"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-black text-slate-455 uppercase tracking-widest">Contact Phone</label>
                    <input 
                      type="text" 
                      required
                      value={profileData.phone}
                      onChange={(e) => setProfileData({ ...profileData, phone: e.target.value })}
                      className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl focus:outline-none focus:border-sky-500"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-black text-slate-455 uppercase tracking-widest">Logistics License Number</label>
                    <input 
                      type="text" 
                      disabled
                      value={profileData.license}
                      className="w-full px-3.5 py-2.5 border border-slate-200 bg-slate-50/50 rounded-xl text-slate-400 cursor-not-allowed font-mono"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-black text-slate-455 uppercase tracking-widest">Active Transport Fleet Size</label>
                    <input 
                      type="text"
                      value={profileData.fleetSize}
                      onChange={(e) => setProfileData({ ...profileData, fleetSize: e.target.value })}
                      className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl focus:outline-none focus:border-sky-500"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full py-3 bg-sky-500 hover:bg-sky-400 text-white font-black text-xs uppercase tracking-widest rounded-xl transition-all shadow-md cursor-pointer"
                >
                  Update Profile
                </button>
              </form>
            </div>
          </div>
        )}

      </main>

      {/* EXPORTER CONTACT DETAIL MODAL */}
      {selectedExporter && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedExporter(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-sm bg-white border border-slate-200 rounded-2xl p-5 shadow-2xl animate-in scale-in duration-200 z-10 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-2">
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Exporter Contact</span>
              <button onClick={() => setSelectedExporter(null)} className="p-1 hover:bg-slate-50 rounded-full cursor-pointer"><X className="w-4 h-4 text-slate-400" /></button>
            </div>
            <div className="space-y-2 text-xs font-semibold text-slate-700">
              <span className="block font-black text-slate-900 text-sm">{selectedExporter.name}</span>
              <span className="block text-slate-555 font-bold">Contact: {selectedExporter.person}</span>
              <span className="block text-slate-450 font-mono">📧 {selectedExporter.email}</span>
              <span className="block text-slate-450 font-mono">📱 {selectedExporter.phone}</span>
            </div>
          </div>
        </div>
      )}

      {/* ASSIGNED ORDER DETAILS MODAL */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedOrder(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest">Order Assignment details</h3>
              <button onClick={() => setSelectedOrder(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer"><X className="w-4 h-4 text-slate-400" /></button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs font-semibold text-slate-655">
              {/* Exporter info */}
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 space-y-2">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block border-b border-slate-100 pb-1">Exporter Information</span>
                <span className="block text-slate-900 font-extrabold">Trade Exporter Ltd</span>
                <span className="block text-slate-500">Contact: Harini K.</span>
                <span className="block text-slate-450 font-mono">📧 harini@trade.com</span>
                <span className="block text-slate-450 font-mono">📱 +91-98765-43210</span>
              </div>

              {/* Cargo info */}
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 space-y-2">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block border-b border-slate-100 pb-1">Product Specifications</span>
                <span className="block text-slate-900 font-extrabold">{selectedOrder.product}</span>
                <span className="block text-slate-500 font-mono">HS Code: {selectedOrder.hscode || "0910.30"}</span>
                <span className="block text-slate-555">Quantity: {selectedOrder.qty}</span>
                <span className="block text-slate-450">Package weight: Est 1,020 kg (Gross)</span>
              </div>
            </div>

            {/* Shipping logistics info */}
            <div className="p-4 border border-slate-100 rounded-xl bg-slate-50/20 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs font-semibold text-slate-655">
              <div className="space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Pickup Address</span>
                <span className="block text-slate-700 leading-relaxed">Sector 4, JN Port Trust, Mumbai, Maharashtra</span>
              </div>
              <div className="space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Preferred Shipping Method</span>
                <span className="block text-slate-700 capitalize">{selectedOrder.shippingMode || "Sea Freight"}</span>
              </div>
              <div className="space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Destination Country</span>
                <span className="block text-slate-700">{selectedOrder.country}</span>
              </div>
              <div className="space-y-1">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Expected Pickup Date</span>
                <span className="block text-slate-700">22 July 2026</span>
              </div>
            </div>

            {/* Actions */}
            <div className="flex gap-2 border-t border-slate-100 pt-3 mt-3">
              {selectedOrder.status !== 'Accepted' ? (
                <>
                  <button 
                    onClick={() => handleAcceptShipment(selectedOrder)}
                    className="flex-grow py-3 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow transition-all cursor-pointer text-center"
                  >
                    Accept Shipment
                  </button>
                  <button 
                    onClick={() => handleRejectShipment(selectedOrder.id)}
                    className="px-4 py-3 text-xs font-bold text-red-600 border border-red-100 hover:bg-red-50 rounded-xl transition-all cursor-pointer text-center"
                  >
                    Reject Shipment
                  </button>
                </>
              ) : (
                <div className="w-full text-center py-2.5 bg-emerald-50 text-emerald-600 text-xs font-extrabold rounded-xl uppercase tracking-wider">
                  Shipment assignment locked
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* SHIPMENT MANAGEMENT MODAL (Timeline & updates status) */}
      {selectedShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setSelectedShipment(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest font-mono">Manage Cargo #{selectedShipment.id}</h3>
              <button onClick={() => setSelectedShipment(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer"><X className="w-4 h-4 text-slate-400" /></button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs font-semibold text-slate-655">
              <div className="space-y-1.5 p-3.5 border border-slate-100 rounded-xl bg-slate-50/20">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Cargo Specifications</span>
                <span className="block font-bold text-slate-900">{selectedShipment.product}</span>
                <span className="block text-slate-500">Qty: {selectedShipment.qty}</span>
                <span className="block text-slate-555 font-bold">Tracking: <span className="font-mono">{selectedShipment.tracking}</span></span>
              </div>
              <div className="space-y-1.5 p-3.5 border border-slate-100 rounded-xl bg-slate-50/20">
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Addresses</span>
                <span className="block"><span className="text-slate-400">Pickup:</span> Mumbai Hub</span>
                <span className="block"><span className="text-slate-400">Dest:</span> {selectedShipment.destAddress}</span>
              </div>
            </div>

            {/* Timeline progress mapping */}
            <div className="space-y-3 pt-2">
              <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Current Milestone Status</span>
              
              <div className="flex flex-col gap-2.5 max-h-44 overflow-y-auto pr-2">
                {[
                  'Shipment Accepted',
                  'Picked Up',
                  'At Export Customs',
                  'In Transit',
                  'At Import Customs',
                  'Out for Delivery',
                  'Delivered'
                ].map((step, idx) => {
                  const milestones = [
                    'Shipment Accepted',
                    'Picked Up',
                    'At Export Customs',
                    'In Transit',
                    'At Import Customs',
                    'Out for Delivery',
                    'Delivered'
                  ];
                  const currentIdx = milestones.indexOf(selectedShipment.status);
                  const isDone = idx <= currentIdx;
                  const isCurrent = idx === currentIdx;
                  
                  return (
                    <div key={step} className="flex items-start gap-3">
                      <div className="flex flex-col items-center">
                        <div className={`w-4 h-4 rounded-full flex items-center justify-center ${
                          isCurrent ? 'bg-sky-500 text-white ring-4 ring-sky-500/20' : isDone ? 'bg-emerald-500 text-white' : 'bg-slate-100 text-slate-400'
                        }`}>
                          {isDone ? <span className="text-[9px]">✓</span> : <span className="text-[8px]">{idx+1}</span>}
                        </div>
                        {idx < 6 && <div className={`w-0.5 h-6 ${idx < currentIdx ? 'bg-emerald-500' : 'bg-slate-200'}`}></div>}
                      </div>
                      <span className={`text-[11px] font-bold ${isCurrent ? 'text-sky-600 font-black' : isDone ? 'text-slate-800' : 'text-slate-400'}`}>
                        {step}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Dropdown status update controls */}
            {selectedShipment.status !== 'Delivered' && (
              <div className="border-t border-slate-100 pt-4 flex items-end gap-3.5">
                <div className="flex-grow space-y-1.5">
                  <label className="text-[9px] font-black text-slate-450 uppercase tracking-wider block">Update Milestone Status</label>
                  <select 
                    value={statusToUpdate}
                    onChange={(e) => setStatusToUpdate(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-indigo-500 cursor-pointer"
                  >
                    <option value="">Select Milestone...</option>
                    <option>Shipment Accepted</option>
                    <option>Picked Up</option>
                    <option>At Export Customs</option>
                    <option>In Transit</option>
                    <option>At Import Customs</option>
                    <option>Out for Delivery</option>
                    <option>Delivered</option>
                  </select>
                </div>
                <button
                  onClick={() => handleUpdateStatus(selectedShipment.id)}
                  className="px-4 py-2 bg-indigo-650 hover:bg-indigo-500 text-white text-xs font-bold rounded-xl transition-all cursor-pointer shadow-md shadow-indigo-600/10"
                >
                  Update Status
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TRACKING MODAL (Shipment tracking progress details) */}
      {trackingShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setTrackingShipment(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
          <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest font-mono">Live Tracking: {trackingShipment.tracking}</h3>
              <button onClick={() => setTrackingShipment(null)} className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer"><X className="w-4 h-4 text-slate-400" /></button>
            </div>

            {/* Info details */}
            <div className="grid grid-cols-2 gap-4 border border-slate-100 rounded-xl p-4 text-xs font-semibold text-slate-655 bg-slate-50/20">
              <div className="space-y-1.5">
                <span className="block text-[9px] font-black text-slate-400 uppercase">Current Location</span>
                <span className="block text-slate-900 font-extrabold">{trackingShipment.currentLoc}</span>
              </div>
              <div className="space-y-1.5">
                <span className="block text-[9px] font-black text-slate-400 uppercase">Estimated Delivery</span>
                <span className="block text-slate-900 font-extrabold">{trackingShipment.eta || "10 Days"}</span>
              </div>
              <div className="space-y-1.5">
                <span className="block text-[9px] font-black text-slate-400 uppercase">Milestone Status</span>
                <span className="block text-indigo-650 font-black">{trackingShipment.status}</span>
              </div>
              <div className="space-y-1.5">
                <span className="block text-[9px] font-black text-slate-400 uppercase">Last Updated</span>
                <span className="block text-slate-500">Just now</span>
              </div>
            </div>

            {/* Interactive Progress Bar */}
            <div className="space-y-4 pt-2">
              <span className="text-[9px] font-black text-slate-455 uppercase tracking-widest block">Cargo Route Milestones</span>
              
              <div className="relative">
                {/* Horizontal Progress Track line */}
                <div className="absolute top-3 left-3 right-3 h-1 bg-slate-150 rounded-full z-0"></div>
                {/* Visual complete fill line based on status */}
                {(() => {
                  const milestones = [
                    'Shipment Accepted',
                    'Picked Up',
                    'At Export Customs',
                    'In Transit',
                    'At Import Customs',
                    'Out for Delivery',
                    'Delivered'
                  ];
                  const currentIdx = milestones.indexOf(trackingShipment.status);
                  const percent = (currentIdx / 6) * 100;
                  return (
                    <div className="absolute top-3 left-3 h-1 bg-emerald-500 rounded-full transition-all duration-500 z-0" style={{ width: `calc(${percent}% - 24px)` }}></div>
                  );
                })()}

                {/* Milestones nodes */}
                <div className="flex justify-between relative z-10">
                  {[
                    { label: 'Accepted', code: 'Shipment Accepted' },
                    { label: 'Picked Up', code: 'Picked Up' },
                    { label: 'Export Customs', code: 'At Export Customs' },
                    { label: 'In Transit', code: 'In Transit' },
                    { label: 'Import Customs', code: 'At Import Customs' },
                    { label: 'Out for Del', code: 'Out for Delivery' },
                    { label: 'Delivered', code: 'Delivered' }
                  ].map((m, idx) => {
                    const milestones = [
                      'Shipment Accepted',
                      'Picked Up',
                      'At Export Customs',
                      'In Transit',
                      'At Import Customs',
                      'Out for Delivery',
                      'Delivered'
                    ];
                    const currentIdx = milestones.indexOf(trackingShipment.status);
                    const isDone = idx <= currentIdx;
                    const isCurrent = idx === currentIdx;
                    return (
                      <div key={idx} className="flex flex-col items-center text-center space-y-1">
                        <div className={`w-7 h-7 rounded-full flex items-center justify-center border-2 transition-all ${
                          isCurrent 
                            ? 'bg-white border-sky-500 text-sky-500 ring-4 ring-sky-500/15' 
                            : isDone 
                            ? 'bg-emerald-500 border-emerald-500 text-white' 
                            : 'bg-white border-slate-200 text-slate-400'
                        }`}>
                          <span className="text-[8px] font-black">{idx + 1}</span>
                        </div>
                        <span className={`text-[8px] max-w-[50px] font-bold block ${isCurrent ? 'text-sky-600 font-extrabold' : isDone ? 'text-slate-800' : 'text-slate-400'}`}>
                          {m.label}
                        </span>
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
