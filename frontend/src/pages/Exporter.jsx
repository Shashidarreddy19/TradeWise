import React, { useState, useEffect } from 'react';
import { 
  Globe, Bell, User as UserIcon, Settings, LogOut, Briefcase, ShoppingCart, 
  Truck, ArrowUpRight, Search, Plus, Trash2, Edit3, Eye, X, Check, AlertTriangle, 
  ChevronDown, HelpCircle, Activity, TrendingUp, Sliders, DollarSign, Loader2,
  ArrowLeft, ArrowRight, Shield, Sparkles, FileText, MapPin
} from 'lucide-react';
import { productsApi, ordersApi, shipmentsApi, proposalApi, dashboardApi, marketApi, referenceApi, regulatoryApi, aiApi, intelligenceApi, clearAuth, getUser, isAuthenticated } from '../services';
import { OverviewView, ProductsView, AnalysisView, OrdersView, ProfileView, LogisticsPlannerView } from './exporter/index';
import ThemeToggle from '../components/ThemeToggle';

// Intelligence APIs (intelligenceApi) handle Cost Estimation, Incentives, Market Opportunity, Negotiation.

export default function Exporter({ onNavigate, onLogout }) {
  // Active View: 'overview', 'products', 'analysis', 'orders', 'logistics', 'profile'
  const [activeView, setActiveView] = useState('overview');
  
  // Validation errors state
  const [errors, setErrors] = useState({});
  
  // Navigation Profile Menu Dropdown
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  
  // Notifications Dropdown
  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState(() => {
    const saved = localStorage.getItem('tradewise_notifications');
    if (saved) return JSON.parse(saved);
    return [];
  });

  useEffect(() => {
    localStorage.setItem('tradewise_notifications', JSON.stringify(notifications));
  }, [notifications]);

  // Toast stack state
  const [toasts, setToasts] = useState([]);
  const [quotaExhausted, setQuotaExhausted] = useState(false);
  
  const addToast = (message, type = 'success') => {
    // Detect quota exhaustion and show persistent banner
    if (message?.toLowerCase().includes('quota') || message?.toLowerCase().includes('rate limit')) {
      setQuotaExhausted(true);
      setTimeout(() => setQuotaExhausted(false), 60000); // auto-hide after 60s
    }
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 5000);
  };

  // 1. Seed Products State
  const [products, setProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(true);

  // Product Filter & Search
  const [searchProduct, setSearchProduct] = useState('');
  const [filterCategory, setFilterCategory] = useState('All');

  // Add Product Drawer State
  const [showAddDrawer, setShowAddDrawer] = useState(false);
  const [newProdName, setNewProdName] = useState('');
  const [newProdHscode, setNewProdHscode] = useState('');
  const [newProdCategory, setNewProdCategory] = useState('Spices');
  const [newProdDesc, setNewProdDesc] = useState('');
  const [newProdPrice, setNewProdPrice] = useState('');
  const [newProdWeight, setNewProdWeight] = useState('');
  // Set when editing an existing catalog record; null when creating.
  const [editingProductId, setEditingProductId] = useState(null);
  const [newProdCurrency, setNewProdCurrency] = useState('INR');
  const [newProdOrigin, setNewProdOrigin] = useState('India');
  const [newProdMaterial, setNewProdMaterial] = useState('');
  const [newProdComposition, setNewProdComposition] = useState('');
  const [newProdFunction, setNewProdFunction] = useState('');
  const [newProdManufacturing, setNewProdManufacturing] = useState('');
  const [newProdPhysicalForm, setNewProdPhysicalForm] = useState('');
  const [newProdSpecifications, setNewProdSpecifications] = useState('');
  const [drawerLoading, setDrawerLoading] = useState(false);

  // HS Code suggestion state
  const [hsCodeSuggestions, setHsCodeSuggestions] = useState([]);
  const [hsCodeLoading, setHsCodeLoading] = useState(false);
  const [hsCodeDisclaimer, setHsCodeDisclaimer] = useState('');

  const handleSuggestHsCode = async () => {
    if (!newProdName.trim()) {
      addToast('Enter a product name first to get HS code suggestions.', 'error');
      return;
    }
    
    // Auto-synthesize description if not typed yet
    const effectiveDesc = newProdDesc.trim() || [
      newProdName,
      newProdCategory,
      newProdMaterial ? `Material: ${newProdMaterial}` : '',
      newProdComposition ? `Composition: ${newProdComposition}` : '',
      newProdFunction ? `Function: ${newProdFunction}` : '',
      newProdPhysicalForm ? `Form: ${newProdPhysicalForm}` : '',
      newProdManufacturing ? `Process: ${newProdManufacturing}` : ''
    ].filter(Boolean).join('. ');

    setHsCodeLoading(true);
    setHsCodeSuggestions([]);
    setHsCodeDisclaimer('');

    try {
      const res = await intelligenceApi.classifyHsCode({
        productName: newProdName,
        category: newProdCategory,
        description: effectiveDesc,
        material: newProdMaterial || undefined,
        composition: newProdComposition || undefined,
        function: newProdFunction || undefined,
        manufacturingProcess: newProdManufacturing || undefined,
        physicalForm: newProdPhysicalForm || undefined,
        specifications: newProdSpecifications || undefined,
      });

      // Backend returns topCandidates with hsCode, officialDescription, matchScore, chapter, heading
      const candidates = (res.topCandidates || []).map(h => ({
        hs_code: h.hsCode,
        description: h.officialDescription,
        chapter: h.chapter,
        heading: h.heading,
        similarity_score: (h.matchScore || 80) / 100,
        reason: h.reason,
      }));

      const disclaimer = res.needsReview
        ? 'Low confidence — verify with customs broker before filing.'
        : `Classification: ${res.confidenceLevel || 'MATCHED'} (${res.classificationMode || 'DATABASE'})`;

      if (candidates.length > 0) {
        setHsCodeSuggestions(candidates);
        setHsCodeDisclaimer(disclaimer);
        if (candidates[0]?.hs_code) {
          setNewProdHscode(candidates[0].hs_code);
          if (errors.hscode) setErrors(prev => ({ ...prev, hscode: null }));
        }
        addToast(`Found ${candidates.length} HS code suggestions. Top match selected!`, 'success');
      } else if (res.recommendedHsCode) {
        setHsCodeSuggestions([{
          hs_code: res.recommendedHsCode,
          description: res.classificationExplanation || 'AI-recommended code',
          similarity_score: (res.confidenceScore || 80) / 100,
        }]);
        setNewProdHscode(res.recommendedHsCode);
        if (errors.hscode) setErrors(prev => ({ ...prev, hscode: null }));
        setHsCodeDisclaimer(disclaimer);
        addToast(`HS Code ${res.recommendedHsCode} assigned!`, 'success');
      } else {
        // Fast fallback to candidate lookup
        try {
          const quickRes = await intelligenceApi.getHsCandidates(newProdName, newProdCategory, newProdMaterial);
          if (quickRes?.topCandidates?.length > 0) {
            const qc = quickRes.topCandidates.map(h => ({
              hs_code: h.hsCode,
              description: h.officialDescription,
              chapter: h.chapter,
              heading: h.heading,
              similarity_score: (h.matchScore || 70) / 100,
              reason: h.reason
            }));
            setHsCodeSuggestions(qc);
            setNewProdHscode(qc[0].hs_code);
            addToast(`Selected HS Code ${qc[0].hs_code} from trade database.`, 'success');
          } else {
            addToast('No automatic match found. You can enter the HS code directly.', 'info');
          }
        } catch {
          addToast('You can enter the HS code manually in the box.', 'info');
        }
      }
    } catch (err) {
      // Graceful fallback on network/timeout
      try {
        const quickRes = await intelligenceApi.getHsCandidates(newProdName, newProdCategory, newProdMaterial);
        if (quickRes?.topCandidates?.length > 0) {
          const qc = quickRes.topCandidates.map(h => ({
            hs_code: h.hsCode,
            description: h.officialDescription,
            chapter: h.chapter,
            heading: h.heading,
            similarity_score: (h.matchScore || 70) / 100,
            reason: h.reason
          }));
          setHsCodeSuggestions(qc);
          setNewProdHscode(qc[0].hs_code);
          addToast(`Selected HS Code ${qc[0].hs_code} from trade database.`, 'success');
        } else {
          addToast('Please enter the HS code manually (e.g. 100630).', 'info');
        }
      } catch {
        addToast('Please enter the HS code manually in the input box.', 'info');
      }
    } finally {
      setHsCodeLoading(false);
    }
  };

  // 2. Seed Orders State
  const [orders, setOrders] = useState([]);
  const [loadingOrders, setLoadingOrders] = useState(true);

  const [selectedOrder, setSelectedOrder] = useState(null);
  const [orderFilter, setOrderFilter] = useState('All');

  // 3. Seed Shipments State
  const [shipments, setShipments] = useState([]);

  // Dashboard stats
  const [dashboardStats, setDashboardStats] = useState(null);

  // Available countries and categories from reference API
  const [countries, setCountries] = useState([]);
  const [categories, setCategories] = useState([]);

  // Fetch all data on mount
  useEffect(() => {
    if (!isAuthenticated()) {
      onNavigate('/login');
      return;
    }
    fetchProducts();
    fetchOrders();
    fetchShipments();
    fetchDashboard();
    fetchReferenceData();
  }, []);

  const fetchShipments = async () => {
    try {
      const res = await shipmentsApi.getExporterShipments();
      const mapped = (res.data || []).map(s => ({
        id: s.id,
        orderId: s.orderId,
        logistics: s.logisticsPartnerName || s.carrierName || s.logisticsPartnerCompany || s.carrierCompany || 'Assigned Carrier',
        logisticsCompany: s.logisticsPartnerCompany || s.carrierCompany,
        dest: s.orderDestinationCountry,
        product: s.orderProductName,
        qty: `${s.orderQuantity} units`,
        tracking: s.trackingNumber || '',
        status: s.shipmentStatus,
        rawStatus: s.shipmentStatus,
        eta: s.estimatedDelivery ? new Date(s.estimatedDelivery).toLocaleDateString('en-IN') : 'TBD',
        origin: s.origin || 'Mumbai, India',
        destination: s.destination || '',
        cost: s.cost,
        currency: s.currency,
        services: s.services,
        pickupDate: s.pickupDate,
        trackingHistory: s.trackingHistory || [],
        createdAt: s.createdAt,
      }));
      setShipments(mapped);
    } catch {
      // non-fatal
    }
  };

  const fetchProducts = async () => {
    try {
      setLoadingProducts(true);
      const res = await productsApi.getAll();
      const mapped = (res.data || []).map(p => ({
        id: p.id,
        name: p.name,
        hscode: p.hsCode,
        category: p.categoryName,
        categoryId: p.categoryId,
        price: parseFloat(p.price),
        unit: 'kg',
        currency: 'INR',
        origin: 'India',
        status: 'Active',
        weight: p.weight,
        description: p.description,
        quantity: p.quantity,
      }));
      setProducts(mapped);
      // Auto-select first product for market analysis if none selected
      if (mapped.length > 0 && !selectedAnalysisProduct) {
        setSelectedAnalysisProduct(mapped[0].name);
      }
    } catch (err) {
      addToast(err.message || 'Failed to load products', 'error');
    } finally {
      setLoadingProducts(false);
    }
  };

  const fetchOrders = async () => {
    try {
      setLoadingOrders(true);
      const res = await ordersApi.getMyOrders();
      const mapped = (res.data || []).map(o => ({
        id: o.id,
        product: o.productName,
        hscode: o.productHsCode,
        country: o.destinationCountryName,
        countryId: o.destinationCountryId,
        qty: `${o.quantity} units`,
        value: `${Number(o.totalPrice).toLocaleString('en-IN')}`,
        status: formatOrderStatus(o.status),
        date: o.createdAt ? new Date(o.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'long', year: 'numeric' }) : '',
        logisticsPartner: o.assignedLogisticsPartnerCompany || 'TBD',
        pickupLocation: o.pickupLocation,
        shippingRequirements: o.shippingRequirements,
        specialInstructions: o.specialInstructions,
        productId: o.productId,
        rawStatus: o.status,
      }));
      setOrders(mapped);
    } catch (err) {
      addToast(err.message || 'Failed to load orders', 'error');
    } finally {
      setLoadingOrders(false);
    }
  };

  const fetchDashboard = async () => {
    try {
      const res = await dashboardApi.getExporterDashboard();
      setDashboardStats(res.data);
    } catch (err) {
      // Non-critical, dashboard will show 0s
    }
  };

  const fetchReferenceData = async () => {
    try {
      const [countriesRes, categoriesRes] = await Promise.all([
        referenceApi.getCountries(),
        referenceApi.getCategories(),
      ]);
      setCountries(countriesRes.data || []);
      setCategories(categoriesRes.data || []);
    } catch (err) {
      // Non-critical
    }
  };

  const formatOrderStatus = (status) => {
    switch (status) {
      case 'PENDING_LOGISTICS': return 'Pending';
      case 'LOGISTICS_ACCEPTED': return 'Accepted';
      case 'IN_TRANSIT': return 'Shipped';
      case 'DELIVERED': return 'Delivered';
      case 'REJECTED': return 'Rejected';
      default: return status;
    }
  };

  const [selectedShipment, setSelectedShipment] = useState(null);
  const [assigningPartner, setAssigningPartner] = useState('');

  // 4. Market Analysis (managed by AnalysisView component)
  const [selectedAnalysisProduct, setSelectedAnalysisProduct] = useState('');
  const [selectedCountry, setSelectedCountry] = useState('Germany');
  const [analysisSubView, setAnalysisSubView] = useState('select');


  const validateProductForm = () => {
    const newErrors = {};
    if (!newProdName.trim()) {
      newErrors.name = 'Product name is required';
    }
    const cleanHs = (newProdHscode || '').replace(/[\s.]/g, '');
    if (!cleanHs) {
      newErrors.hscode = 'HS code is required (e.g., 100630). Enter manually or click Predict.';
    } else if (!/^\d{4,10}$/.test(cleanHs)) {
      newErrors.hscode = 'Please enter a valid numeric HS code (4 to 10 digits)';
    }
    if (!newProdPrice) {
      newErrors.price = 'Unit price is required';
    } else if (parseFloat(newProdPrice) <= 0 || isNaN(parseFloat(newProdPrice))) {
      newErrors.price = 'Price must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSaveProduct = async (e) => {
    if (e && e.preventDefault) e.preventDefault();
    if (!validateProductForm()) {
      addToast('Please complete required fields (Name, HS Code, Price).', 'error');
      return;
    }
    setDrawerLoading(true);
    try {
      // Resolve the category ID from the loaded category list or fallback safely
      const categoryObj = categories.find(c => 
        c.categoryName?.toLowerCase() === newProdCategory?.toLowerCase() ||
        c.name?.toLowerCase() === newProdCategory?.toLowerCase()
      );
      const categoryId = categoryObj ? categoryObj.id : (categories[0]?.id || 1);

      const cleanHs = (newProdHscode || '').replace(/[\s.]/g, '');
      const effectiveDesc = newProdDesc.trim() || [
        newProdName.trim(),
        newProdCategory,
        newProdMaterial ? `Material: ${newProdMaterial}` : '',
        newProdComposition ? `Composition: ${newProdComposition}` : '',
        newProdFunction ? `Function: ${newProdFunction}` : '',
        newProdPhysicalForm ? `Form: ${newProdPhysicalForm}` : '',
        newProdManufacturing ? `Process: ${newProdManufacturing}` : ''
      ].filter(Boolean).join('. ');

      const payload = {
        categoryId: categoryId,
        name: newProdName.trim(),
        hsCode: cleanHs || '100630',
        description: effectiveDesc,
        material: newProdMaterial || null,
        composition: newProdComposition || null,
        function: newProdFunction || null,
        manufacturingProcess: newProdManufacturing || null,
        physicalForm: newProdPhysicalForm || null,
        specifications: newProdSpecifications || null,
        price: parseFloat(newProdPrice),
        quantity: null,
        weight: newProdWeight ? parseFloat(newProdWeight) : 1.0,
      };

      if (editingProductId) {
        await productsApi.update(editingProductId, payload);
      } else {
        await productsApi.create(payload);
      }
      await fetchProducts(); // Refresh from server

      setShowAddDrawer(false);
      setNewProdName('');
      setNewProdHscode('');
      setNewProdDesc('');
      setNewProdPrice('');
      setNewProdWeight('');
      setNewProdMaterial('');
      setNewProdComposition('');
      setNewProdFunction('');
      setNewProdManufacturing('');
      setNewProdPhysicalForm('');
      setNewProdSpecifications('');
      setErrors({});
      addToast(editingProductId ? 'Product updated successfully.'
                                : 'Product saved successfully!', 'success');
      setEditingProductId(null);
    } catch (err) {
      addToast(err.message || 'Failed to save product', 'error');
    } finally {
      setDrawerLoading(false);
    }
  };

  const handleAnalyzeProduct = async () => {
    if (!validateProductForm()) {
      addToast('Please complete required fields (Name, HS Code, Price).', 'error');
      return;
    }
    setDrawerLoading(true);
    try {
      const categoryObj = categories.find(c => 
        c.categoryName?.toLowerCase() === newProdCategory?.toLowerCase() ||
        c.name?.toLowerCase() === newProdCategory?.toLowerCase()
      );
      const categoryId = categoryObj ? categoryObj.id : (categories[0]?.id || 1);

      const cleanHs = (newProdHscode || '').replace(/[\s.]/g, '');
      const effectiveDesc = newProdDesc.trim() || [
        newProdName.trim(),
        newProdCategory,
        newProdMaterial ? `Material: ${newProdMaterial}` : '',
        newProdComposition ? `Composition: ${newProdComposition}` : ''
      ].filter(Boolean).join('. ');

      const payload = {
        categoryId: categoryId,
        name: newProdName.trim(),
        hsCode: cleanHs || '100630',
        description: effectiveDesc,
        material: newProdMaterial || null,
        composition: newProdComposition || null,
        function: newProdFunction || null,
        manufacturingProcess: newProdManufacturing || null,
        physicalForm: newProdPhysicalForm || null,
        specifications: newProdSpecifications || null,
        price: parseFloat(newProdPrice),
        quantity: null,
        weight: newProdWeight ? parseFloat(newProdWeight) : 1.0,
      };

      if (editingProductId) {
        await productsApi.update(editingProductId, payload);
      } else {
        await productsApi.create(payload);
      }
      await fetchProducts();

      setShowAddDrawer(false);
      const savedProdName = newProdName;
      setNewProdName('');
      setNewProdHscode('');
      setNewProdDesc('');
      setNewProdPrice('');
      setNewProdWeight('');
      setNewProdMaterial('');
      setNewProdComposition('');
      setNewProdFunction('');
      setNewProdManufacturing('');
      setNewProdPhysicalForm('');
      setNewProdSpecifications('');
      setErrors({});
      setEditingProductId(null);

      // Navigate to Analysis view
      setSelectedAnalysisProduct(savedProdName);
      setAnalyzedProduct(null);
      setIsAnalyzing(true);
      setActiveView('analysis');

      setTimeout(() => {
        setIsAnalyzing(false);
        setAnalyzedProduct(savedProdName);
        addToast(`Product saved & ready for export market analysis!`, 'success');
      }, 1000);
    } catch (err) {
      addToast(err.message || 'Failed to save product', 'error');
    } finally {
      setDrawerLoading(false);
    }
  };

  const handleEditProduct = (p) => {
    // Load the record into the same drawer used for creation.
    setEditingProductId(p.id);
    setNewProdName(p.name || '');
    setNewProdHscode(p.hscode || '');
    setNewProdDesc(p.description || '');
    setNewProdPrice(p.price != null ? String(p.price) : '');
    setNewProdWeight(p.weight != null ? String(p.weight) : '');
    if (p.category) setNewProdCategory(p.category);
    setErrors({});
    setShowAddDrawer(true);
  };

  const handleDeleteProduct = async (id, name) => {
    try {
      await productsApi.delete(id);
      setProducts(prev => prev.filter(p => p.id !== id));
      addToast(`${name} deleted from catalog.`, 'success');
    } catch (err) {
      addToast(err.message || 'Failed to delete product', 'error');
    }
  };

  const handleAcceptOrder = (orderId) => {
    setOrders(prev => prev.map(o => o.id === orderId ? { ...o, status: 'Accepted' } : o));
    setSelectedOrder(null);
    addToast(`Order #${orderId} accepted!`, 'success');
  };

  const handleRejectOrder = (orderId) => {
    setOrders(prev => prev.map(o => o.id === orderId ? { ...o, status: 'Rejected' } : o));
    setSelectedOrder(null);
    addToast(`Order #${orderId} rejected.`, 'error');
  };

  const handleAssignLogistics = (orderId, partnerName) => {
    if (!partnerName) {
      addToast('Please select a logistics partner.', 'error');
      return;
    }
    
    // Update local orders state
    setOrders(prev => prev.map(o => o.id === orderId ? { ...o, status: 'Pending', logisticsPartner: partnerName } : o));
    
    // Also push updates to localStorage notifications for logistics dashboard
    const savedNotifs = localStorage.getItem('tradewise_notifications');
    const notifList = savedNotifs ? JSON.parse(savedNotifs) : [];
    const newNotif = {
      id: Date.now(),
      text: `New Shipment Assignment for Order #${orderId} from Trade Exporter Ltd.`,
      time: "Just now",
      type: "order",
      read: false
    };
    localStorage.setItem('tradewise_notifications', JSON.stringify([newNotif, ...notifList]));

    addToast(`Logistics partner '${partnerName}' assigned to Order #${orderId}.`, 'success');
    setSelectedOrder(null);
    setAssigningPartner('');
  };

  const handleLogout = () => {
    clearAuth();
    localStorage.removeItem('tradewise_notifications');
    addToast('Logging out...', 'success');
    setTimeout(() => {
      if (onLogout) onLogout();
      else onNavigate('/');
    }, 500);
  };

  // Close dropdowns on window click
  useEffect(() => {
    const handleOutsideClick = () => {
      setShowProfileMenu(false);
      setShowNotifications(false);
    };
    window.addEventListener('click', handleOutsideClick);
    return () => window.removeEventListener('click', handleOutsideClick);
  }, []);

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col antialiased font-sans relative">
      
      {/* Toast Notification Stack */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-2.5">
        {quotaExhausted && (
          <div className="flex items-center gap-3 px-4 py-3 rounded-xl border border-amber-500/30 bg-card backdrop-blur-md shadow-lg animate-scale-in max-w-sm">
            <span className="text-amber-500 text-sm font-bold">!</span>
            <div>
              <span className="text-xs font-bold text-amber-600 dark:text-amber-400 block">API Quota Notice</span>
              <span className="text-[10px] text-muted-foreground">Using cached data where available.</span>
            </div>
            <button onClick={() => setQuotaExhausted(false)} className="ml-auto text-muted-foreground hover:text-foreground cursor-pointer text-xs">×</button>
          </div>
        )}
        {toasts.map(t => (
          <div key={t.id} className={`flex items-center gap-2.5 px-4 py-3 rounded-lg border backdrop-blur-md shadow-md animate-scale-in text-xs font-semibold ${
            t.type === 'success' 
              ? 'bg-card border-emerald-500/30 text-emerald-600 dark:text-emerald-400' 
              : 'bg-card border-destructive/30 text-destructive'
          }`}>
            <div className={`w-2 h-2 rounded-full ${t.type === 'success' ? 'bg-emerald-500' : 'bg-destructive'}`}></div>
            <span>{t.message}</span>
          </div>
        ))}
      </div>

      {/* TOP NAVIGATION BAR */}
      <nav className="fixed top-0 left-0 right-0 h-14 bg-card/90 border-b border-border backdrop-blur-md shadow-xs z-30 flex items-center justify-between px-6">
        {/* Left Logo */}
        <div className="flex items-center gap-2 cursor-pointer select-none" onClick={() => onNavigate('/')}>
          <div className="w-7 h-7 rounded-lg bg-primary flex items-center justify-center text-primary-foreground font-bold text-xs shadow-xs">
            T
          </div>
          <span className="text-base font-bold text-foreground tracking-tight">Trade<span className="text-primary">Wise</span></span>
          <span className="badge-primary text-[9px] uppercase tracking-wider ml-1">Exporter</span>
        </div>

        {/* Center Links */}
        <div className="hidden md:flex items-center gap-6 h-full text-xs font-medium text-muted-foreground">
          <button 
            onClick={() => onNavigate('/')}
            className="hover:text-foreground transition-colors h-full px-1 cursor-pointer"
          >
            Home
          </button>
          
          <button 
            onClick={() => setActiveView('overview')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'overview' ? 'text-primary font-semibold' : 'hover:text-foreground'}`}
          >
            Overview
            {activeView === 'overview' && <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => setActiveView('products')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'products' ? 'text-primary font-semibold' : 'hover:text-foreground'}`}
          >
            Products
            {activeView === 'products' && <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => { setActiveView('analysis'); setAnalysisSubView('select'); }}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'analysis' ? 'text-primary font-semibold' : 'hover:text-foreground'}`}
          >
            Market Analysis
            {activeView === 'analysis' && <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => setActiveView('orders')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'orders' ? 'text-primary font-semibold' : 'hover:text-foreground'}`}
          >
            Orders & Fulfillment
            {activeView === 'orders' && <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => setActiveView('logistics-planner')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'logistics-planner' ? 'text-primary font-semibold' : 'hover:text-foreground'}`}
          >
            Logistics Planner
            {activeView === 'logistics-planner' && <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-primary rounded-t-full"></div>}
          </button>

        </div>

        {/* Right side controls */}
        <div className="flex items-center gap-3 relative">
          
          <ThemeToggle variant="simple" />

          {/* Notification Bell */}
          <div className="relative">
            <button 
              onClick={(e) => {
                e.stopPropagation();
                setShowNotifications(!showNotifications);
                setShowProfileMenu(false);
              }}
              className="p-2 text-muted-foreground hover:text-foreground rounded-lg hover:bg-muted cursor-pointer relative border border-border/60"
              aria-label="Notifications"
            >
              <Bell className="w-4 h-4" />
              {notifications.filter(n => !n.read).length > 0 && (
                <span className="absolute top-1 right-1 w-3.5 h-3.5 bg-primary text-primary-foreground text-[8px] font-bold rounded-full flex items-center justify-center">
                  {notifications.filter(n => !n.read).length}
                </span>
              )}
            </button>

            {/* Notification Dropdown Box */}
            {showNotifications && (
              <div 
                onClick={(e) => e.stopPropagation()} 
                className="absolute right-0 mt-2 w-80 bg-popover border border-border rounded-xl shadow-lg p-3 animate-scale-in z-50"
              >
                <div className="flex justify-between items-center border-b border-border pb-2 mb-2">
                  <span className="text-xs font-semibold text-foreground">Notifications</span>
                  <button 
                    onClick={() => {
                      setNotifications(notifications.map(n => ({ ...n, read: true })));
                      addToast('All notifications marked as read', 'success');
                    }}
                    className="text-[10px] font-semibold text-primary hover:underline"
                  >
                    Mark all read
                  </button>
                </div>
                
                <div className="space-y-1.5 max-h-60 overflow-y-auto">
                  {notifications.length > 0 ? (
                    notifications.map(n => (
                      <div key={n.id} className={`p-2 rounded-lg border text-xs transition-colors ${n.read ? 'border-border/40 bg-card' : 'border-primary/20 bg-accent/40'}`}>
                        <div className="flex justify-between items-start">
                          <span className="font-medium text-foreground text-[11px] leading-snug">{n.text}</span>
                          {!n.read && <div className="w-1.5 h-1.5 rounded-full bg-primary shrink-0 mt-1 ml-1"></div>}
                        </div>
                        <span className="text-[10px] text-muted-foreground mt-1 block">{n.time}</span>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-5 text-muted-foreground text-xs">No new notifications</div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Profile Dropdown avatar */}
          <div className="relative">
            <button 
              onClick={(e) => {
                e.stopPropagation();
                setShowProfileMenu(!showProfileMenu);
                setShowNotifications(false);
              }}
              className="flex items-center gap-1.5 px-2 py-1 rounded-lg border border-border bg-card hover:bg-muted transition-colors cursor-pointer"
            >
              <div className="w-6 h-6 rounded-full bg-primary/15 text-primary flex items-center justify-center text-[10px] font-bold">
                {(() => { const name = getUser()?.name || ''; const parts = name.split(' '); return parts.length >= 2 ? (parts[0][0] + parts[parts.length-1][0]).toUpperCase() : name.slice(0,2).toUpperCase() || 'TW'; })()}
              </div>
              <ChevronDown className="w-3 h-3 text-muted-foreground" />
            </button>

            {/* Profile Dropdown Box */}
            {showProfileMenu && (
              <div 
                onClick={(e) => e.stopPropagation()} 
                className="absolute right-0 mt-2 w-48 bg-popover border border-border rounded-xl shadow-lg p-2 animate-scale-in z-50"
              >
                <div className="p-2 border-b border-border mb-1">
                  <span className="block text-xs font-semibold text-foreground truncate">{getUser()?.name || 'Trade User'}</span>
                  <span className="block text-[10px] text-muted-foreground mt-0.5 truncate">{getUser()?.email || 'exporter@company.com'}</span>
                </div>
                
                <button 
                  onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full text-left flex items-center gap-2 p-1.5 rounded-md text-xs text-foreground hover:bg-muted cursor-pointer"
                >
                  <UserIcon className="w-3.5 h-3.5 text-muted-foreground" />
                  View Profile
                </button>

                <button 
                  onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full text-left flex items-center gap-2 p-1.5 rounded-md text-xs text-foreground hover:bg-muted cursor-pointer"
                >
                  <Settings className="w-3.5 h-3.5 text-muted-foreground" />
                  Settings
                </button>

                <button 
                  onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full text-left flex items-center gap-2 p-1.5 rounded-md text-xs text-foreground hover:bg-muted cursor-pointer"
                >
                  <Briefcase className="w-3.5 h-3.5 text-muted-foreground" />
                  My Company
                </button>

                <div className="border-t border-border my-1"></div>

                <button 
                  onClick={handleLogout}
                  className="w-full text-left flex items-center gap-2 p-1.5 rounded-md text-xs font-medium text-destructive hover:bg-destructive/10 cursor-pointer"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Logout
                </button>
              </div>
            )}
          </div>

        </div>
      </nav>

      {/* VIEW RENDER DISPATCHER */}
      <div className="flex-grow pt-24 pb-12 px-6 max-w-7xl w-full mx-auto relative z-10">

        {/* VIEW: OVERVIEW / DASHBOARD */}
        {activeView === 'overview' && (
          <OverviewView
            products={products}
            orders={orders}
            dashboardStats={dashboardStats}
            setActiveView={setActiveView}
            setAnalysisSubView={setAnalysisSubView}
            setOrderFilter={setOrderFilter}
            setErrors={setErrors}
            setEditingProductId={setEditingProductId}
            setNewProdName={setNewProdName}
            setNewProdHscode={setNewProdHscode}
            setNewProdDesc={setNewProdDesc}
            setNewProdPrice={setNewProdPrice}
            setNewProdWeight={setNewProdWeight}
            setShowAddDrawer={setShowAddDrawer}
            setSelectedAnalysisProduct={setSelectedAnalysisProduct}
          />
        )}
        {/* VIEW: PRODUCTS CATALOG */}
        {activeView === 'products' && (
          <ProductsView
            products={products}
            searchProduct={searchProduct}
            setSearchProduct={setSearchProduct}
            filterCategory={filterCategory}
            setFilterCategory={setFilterCategory}
            setErrors={setErrors}
            setEditingProductId={setEditingProductId}
            setNewProdName={setNewProdName}
            setNewProdHscode={setNewProdHscode}
            setNewProdDesc={setNewProdDesc}
            setNewProdPrice={setNewProdPrice}
            setNewProdWeight={setNewProdWeight}
            setShowAddDrawer={setShowAddDrawer}
            setSelectedAnalysisProduct={setSelectedAnalysisProduct}
            setActiveView={setActiveView}
            setAnalysisSubView={setAnalysisSubView}
            handleEditProduct={handleEditProduct}
            handleDeleteProduct={handleDeleteProduct}
            addToast={addToast}
          />
        )}
        {/* VIEW: MARKET ANALYSIS */}
        {activeView === 'analysis' && (
          <AnalysisView
            products={products}
            countries={countries}
            categories={categories}
            addToast={addToast}
            setActiveView={setActiveView}
            fetchOrders={fetchOrders}
            fetchDashboard={fetchDashboard}
            selectedAnalysisProduct={selectedAnalysisProduct}
            setSelectedAnalysisProduct={setSelectedAnalysisProduct}
            selectedCountry={selectedCountry}
            setSelectedCountry={setSelectedCountry}
            analysisSubView={analysisSubView}
            setAnalysisSubView={setAnalysisSubView}
          />
        )}

        {/* VIEW: LOGISTICS PLANNER */}
        {activeView === 'logistics-planner' && (
          <LogisticsPlannerView
            addToast={addToast}
            products={products}
            countries={countries}
          />
        )}

        {activeView === 'orders' && (
          <OrdersView
            orders={orders}
            shipments={shipments}
            orderFilter={orderFilter}
            setOrderFilter={setOrderFilter}
            selectedOrder={selectedOrder}
            setSelectedOrder={setSelectedOrder}
            selectedShipment={selectedShipment}
            setSelectedShipment={setSelectedShipment}
            assigningPartner={assigningPartner}
            setAssigningPartner={setAssigningPartner}
            handleAcceptOrder={handleAcceptOrder}
            handleRejectOrder={handleRejectOrder}
            handleAssignLogistics={handleAssignLogistics}
            addToast={addToast}
            fetchOrders={fetchOrders}
            fetchShipments={fetchShipments}
          />
        )}

        {activeView === 'profile' && (
          <ProfileView user={getUser()} addToast={addToast} />
        )}

      </div>      {/* ADD PRODUCT DRAWER PANEL */}
      {showAddDrawer && (
        <div className="fixed inset-0 z-50 flex justify-end">
          {/* Drawer Backdrop blur */}
          <div 
            onClick={() => setShowAddDrawer(false)}
            className="absolute inset-0 bg-background/80 backdrop-blur-xs"
          ></div>

          {/* Drawer Content */}
          <div className="relative w-full max-w-md bg-card border-l border-border h-full shadow-2xl p-6 sm:p-7 flex flex-col justify-between overflow-hidden animate-scale-in">
            {/* Header */}
            <div className="flex justify-between items-center border-b border-border pb-4 mb-4 shrink-0">
              <div>
                <h2 className="text-base font-bold text-foreground tracking-tight">{editingProductId ? 'Edit Product' : 'Add New Product'}</h2>
                <span className="text-xs text-muted-foreground block mt-0.5">{editingProductId ? 'Update specifications for this catalog record' : 'Input product details for analysis'}</span>
              </div>
              <button 
                onClick={() => { setShowAddDrawer(false); setEditingProductId(null); }}
                className="p-1.5 hover:bg-muted rounded-lg cursor-pointer text-muted-foreground hover:text-foreground"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Scrollable Form Content */}
            <div className="flex-grow overflow-y-auto pr-1 space-y-3.5">
              {/* Inputs */}
              <form className="space-y-3.5" onSubmit={handleSaveProduct}>
                {/* Name */}
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Product Name</label>
                  <input 
                    type="text" 
                    value={newProdName}
                    onChange={(e) => {
                      setNewProdName(e.target.value);
                      if (errors.name) setErrors({ ...errors, name: null });
                    }}
                    placeholder="e.g., Organic Turmeric Powder"
                    className={`input-claude ${errors.name ? 'border-destructive' : ''}`}
                  />
                  {errors.name && (
                    <p className="text-[11px] text-destructive font-medium mt-1">{errors.name}</p>
                  )}
                </div>

                {/* Product Category */}
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Product Category</label>
                  <select 
                    value={newProdCategory}
                    onChange={(e) => setNewProdCategory(e.target.value)}
                    className="input-claude cursor-pointer"
                  >
                    {categories.length > 0 ? (
                      categories.map(cat => (
                        <option key={cat.id} value={cat.categoryName}>{cat.categoryName}</option>
                      ))
                    ) : (
                      <>
                        <option>Agricultural Products</option>
                        <option>Spices</option>
                        <option>Textiles</option>
                        <option>Food Products</option>
                        <option>Handicrafts</option>
                      </>
                    )}
                  </select>
                </div>

                {/* Product Description */}
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Product Description</label>
                  <textarea 
                    rows="3"
                    value={newProdDesc}
                    onChange={(e) => {
                      setNewProdDesc(e.target.value);
                      if (errors.description) setErrors({ ...errors, description: null });
                    }}
                    placeholder="Describe your product: material, quality, origin, specifications..."
                    className={`input-claude resize-none ${errors.description ? 'border-destructive' : ''}`}
                  />
                  {errors.description && (
                    <p className="text-[11px] text-destructive font-medium mt-1">{errors.description}</p>
                  )}
                </div>

              {/* Material */}
              <div className="space-y-1">
                <label className="text-xs font-medium text-foreground">Material</label>
                <input type="text" value={newProdMaterial} onChange={e => setNewProdMaterial(e.target.value)} placeholder="e.g., Cotton, Steel, Wood, Turmeric" className="input-claude"/>
              </div>

              {/* Composition + Manufacturing Process (side by side) */}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Composition</label>
                  <input type="text" value={newProdComposition} onChange={e => setNewProdComposition(e.target.value)} placeholder="e.g., 100% pure" className="input-claude"/>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Process</label>
                  <input type="text" value={newProdManufacturing} onChange={e => setNewProdManufacturing(e.target.value)} placeholder="e.g., Dried & Ground" className="input-claude"/>
                </div>
              </div>

              {/* Function + Physical Form (side by side) */}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Function / Use</label>
                  <input type="text" value={newProdFunction} onChange={e => setNewProdFunction(e.target.value)} placeholder="e.g., Spice, Culinary" className="input-claude"/>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Physical Form</label>
                  <input type="text" value={newProdPhysicalForm} onChange={e => setNewProdPhysicalForm(e.target.value)} placeholder="e.g., Powder, Liquid" className="input-claude"/>
                </div>
              </div>

                {/* HS Code */}
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">HS Code</label>
                  <div className="flex gap-2">
                    <input 
                      type="text" 
                      value={newProdHscode}
                      onChange={(e) => {
                        setNewProdHscode(e.target.value);
                        if (errors.hscode) setErrors({ ...errors, hscode: null });
                      }}
                      placeholder="Click Predict to auto-detect"
                      className={`input-claude font-mono ${errors.hscode ? 'border-destructive' : ''}`}
                    />
                    <button
                      type="button"
                      onClick={handleSuggestHsCode}
                      disabled={!newProdName.trim() || hsCodeLoading}
                      className="btn-primary text-xs py-2 px-3 whitespace-nowrap cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1.5"
                    >
                      {hsCodeLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Sparkles className="w-3.5 h-3.5" />}
                      <span>{hsCodeLoading ? 'Predicting...' : 'Predict'}</span>
                    </button>
                  </div>
                  {!newProdName.trim() ? (
                    <p className="text-[10px] text-muted-foreground mt-1">Enter a product name to auto-predict HS code or enter it manually.</p>
                  ) : (
                    <p className="text-[10px] text-muted-foreground mt-1">Tip: Click Predict to auto-detect HS code from official Indian tariff schedule.</p>
                  )}
                  {errors.hscode && (
                    <p className="text-[11px] text-destructive font-medium mt-1">{errors.hscode}</p>
                  )}
                  {/* HS Code Suggestions Dropdown */}
                  {hsCodeSuggestions.length > 0 && (
                    <div className="mt-2 border border-border bg-muted/40 rounded-xl p-2.5 space-y-1.5 max-h-40 overflow-y-auto">
                      <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block">Predictions (click to select)</span>
                      {hsCodeSuggestions.map((s, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setNewProdHscode(s.hs_code);
                            setHsCodeSuggestions([]);
                            if (errors.hscode) setErrors({ ...errors, hscode: null });
                            addToast(`HS Code ${s.hs_code} selected`, 'success');
                          }}
                          className="w-full text-left p-2 rounded-lg border border-border bg-card hover:border-primary/50 hover:bg-accent/50 transition-all cursor-pointer"
                        >
                          <div className="flex justify-between items-center">
                            <span className="font-mono text-xs font-bold text-foreground">{s.hs_code}</span>
                            <span className="badge-primary text-[9px]">{Math.round((s.similarity_score || 0) * 100)}% match</span>
                          </div>
                          <p className="text-[10px] text-muted-foreground mt-0.5 line-clamp-1">{s.description}</p>
                        </button>
                      ))}
                      {hsCodeDisclaimer && (
                        <p className="text-[9px] text-muted-foreground mt-1 italic">{hsCodeDisclaimer}</p>
                      )}
                    </div>
                  )}
                </div>

                {/* Unit weight */}
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Unit Weight (kg)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.001"
                    value={newProdWeight}
                    onChange={(e) => setNewProdWeight(e.target.value)}
                    placeholder="0.45"
                    className="input-claude"
                  />
                  <p className="text-[10px] text-muted-foreground">Used to calculate freight and landed cost per unit.</p>
                </div>

                {/* Pricing Row */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-foreground">Unit Price</label>
                    <input 
                      type="number" 
                      value={newProdPrice}
                      onChange={(e) => {
                        setNewProdPrice(e.target.value);
                        if (errors.price) setErrors({ ...errors, price: null });
                      }}
                      placeholder="350"
                      className={`input-claude ${errors.price ? 'border-destructive' : ''}`}
                    />
                    {errors.price && (
                      <p className="text-[11px] text-destructive font-medium mt-1">{errors.price}</p>
                    )}
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-foreground">Currency</label>
                    <select 
                      value={newProdCurrency}
                      onChange={(e) => setNewProdCurrency(e.target.value)}
                      className="input-claude cursor-pointer"
                    >
                      <option>INR</option>
                      <option>USD</option>
                      <option>EUR</option>
                    </select>
                  </div>
                </div>

                {/* Origin */}
                <div className="space-y-1">
                  <label className="text-xs font-medium text-foreground">Country of Origin</label>
                  <input 
                    type="text" 
                    value="India" 
                    disabled 
                    className="input-claude bg-muted text-muted-foreground cursor-not-allowed"
                  />
                </div>
              </form>
            </div>

            {/* Actions footer */}
            <div className="space-y-2 pt-4 mt-4 border-t border-border shrink-0">
              <button 
                onClick={handleSaveProduct}
                disabled={drawerLoading}
                className="btn-primary w-full py-2.5 text-xs font-semibold cursor-pointer"
              >
                {drawerLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Save Product'}
              </button>
              <button 
                onClick={handleAnalyzeProduct}
                disabled={drawerLoading}
                className="btn-outline w-full py-2.5 text-xs font-semibold cursor-pointer"
              >
                {drawerLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Analyze Product'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}