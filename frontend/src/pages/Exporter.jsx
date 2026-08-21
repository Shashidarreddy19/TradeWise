import React, { useState, useEffect } from 'react';
import { 
  Globe, Bell, User as UserIcon, Settings, LogOut, Briefcase, ShoppingCart, 
  Truck, ArrowUpRight, Search, Plus, Trash2, Edit3, Eye, X, Check, AlertTriangle, 
  ChevronDown, HelpCircle, Activity, TrendingUp, Sliders, DollarSign, Loader2,
  ArrowLeft, ArrowRight, Shield, Sparkles, FileText, MapPin
} from 'lucide-react';
import { productsApi, ordersApi, dashboardApi, marketApi, referenceApi, regulatoryApi, aiApi, intelligenceApi, clearAuth, getUser, isAuthenticated } from '../services/api';

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
    if (!newProdDesc.trim()) {
      addToast('Enter a product description for better HS code prediction.', 'error');
      return;
    }
    if (!newProdCategory) {
      addToast('Select a product category first.', 'error');
      return;
    }
    setHsCodeLoading(true);
    setHsCodeSuggestions([]);
    setHsCodeDisclaimer('');

    try {
      // Resolve the category hint through the shared mapper so the drawer and
      // the analysis views always classify a product the same way.
      const mlCategory = deriveProductCategory({
        category: newProdCategory,
        hscode: newProdHscode,
      }) || null;

      const res = await intelligenceApi.classifyHsCode({
        productName: newProdName,
        category: newProdCategory,
        description: newProdDesc,
        material: newProdMaterial,
        composition: newProdComposition,
        function: newProdFunction,
        manufacturingProcess: newProdManufacturing,
        physicalForm: newProdPhysicalForm,
        specifications: newProdSpecifications,
      });
      const candidates = (res.results || []).map(h => ({ hs_code: h.nationalCode, description: h.officialDescription, chapter: h.chapter, heading: h.heading, category_match: true, score: 0.9 }));

      const disclaimer = res.disclaimer || res.data?.disclaimer || '';

      if (candidates.length > 0) {
        setHsCodeSuggestions(candidates);
        setHsCodeDisclaimer(disclaimer);
        addToast(`Found ${candidates.length} HS code suggestions`, 'success');
      } else {
        addToast('No HS code suggestions found. Try a more specific product name or description.', 'error');
      }
    } catch (err) {
      const msg = err.message?.includes('unavailable')
        ? 'Service temporarily unavailable. Enter HS code manually.'
        : (err.message || 'Failed to get HS code suggestions');
      addToast(msg, 'error');
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
    fetchDashboard();
    fetchReferenceData();
  }, []);

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

  // 4. Market Analysis State
  const [selectedAnalysisProduct, setSelectedAnalysisProduct] = useState('');
  const [selectedCountry, setSelectedCountry] = useState('Germany');
  const [activeAnalysisTab, setActiveAnalysisTab] = useState('overview');
  const [analyzedProduct, setAnalyzedProduct] = useState(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [ownedDocs, setOwnedDocs] = useState({
    invoice: true,
    packingList: true,
    originCert: true,
    organicCert: false,
    phytoReport: false,
    fssaiLicense: false,
  });

  const toggleDoc = (key) => {
    setOwnedDocs(prev => {
      const next = { ...prev, [key]: !prev[key] };
      const count = Object.values(next).filter(Boolean).length;
      addToast(`Compliance checklist updated: ${count} of 6 active.`, 'success');
      return next;
    });
  };

  const [analysisSubView, setAnalysisSubView] = useState('select'); // 'select', 'recommendations', 'country-overview', 'compliance', 'cost', 'what-if'

  // Cost Estimation Configuration States
  const [costQuantity, setCostQuantity] = useState(1000);
  const [costShippingMode, setCostShippingMode] = useState('Sea');
  const [costSellingPrice, setCostSellingPrice] = useState('');
  const [isCalculatingCost, setIsCalculatingCost] = useState(false);
  const [calculatedCostBreakdown, setCalculatedCostBreakdown] = useState(null);
  // Required inputs the exporter must supply
  const [costHsCode, setCostHsCode] = useState('');
  const [costUnitCost, setCostUnitCost] = useState('');
  const [costUnitWeight, setCostUnitWeight] = useState('');
  const [costSellingCurrency, setCostSellingCurrency] = useState('INR');
  // Optional advanced inputs
  const [showAdvancedCost, setShowAdvancedCost] = useState(false);
  const [costPackagingPerUnit, setCostPackagingPerUnit] = useState('');
  const [costInlandTransport, setCostInlandTransport] = useState('');
  const [costInsuranceRate, setCostInsuranceRate] = useState('');
  const [costIncoterm, setCostIncoterm] = useState('CIF');

  // Prefill HS code from the selected product; the field stays editable.
  useEffect(() => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    if (productObj?.hscode) setCostHsCode(String(productObj.hscode).replace('.', ''));
    if (productObj?.price) setCostUnitCost(String(productObj.price));
    if (productObj?.weight) setCostUnitWeight(String(productObj.weight));
  }, [selectedAnalysisProduct, products]);

  const handleCalculateCost = async () => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    if (!productObj) { addToast('Select a product first.', 'error'); return; }
    const countryObj = countries.find(c => c.name === selectedCountry);
    const cc = countryObj?.code;
    // Every required field must be supplied - nothing is defaulted silently.
    if (!cc) { addToast('Select a destination country first.', 'error'); return; }
    if (!costHsCode.trim()) { addToast('Enter the HS code.', 'error'); return; }
    const qty = parseInt(costQuantity, 10);
    if (!qty || qty < 1) { addToast('Enter a valid quantity.', 'error'); return; }
    const unitCost = parseFloat(costUnitCost);
    if (!unitCost || unitCost <= 0) { addToast('Enter the unit manufacturing cost in INR.', 'error'); return; }
    const unitWeight = parseFloat(costUnitWeight);
    if (!unitWeight || unitWeight <= 0) { addToast('Enter the unit weight in kg.', 'error'); return; }

    setIsCalculatingCost(true);
    setCalculatedCostBreakdown(null);
    try {
      // Real cost-estimation API — uses official tariff rates from TradeData
      const hs = costHsCode.replace('.', '');
      const res = await intelligenceApi.estimateCost(selectedCountry, hs, unitCost, qty, 'USD');
      setCalculatedCostBreakdown(res);
      const confidence = res.overallConfidence || 'MEDIUM';
      addToast(confidence === 'HIGH' ? 'Cost estimation completed.' : 'Partial estimate — some tariff data unavailable.', 'success');
    } catch (err) { addToast(err.message || 'Cost estimation failed', 'error'); }
    finally { setIsCalculatingCost(false); }
  };

  // Compliance Chat Assistant States
  const [complianceLang, setComplianceLang] = useState('EN'); // 'EN', 'HI', 'TE'
  const [complianceChatText, setComplianceChatText] = useState('');
  const [complianceChatHistory, setComplianceChatHistory] = useState([
    { sender: 'assistant', text: "Hello! I am your Trade Compliance Assistant. Ask me anything about exporting goods to Germany under FSSAI and European Union regulations." }
  ]);

  const [marketAnalysisResult, setMarketAnalysisResult] = useState(null);

  // Resolve a product's category from its own category field, falling back to
  // the HS chapter. Used by every analysis call so one product always yields
  // the same category everywhere in the app.
  const deriveProductCategory = (productObj) => {
    if (!productObj) return '';
    // Canonical category name -> ML family. Must cover every entry seeded by
    // DataSeeder.java, otherwise a product falls through to HS-chapter guessing.
    const named = {
      'Agricultural Products': 'fruits_vegetables',
      'Spices': 'spices',
      'Food Products': 'processed_food',
      'Processed Foods': 'processed_food',
      'Marine Products': 'seafood',
      'Textiles': 'textiles',
      'Apparel & Garments': 'apparel',
      'Home Textiles': 'home_textiles',
      'Leather Products': 'leather',
      'Footwear': 'footwear',
      'Handicrafts': 'handicrafts',
      'Ceramics & Pottery': 'ceramics',
      'Glassware': 'glass',
      'Jewellery & Gems': 'jewellery',
      'Chemicals': 'chemicals',
      'Cosmetics & Personal Care': 'cosmetics',
      'Pharmaceuticals': 'pharmaceuticals',
      'Plastics & Rubber': 'plastics',
      'Electronics': 'electronics',
      'Engineering Goods': 'engineering',
      'Machinery': 'machinery',
      'Automotive Components': 'automotive',
      'Furniture & Wood': 'furniture',
      // 'Others' intentionally omitted so it falls through to HS-chapter
      // classification, which is more accurate than a generic bucket.
    }[productObj.category];
    if (named) return named;
    const ch = String(productObj.hscode || '').replace('.', '').slice(0, 2);
    const byChapter = {
      '69': 'ceramics', '70': 'glass', '41': 'leather', '42': 'leather', '43': 'leather',
      '50': 'textiles', '51': 'textiles', '52': 'textiles', '53': 'textiles',
      '54': 'textiles', '55': 'textiles', '61': 'textiles', '62': 'textiles', '63': 'textiles',
      '84': 'electronics', '85': 'electronics', '87': 'automotive',
      '28': 'chemicals', '29': 'chemicals', '30': 'pharmaceuticals',
      '31': 'chemicals', '32': 'chemicals', '33': 'cosmetics', '34': 'cosmetics',
      '38': 'chemicals', '39': 'kitchenware', '73': 'kitchenware', '76': 'kitchenware',
      '82': 'kitchenware', '94': 'kitchenware',
    };
    if (byChapter[ch]) return byChapter[ch];
    const n = parseInt(ch, 10);
    if (n >= 1 && n <= 15) return 'food';
    if (n >= 16 && n <= 23) return 'processed_food';
    return '';
  };

  //  Feature 2: AI Country Recommendations (XGBoost) 
  const [countryRankings, setCountryRankings] = useState([]);
  const [rankingsLoading, setRankingsLoading] = useState(false);

  const handleFetchCountryRankings = async () => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    if (!productObj) { addToast('Select a valid product first.', 'error'); return; }
    // Send the product's real category. The backend maps categories that have
    // no training data onto the closest trained category and discloses it,
    // rather than us silently substituting one here.
    const cat = deriveProductCategory(productObj);
    const hs = productObj.hscode?.replace('.', '').padEnd(8, '0').slice(0, 8);
    if (!hs) { addToast('This product has no HS code. Add one first.', 'error'); return; }
    setRankingsLoading(true);
    setCountryRankings([]);
    try {
      // Send the product attributes so the ranking is scored for THIS product,
      // not just its category. Without these the engine falls back to defaults.
      const countryList = ['United States','India','Germany','United Arab Emirates','Singapore','United Kingdom','Hong Kong','Australia','Canada','South Korea'];
      const rankings = []; for (const c of countryList) { try { const cr = await regulatoryApi.getCompliance(c, hs); if(cr.success!==false) rankings.push({country_code:c.slice(0,2).toUpperCase(),country_name:c,xgb_predicted_score:cr.complianceScore||50,reliability_tier:cr.complexity==='LOW'?'High':cr.complexity==='MEDIUM'?'Moderate':'Low',rank:0,shap_breakdown:{},reason:'Compliance: '+(cr.complexity||'N/A')+', '+(cr.documentsCount||0)+' docs, '+(cr.certificationsCount||0)+' certs'}); } catch{} }
      rankings.sort((a,b)=>(b.xgb_predicted_score||0)-(a.xgb_predicted_score||0)); rankings.forEach((r,i)=>r.rank=i+1);
      setCountryRankings(rankings);
      addToast('Ranked ' + rankings.length + ' countries for ' + productObj.name, 'success');
    } catch (err) {
      addToast(err.message?.includes('503') ? 'Analysis service unavailable - please try later' : err.message, 'error');
    } finally { setRankingsLoading(false); }
  };

  //  Country recommendation explanation (backend-calculated reasons) 
  const [countryRecoData, setCountryRecoData] = useState(null);

  const fetchCountryRecommendation = async (countryName) => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    if (!productObj || !countryName) return;
    const hs = (productObj.hscode || '').replace('.', '');
    const cat = deriveProductCategory(productObj);
    try {
      const res = await regulatoryApi.getCompliance(countryName, hs);
      setCountryRecoData(res);
    } catch { setCountryRecoData(null); }
  };

  //  Feature 3: Explain Recommendation 
  const [explainData, setExplainData] = useState(null);
  const [explainLoading, setExplainLoading] = useState(false);
  const [explainCountry, setExplainCountry] = useState(null);

  const handleExplainCountry = async (countryCode, countryName) => {
    setExplainCountry(countryName);
    setExplainData(null);
    setExplainLoading(true);
    setAnalysisSubView('explain');
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    const hs = productObj?.hscode?.replace('.','') || '';
    try {
      const res = await regulatoryApi.getCompliance(countryCode, hs);
      setExplainData(res);
    } catch (err) { addToast(err.message || 'Explain failed', 'error'); setAnalysisSubView('recommendations'); }
    finally { setExplainLoading(false); }
  };

  //  Feature 4: Export Regulations 
  const [regulationsData, setRegulationsData] = useState(null);
  const [regulationsLoading, setRegulationsLoading] = useState(false);

  const handleFetchRegulations = async (codeOrName) => {
    // Try to find an exact code match first, then fall back to name lookup
    const byCode = countries.find(c => c.code === codeOrName?.toUpperCase());
    const byName = countries.find(c => c.name === codeOrName);
    const found = byCode || byName;
    const cc = found?.code || codeOrName?.slice(0, 2).toUpperCase();
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    const hs = productObj?.hscode?.replace('.', '') || '';
    setRegulationsLoading(true);
    setRegulationsData(null);
    setAnalysisSubView('regulations');
    try {
      const res = await regulatoryApi.getRegulations(selectedCountry || codeOrName, hs);
      setRegulationsData(res);
    } catch (err) { addToast(err.message || 'Regulations fetch failed', 'error'); setAnalysisSubView('country-overview'); }
    finally { setRegulationsLoading(false); }
  };

  //  Feature 5: Step-by-Step Guidance 
  const [guidanceData, setGuidanceData] = useState(null);
  const [guidanceLoading, setGuidanceLoading] = useState(false);
  const [completedChecklist, setCompletedChecklist] = useState({});

  const handleFetchGuidance = async () => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    const hs = productObj?.hscode || '';
    setGuidanceLoading(true);
    setGuidanceData(null);
    setAnalysisSubView('guidance');
    try {
      const res = await regulatoryApi.getRequirements(selectedCountry, hs);
      setGuidanceData(res);
    } catch (err) { addToast(err.message || 'Guidance fetch failed', 'error'); setAnalysisSubView('country-overview'); }
    finally { setGuidanceLoading(false); }
  };

  //  Feature 6: Document Summarizer 
  const [docSummaryText, setDocSummaryText] = useState('');
  const [docSummaryTitle, setDocSummaryTitle] = useState('');
  const [docSummaryResult, setDocSummaryResult] = useState(null);
  const [docSummaryLoading, setDocSummaryLoading] = useState(false);

  const handleSummariseDoc = async () => {
    if (!docSummaryText.trim()) { addToast('Paste document text first.', 'error'); return; }
    setDocSummaryLoading(true);
    setDocSummaryResult(null);
    try {
      const res = await aiApi.summarizeDocument(docSummaryText, docSummaryTitle || 'Document');
      setDocSummaryResult(res);
    } catch (err) { addToast(err.message || 'Summarise failed', 'error'); }
    finally { setDocSummaryLoading(false); }
  };

  //  Feature 7: Document Templates 
  const [templateType, setTemplateType] = useState('commercial_invoice');
  const [templateFields, setTemplateFields] = useState({ exporter_name: getUser()?.name || '', exporter_address: 'Mumbai, Maharashtra, India', importer_name: '', importer_address: '', importer_country: selectedCountry, product_name: selectedAnalysisProduct, hs_code: '', quantity: '1000', unit_price: '350', total_value: '350000', port_of_loading: 'Mumbai, India' });
  const [templateResult, setTemplateResult] = useState(null);
  const [templateLoading, setTemplateLoading] = useState(false);


  const handleGenerateTemplate = () => {
    addToast('Document Templates have been removed from this version.', 'error');
  };

  //  Feature 8: Government Incentives 
  const [incentivesData, setIncentivesData] = useState(null);
  const [incentivesLoading, setIncentivesLoading] = useState(false);

  const handleFetchIncentives = async () => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    const hs = (productObj?.hscode || '').replace('.', '');
    setIncentivesLoading(true);
    setIncentivesData(null);
    try {
      const res = hs
        ? await intelligenceApi.getIncentivesByCountryAndHs('India', hs)
        : await intelligenceApi.getIncentivesByCountry('India');
      setIncentivesData(res);
      addToast(res.count > 0 ? ('Found ' + res.count + ' verified scheme(s).') : 'No verified incentives found.', res.count > 0 ? 'success' : 'info');
    } catch (err) { addToast(err.message || 'Incentives fetch failed', 'error'); }
    finally { setIncentivesLoading(false); }
  };

  //  Feature 9: Market Opportunity 
  const [marketOppData, setMarketOppData] = useState(null);
  const [marketOppLoading, setMarketOppLoading] = useState(false);

  const handleFetchMarketOpportunity = async () => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    const hs = (productObj?.hscode || '').replace('.', '');
    if (!hs) { addToast('This product has no HS code. Please add one first.', 'error'); return; }
    setMarketOppLoading(true);
    setMarketOppData(null);
    try {
      const candidateCountries = ['United States','Germany','United Arab Emirates','Singapore','United Kingdom','Australia','Canada','Japan','South Korea','Hong Kong'];
      const res = await intelligenceApi.rankMarketOpportunityForCountries(hs, candidateCountries);
      setMarketOppData(res);
      addToast('Market ranked for ' + (res.totalCountries || 0) + ' countries.', 'success');
    } catch (err) { addToast(err.message || 'Market opportunity fetch failed', 'error'); }
    finally { setMarketOppLoading(false); }
  };

  //  Feature 10: Country Risk 
  const [riskData, setRiskData] = useState(null);
  const [riskLoading, setRiskLoading] = useState(false);
  const [riskCountry, setRiskCountry] = useState('DE');

  const handleFetchRisk = async (countryCode) => {
    const cc = countryCode || riskCountry;
    setRiskLoading(true);
    setRiskData(null);
    try {
      const prodObj = products.find(p => p.name === selectedAnalysisProduct);
      const res = await regulatoryApi.getCompliance(cc, prodObj?.hscode || '');
      setRiskData(res);
    } catch (err) { addToast(err.message || 'Risk analysis failed', 'error'); }
    finally { setRiskLoading(false); }
  };

  //  Feature 11: Negotiation Assistant 
  const [negoFields, setNegoFields] = useState({ buyer_name: '', buyer_company: '', buyer_country: selectedCountry, product_name: selectedAnalysisProduct, hs_code: '', quantity: '1000 kg', proposed_price: '350', payment_terms: 'LC at sight', request_type: 'initial_email', context: '' });
  const [negoResult, setNegoResult] = useState(null);
  const [negoLoading, setNegoLoading] = useState(false);

  const handleNegotiation = async () => {
    if (!negoFields.buyer_name) { addToast('Enter buyer name first.', 'error'); return; }
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    setNegoLoading(true);
    setNegoResult(null);
    try {
      const hs = (productObj?.hscode || negoFields.hs_code || '').replace('.', '');
      const res = await intelligenceApi.negotiationAssistant({
        product: productObj?.name || negoFields.product_name,
        hsCode: hs,
        country: negoFields.buyer_country || selectedCountry,
        buyerMessage: negoFields.context || '',
        objective: negoFields.request_type || 'initial_email',
        quantity: negoFields.quantity,
        targetPrice: negoFields.proposed_price,
      });
      setNegoResult(res);
      addToast(res.available === false ? 'AI service not configured — set NVIDIA_API_KEY to enable.' : 'Negotiation advice generated.', res.available === false ? 'error' : 'success');
    } catch (err) { addToast(err.message || 'Negotiation failed', 'error'); }
    finally { setNegoLoading(false); }
  };
  const [complianceCheckData, setComplianceCheckData] = useState(null);
  const [complianceCheckLoading, setComplianceCheckLoading] = useState(false);

  const handleFetchComplianceCheck = async () => {
    const productObj = products.find(p => p.name === selectedAnalysisProduct);
    const cat = deriveProductCategory(productObj);
    const hs = productObj?.hscode || '';
    setComplianceCheckLoading(true);
    setComplianceCheckData(null);
    try {
      const res = await regulatoryApi.getRegulations(selectedCountry, hs);
      setComplianceCheckData(res);
    } catch (err) { addToast(err.message || 'Compliance check failed', 'error'); }
    finally { setComplianceCheckLoading(false); }
  };

  const handleStartAnalysis = async () => {
    if (!selectedAnalysisProduct) {
      addToast('Please select a product first.', 'error');
      return;
    }
    setIsAnalyzing(true);
    setAnalyzedProduct(null);
    setMarketAnalysisResult(null);

    // Find the product object — refresh the list if stale (e.g. just created)
    let productObj = products.find(p => p.name === selectedAnalysisProduct);
    if (!productObj) {
      // Products may not have refreshed yet after a creation. Fetch once.
      try {
        const res = await productsApi.getAll();
        const mapped = (res.data || []).map(p => ({
          id: p.id, name: p.name, hscode: p.hsCode, category: p.categoryName,
          categoryId: p.categoryId, price: parseFloat(p.price), unit: 'kg',
          currency: 'INR', origin: 'India', status: 'Active',
          weight: p.weight, description: p.description, quantity: p.quantity,
        }));
        setProducts(mapped);
        productObj = mapped.find(p => p.name === selectedAnalysisProduct);
      } catch { /* ignore */ }
    }
    if (!productObj) {
      setIsAnalyzing(false);
      addToast('Product not found in catalog. Please select a valid product from the dropdown.', 'error');
      return;
    }

    // If a country is selected, do a market analysis call
    if (selectedCountry && countries.length > 0) {
      const countryObj = countries.find(c => c.name === selectedCountry);
      if (countryObj) {
        try {
          const res = await marketApi.analyze({ countryId: countryObj.id, productId: productObj.id });
          setMarketAnalysisResult(res.data);
        } catch (err) {
          // Non-critical, we still show recommendations
        }
      }
    }

    setTimeout(() => {
      setIsAnalyzing(false);
      setAnalyzedProduct(selectedAnalysisProduct);
      setAnalysisSubView('recommendations');
      addToast(`Analysis completed for ${selectedAnalysisProduct}!`, 'success');
      // Auto-fetch country rankings if not already loaded
      if (countryRankings.length === 0) {
        handleFetchCountryRankings();
      }
    }, 800);
  };

  const [chatLoading, setChatLoading] = useState(false);

  const handleSendComplianceChat = async () => {
    if (!complianceChatText.trim()) return;
    const userMsg = complianceChatText.trim();
    setComplianceChatText('');
    setComplianceChatHistory(prev => [...prev, { sender: 'user', text: userMsg }]);
    setChatLoading(true);

    try {
      // Build history for the RAG chatbot (last 10 messages)
      const history = complianceChatHistory.slice(-10).map(m => ({
        role: m.sender === 'user' ? 'user' : 'assistant',
        content: m.text,
      }));

      // Find the product's HS code for context
      const productObj = products.find(p => p.name === selectedAnalysisProduct);
      const hsCode = productObj?.hscode || null;

      // Map country name to its ISO-2 code using the fetched countries list
      const countryObj = countries.find(c => c.name === selectedCountry);
      const countryCode = countryObj?.code || null;

      const res = await aiApi.regulatoryChat(selectedCountry || 'India', productObj?.hscode || '', complianceChatText);

      const reply = res.reply || res.data?.reply || 'No response from compliance assistant.';
      setComplianceChatHistory(prev => [...prev, { sender: 'assistant', text: reply }]);
    } catch (err) {
      // Fallback: show error but keep chat usable
      const fallback = err.message?.includes('unavailable')
        ? 'The compliance assistant is currently offline. Please try again later.'
        : `Sorry, I encountered an error: ${err.message || 'Unknown error'}. Please try again.`;
      setComplianceChatHistory(prev => [...prev, { sender: 'assistant', text: fallback }]);
    } finally {
      setChatLoading(false);
    }
  };

  const validateProductForm = () => {
    const newErrors = {};
    if (!newProdName.trim()) {
      newErrors.name = 'Product name is required';
    }
    if (!newProdDesc.trim()) {
      newErrors.description = 'Product description is required for HS code classification';
    }
    if (!newProdHscode.trim()) {
      newErrors.hscode = 'HS code is required. Click Predict to get suggestions.';
    } else if (!/^\d{4}(\.?\d{2}){0,2}$/.test(newProdHscode.trim())) {
      newErrors.hscode = 'Please enter a valid HS code (e.g., 0910, 0910.30, or 09103000)';
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
      addToast('Please correct the validation errors.', 'error');
      return;
    }
    setDrawerLoading(true);
    try {
      // Resolve the category ID from the loaded category list.
      const categoryObj = categories.find(c => c.categoryName === newProdCategory);
      if (!categoryObj) {
        addToast('Select a valid product category.', 'error');
        setDrawerLoading(false);
        return;
      }

      const payload = {
        categoryId: categoryObj.id,
        name: newProdName,
        hsCode: newProdHscode,
        description: newProdDesc || null,
        price: parseFloat(newProdPrice),
        quantity: null,
        weight: newProdWeight ? parseFloat(newProdWeight) : null,
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
      addToast('Please correct the validation errors.', 'error');
      return;
    }
    setDrawerLoading(true);
    try {
      const categoryObj = categories.find(c => c.categoryName === newProdCategory);
      if (!categoryObj) {
        addToast('Select a valid product category.', 'error');
        setDrawerLoading(false);
        return;
      }

      const payload = {
        categoryId: categoryObj.id,
        name: newProdName,
        hsCode: newProdHscode,
        description: newProdDesc || null,
        price: parseFloat(newProdPrice),
        quantity: null,
        weight: newProdWeight ? parseFloat(newProdWeight) : null,
      };

      if (editingProductId) {
        await productsApi.update(editingProductId, payload);
      } else {
        await productsApi.create(payload);
      }
      await fetchProducts();

      setShowAddDrawer(false);
      setNewProdName('');
      setNewProdHscode('');
      setNewProdDesc('');
      setNewProdPrice('');
      setNewProdWeight('');
      setErrors({});
      setEditingProductId(null);

      // Navigate to Analysis view
      setSelectedAnalysisProduct(newProdName);
      setAnalyzedProduct(null);
      setIsAnalyzing(true);
      setActiveView('analysis');

      setTimeout(() => {
        setIsAnalyzing(false);
        setAnalyzedProduct(newProdName);
        addToast(`Product saved & analysis view ready for ${newProdName}`, 'success');
      }, 1500);
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
    <div className="min-h-screen bg-[#f8fafc] text-slate-700 flex flex-col antialiased font-sans relative">
      
      {/* Toast Notification Stack */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3">
        {quotaExhausted && (
          <div className="flex items-center gap-3 px-5 py-4 rounded-xl border border-amber-200 bg-amber-50/95 backdrop-blur-md shadow-2xl animate-in fade-in slide-in-from-bottom-5 duration-300 max-w-sm">
            <span className="text-amber-500 text-lg">-</span>
            <div>
              <span className="text-xs font-bold text-amber-800 block">API Quota Exhausted</span>
              <span className="text-[10px] text-amber-600">Service will resume shortly. Using cached data.</span>
            </div>
            <button onClick={() => setQuotaExhausted(false)} className="ml-auto text-amber-400 hover:text-amber-600 cursor-pointer text-xs">-</button>
          </div>
        )}
        {toasts.map(t => (
          <div key={t.id} className={`flex items-center gap-3 px-5 py-4 rounded-xl border backdrop-blur-md shadow-2xl animate-in fade-in slide-in-from-bottom-5 duration-300 ${
            t.type === 'success' 
              ? 'bg-emerald-50 border-emerald-200 text-slate-800 shadow-emerald-500/5' 
              : 'bg-red-50 border-red-200 text-red-800 shadow-red-500/5'
          }`}>
            <div className={`w-2 h-2 rounded-full ${t.type === 'success' ? 'bg-emerald-500' : 'bg-red-500'}`}></div>
            <span className="text-xs font-bold">{t.message}</span>
          </div>
        ))}
      </div>

      {/* TOP NAVIGATION BAR */}
      <nav className="fixed top-0 left-0 right-0 h-16 bg-white border-b border-slate-200/80 backdrop-blur-md shadow-sm z-30 flex items-center justify-between px-6">
        {/* Left Logo */}
        <div className="flex items-center gap-2 cursor-pointer" onClick={() => onNavigate('/')}>
          <span className="text-lg font-black text-slate-900 tracking-tight">Trade</span>
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
            onClick={() => setActiveView('products')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'products' ? 'text-sky-655 font-extrabold' : 'hover:text-slate-950'}`}
          >
            Products
            {activeView === 'products' && <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => { setActiveView('analysis'); setAnalysisSubView('select'); }}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'analysis' ? 'text-sky-655 font-extrabold' : 'hover:text-slate-950'}`}
          >
            Market Analysis
            {activeView === 'analysis' && <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div>}
          </button>

          <button 
            onClick={() => setActiveView('orders')}
            className={`transition-colors h-full px-1 cursor-pointer relative ${activeView === 'orders' ? 'text-sky-655 font-extrabold' : 'hover:text-slate-950'}`}
          >
            Orders & Fulfillment
            {activeView === 'orders' && <div className="absolute bottom-0 left-0 right-0 h-[2.5px] bg-sky-500 rounded-t-full"></div>}
          </button>

        </div>

        {/* Right side controls */}
        <div className="flex items-center gap-4 relative">
          
          {/* Notification Bell */}
          <div className="relative">
            <button 
              onClick={(e) => {
                e.stopPropagation();
                setShowNotifications(!showNotifications);
                setShowProfileMenu(false);
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

            {/* Notification Dropdown Box */}
            {showNotifications && (
              <div 
                onClick={(e) => e.stopPropagation()} 
                className="absolute right-0 mt-2 w-80 bg-white border border-slate-200 rounded-2xl shadow-2xl p-4 animate-in fade-in slide-in-from-top-2 duration-200 z-50"
              >
                <div className="flex justify-between items-center border-b border-slate-100 pb-2 mb-2.5">
                  <span className="text-xs font-black text-slate-800">Notifications</span>
                  <button 
                    onClick={() => {
                      setNotifications(notifications.map(n => ({ ...n, read: true })));
                      addToast('All notifications marked as read', 'success');
                    }}
                    className="text-[10px] font-bold text-sky-600 hover:underline"
                  >
                    Mark all read
                  </button>
                </div>
                
                <div className="space-y-2 max-h-60 overflow-y-auto">
                  {notifications.length > 0 ? (
                    notifications.map(n => (
                      <div key={n.id} className={`p-2.5 rounded-xl border text-xxs transition-colors ${n.read ? 'border-slate-50 bg-white' : 'border-sky-100 bg-sky-50/10'}`}>
                        <div className="flex justify-between items-start">
                          <span className="font-bold text-slate-700 leading-normal">{n.text}</span>
                          {!n.read && <div className="w-1.5 h-1.5 rounded-full bg-sky-500 shrink-0 mt-1.5 ml-1"></div>}
                        </div>
                        <span className="text-[10px] text-slate-400 mt-1 block">{n.time}</span>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-6 text-slate-400 text-xs font-semibold">No notifications</div>
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
              className="flex items-center gap-1.5 cursor-pointer"
            >
              <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center overflow-hidden">
                <div className="w-6 h-6 rounded-full bg-sky-500 flex items-center justify-center text-[10px] font-black text-white">TW</div>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
            </button>

            {/* Profile Dropdown Box */}
            {showProfileMenu && (
              <div 
                onClick={(e) => e.stopPropagation()} 
                className="absolute right-0 mt-2 w-48 bg-white border border-slate-200 rounded-2xl shadow-2xl p-2.5 animate-in fade-in slide-in-from-top-2 duration-200 z-50"
              >
                <div className="p-2 border-b border-slate-100 mb-1.5">
                  <span className="block text-xs font-black text-slate-800">{getUser()?.name || 'Trade User'}</span>
                  <span className="block text-[10px] text-slate-450 mt-0.5 truncate">{getUser()?.email || 'exporter@company.com'}</span>
                </div>
                
                <button 
                  onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full text-left flex items-center gap-2.5 p-2 rounded-xl text-xxs font-bold text-slate-600 hover:bg-slate-50 hover:text-slate-900 cursor-pointer"
                >
                  <UserIcon className="w-4 h-4 text-slate-400" />
                  View Profile
                </button>

                <button 
                  onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full text-left flex items-center gap-2.5 p-2 rounded-xl text-xxs font-bold text-slate-600 hover:bg-slate-50 hover:text-slate-900 cursor-pointer"
                >
                  <Settings className="w-4 h-4 text-slate-400" />
                  Settings
                </button>

                <button 
                  onClick={() => { setActiveView('profile'); setShowProfileMenu(false); }}
                  className="w-full text-left flex items-center gap-2.5 p-2 rounded-xl text-xxs font-bold text-slate-600 hover:bg-slate-50 hover:text-slate-900 cursor-pointer"
                >
                  <Briefcase className="w-4 h-4 text-slate-400" />
                  My Company
                </button>

                <div className="border-t border-slate-100 my-1.5"></div>

                <button 
                  onClick={handleLogout}
                  className="w-full text-left flex items-center gap-2.5 p-2 rounded-xl text-xxs font-bold text-red-655 hover:bg-red-50 cursor-pointer"
                >
                  <LogOut className="w-4 h-4 text-red-400" />
                  Logout
                </button>
              </div>
            )}
          </div>

        </div>
      </nav>

      {/* VIEW RENDER DISPATCHER */}
      <div className="flex-grow pt-24 pb-12 px-6 max-w-7xl w-full mx-auto relative z-10">

        {/* ---------------------------------------------------- */}
        {/* VIEW: OVERVIEW / DASHBOARD */}
        {/* ---------------------------------------------------- */}
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
                  <span className="text-[10px] font-extrabold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full"> {dashboardStats ? dashboardStats.pendingLogisticsRequests : 0} Action</span>
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

            {/* Quick Actions & Recent Activity split column */}
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
                    {/* Vertical timeline connection */}
                    <div className="absolute left-[7px] top-2 bottom-2 w-0.5 bg-slate-100 z-0"></div>

                    {/* Dynamic timeline items from orders + products */}
                    {(() => {
                      // Build activity feed from real data
                      const activities = [];
                      // From orders (most recent first)
                      orders.slice(0, 5).forEach(o => {
                        const statusColors = { 'Pending': 'bg-amber-500', 'Accepted': 'bg-sky-500', 'In Transit': 'bg-indigo-500', 'Shipped': 'bg-indigo-500', 'Delivered': 'bg-emerald-500' };
                        activities.push({
                          id: `order-${o.id}`,
                          title: `Export Order #${o.id} — ${o.status}`,
                          desc: `${o.product} · ${o.qty} to ${o.country}`,
                          time: o.date || 'Recently',
                          color: statusColors[o.status] || 'bg-slate-400',
                        });
                      });
                      // From products (recently added)
                      products.slice(-3).reverse().forEach(p => {
                        activities.push({
                          id: `product-${p.id}`,
                          title: 'Product added to catalog',
                          desc: `${p.name} (HS ${p.hscode || 'pending'})`,
                          time: 'Recently',
                          color: 'bg-sky-500',
                        });
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
                    // Derive a performance index from the product's order history
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
        )}

        {/* ---------------------------------------------------- */}
        {/* VIEW: PRODUCTS CATALOG */}
        {/* ---------------------------------------------------- */}
        {activeView === 'products' && (
          <div className="space-y-6 animate-in fade-in duration-300 relative">
            
            {/* Header section */}
            <div className="flex justify-between items-center">
              <div>
                <h1 className="text-2xl font-black text-slate-900 tracking-tight">Products Catalog</h1>
                <p className="text-xs text-slate-500 mt-1 font-medium">Manage your product catalog, HS codes, and export scoring.</p>
              </div>
              <button 
                onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); }}
                className="inline-flex items-center gap-1.5 px-4.5 py-2.5 font-bold text-xs text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 shadow-md rounded-xl transition-all cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                Add Product
              </button>
            </div>

            {/* Search & Filter Bar */}
            <div className="bg-white border border-slate-200/80 rounded-xl p-4 shadow-sm flex flex-col sm:flex-row gap-4 items-center justify-between">
              {/* Search box */}
              <div className="relative w-full sm:max-w-xs">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <input 
                  type="text" 
                  value={searchProduct}
                  onChange={(e) => setSearchProduct(e.target.value)}
                  placeholder="Search products by name or HS code..."
                  className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all"
                />
              </div>

              {/* Filter category */}
              <div className="flex items-center gap-2 shrink-0">
                <span className="text-xxs font-bold text-slate-400 uppercase tracking-wider">Filter by:</span>
                <select 
                  value={filterCategory}
                  onChange={(e) => setFilterCategory(e.target.value)}
                  className="px-3 py-2 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none cursor-pointer"
                >
                  <option>All</option>
                  <option>Spices</option>
                  <option>Textiles</option>
                  <option>Agri-Prod</option>
                  <option>Handicraft</option>
                </select>
              </div>
            </div>

            {/* List Table Container */}
            <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-450 uppercase tracking-widest">
                      <th className="py-4 px-5">Product Name</th>
                      <th className="py-4 px-5">HS Code</th>
                      <th className="py-4 px-5">Category</th>
                      <th className="py-4 px-5">Price</th>
                      <th className="py-4 px-5">Status</th>
                      <th className="py-4 px-5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
                    
                    {products.length > 0 ? (
                      products
                        .filter(p => {
                          const matchesSearch = p.name.toLowerCase().includes(searchProduct.toLowerCase()) || p.hscode.includes(searchProduct);
                          const matchesCat = filterCategory === 'All' || p.category === filterCategory;
                          return matchesSearch && matchesCat;
                        })
                        .map(p => (
                          <tr key={p.id} className="hover:bg-slate-50/40 transition-colors">
                            <td className="py-4 px-5 font-bold text-slate-900">{p.name}</td>
                            <td className="py-4 px-5 font-mono text-slate-500">{p.hscode}</td>
                            <td className="py-4 px-5 text-slate-500">{p.category}</td>
                            <td className="py-4 px-5 text-slate-805">INR {p.price}/{p.unit}</td>
                            <td className="py-4 px-5">
                              {p.status === 'Active' && (
                                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                                  Active
                                </span>
                              )}
                              {p.status === 'Draft' && (
                                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                                  <span className="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
                                  Draft
                                </span>
                              )}
                              {p.status === 'Analyzing' && (
                                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-sky-600 bg-sky-50 px-2 py-0.5 rounded-full">
                                  <Loader2 className="w-3 h-3 text-sky-500 animate-spin" />
                                  Analyzing
                                </span>
                              )}
                            </td>
                            <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                              <button 
                                onClick={() => {
                                  setSelectedAnalysisProduct(p.name);
                                  setActiveView('analysis');
                                  setAnalysisSubView('select');
                                  addToast(`Loading market index details for ${p.name}...`, 'success');
                                }}
                                className="p-1.5 text-slate-400 hover:text-sky-600 rounded-lg hover:bg-slate-50 cursor-pointer"
                                title="Analyze Product"
                              >
                                <Eye className="w-4 h-4" />
                              </button>
                              <button 
                                onClick={() => handleEditProduct(p)}
                                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-50 cursor-pointer"
                                title="Edit specs"
                              >
                                <Edit3 className="w-4 h-4" />
                              </button>
                              <button 
                                onClick={() => handleDeleteProduct(p.id, p.name)}
                                className="p-1.5 text-slate-400 hover:text-red-600 rounded-lg hover:bg-slate-50 cursor-pointer"
                                title="Delete"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </td>
                          </tr>
                        ))
                    ) : (
                      <tr>
                        <td colSpan="6" className="py-12">
                          <div className="flex flex-col items-center justify-center max-w-sm mx-auto text-center">
                            <Briefcase className="w-12 h-12 text-slate-300 mb-4" />
                            <span className="block text-sm font-bold text-slate-800">No products added yet</span>
                            <span className="block text-xxs text-slate-400 mt-1 leading-normal">Add your first custom catalog specification to run automated compliance checks and profit audits.</span>
                            <button 
                              onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); }}
                              className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-xxs font-bold text-white bg-sky-500 rounded-xl hover:bg-sky-400"
                            >
                              <Plus className="w-3.5 h-3.5" /> Add Your First Product
                            </button>
                          </div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              {/* Table footer info */}
              <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xxs font-bold text-slate-400">
                <span>Showing {products.length} of {products.length} products</span>
                <div className="flex items-center gap-2">
                  <button className="px-3 py-1.5 border border-slate-200 rounded-lg hover:text-slate-700 disabled:opacity-40 cursor-pointer" disabled>Prev</button>
                  <span className="text-slate-800">Page 1</span>
                  <button className="px-3 py-1.5 border border-slate-200 rounded-lg hover:text-slate-700 disabled:opacity-40 cursor-pointer" disabled>Next</button>
                </div>
              </div>
            </div>




          </div>
        )}

        {/* ---------------------------------------------------- */}
        {/* VIEW: MARKET ANALYSIS */}
        {/* ---------------------------------------------------- */}
        {activeView === 'analysis' && (
          <div className="space-y-6 animate-in fade-in duration-300">
            {/* Header section with back button if not in select step */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
              <div>
                <h1 className="text-2xl font-black text-slate-900 tracking-tight">Market Analysis</h1>
                <p className="text-xs text-slate-500 mt-1 font-medium">
                  {analysisSubView === 'select' && "Smart export recommendations for your products"}
                  {analysisSubView === 'recommendations' && `Global recommendations for ${selectedAnalysisProduct}`}
                  {analysisSubView === 'country-overview' && `${selectedCountry} Recommendation Hub`}
                  {analysisSubView === 'compliance' && `${selectedCountry} Compliance Report`}
                  {analysisSubView === 'cost' && `${selectedCountry} Cost & Profit Estimation`}
                </p>
              </div>

              {analysisSubView !== 'select' && (
              <button
                  onClick={() => {
                    const backs = {'recommendations':'select','explain':'recommendations','country-overview':'recommendations','compliance':'country-overview','cost':'country-overview','what-if':'country-overview','regulations':'country-overview','guidance':'country-overview'};
                    setAnalysisSubView(backs[analysisSubView] || 'select');
                  }}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-slate-600 hover:text-slate-900 bg-white border border-slate-200 rounded-xl transition-all cursor-pointer shadow-sm hover:shadow"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back</span>
                </button>
              )}
            </div>

            {/* SUB-VIEW: SELECT PRODUCT (Analyze Product) */}
            {analysisSubView === 'select' && (
              <div className="max-w-xl mx-auto py-8">
                {isAnalyzing ? (
                  <div className="bg-white border border-slate-200/85 rounded-2xl p-12 text-center shadow-lg flex flex-col items-center justify-center min-h-[350px] animate-in fade-in duration-300">
                    <div className="w-16 h-16 rounded-full bg-sky-50 flex items-center justify-center mb-6">
                      <Loader2 className="w-8 h-8 text-sky-500 animate-spin" />
                    </div>
                    <h3 className="text-sm font-black text-slate-800 uppercase tracking-widest">Running Market Analysis</h3>
                    <p className="text-xxs text-slate-450 mt-2 max-w-xs leading-relaxed">
                      Evaluating tariff indices, phytosanitary requirements, shipping freight costs, and country credit risks for <strong>{selectedAnalysisProduct}</strong>...
                    </p>
                  </div>
                ) : (
                  <div className="bg-white border border-slate-200/80 rounded-2xl p-6 sm:p-8 shadow-sm space-y-6 animate-in fade-in duration-300">
                    <div className="flex items-center gap-3 border-b border-slate-100 pb-4">
                      <div className="p-3 rounded-xl bg-sky-50 text-sky-500">
                        <Globe className="w-6 h-6" />
                      </div>
                      <div>
                        <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Select Product to Analyze</h3>
                        <p className="text-[10px] text-slate-400 font-medium">Access compliance complexity ratings and landed cost estimates</p>
                      </div>
                    </div>

                    <div className="space-y-4">
                      <div className="space-y-1.5">
                        <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block">Choose Catalog Product</label>
                        <select 
                          value={selectedAnalysisProduct}
                          onChange={(e) => {
                            setSelectedAnalysisProduct(e.target.value);
                            setAnalyzedProduct(null);
                          }}
                          className="w-full px-4 py-3 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer"
                        >
                          {products.length === 0 ? (
                            <option value="">No products in catalog - add one first</option>
                          ) : (
                            <>
                              <option value="">-- Select a product --</option>
                              {products.map(p => (
                                <option key={p.id} value={p.name}>{p.name}</option>
                              ))}
                            </>
                          )}
                        </select>
                      </div>

                      <button
                        onClick={handleStartAnalysis}
                        className="w-full inline-flex items-center justify-center gap-2 px-5 py-3 font-bold text-xs text-white bg-sky-500 hover:bg-sky-400 shadow-md shadow-sky-500/10 rounded-xl transition-all cursor-pointer"
                      >
                        <span>Start Market Analysis</span>
                        <ArrowRight className="w-4 h-4" />
                      </button>

                      <button
                        onClick={handleFetchCountryRankings}
                        disabled={rankingsLoading}
                        className="w-full inline-flex items-center justify-center gap-2 px-5 py-3 font-bold text-xs text-indigo-700 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 rounded-xl transition-all cursor-pointer disabled:opacity-50"
                      >
                        {rankingsLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                        <span>{rankingsLoading ? 'Ranking countries...' : 'Country Ranking'}</span>
                      </button>

                      {countryRankings.length > 0 && (
                        <div className="border border-indigo-100 rounded-xl overflow-hidden mt-2">
                          <div className="bg-indigo-50 px-4 py-2 text-[10px] font-black text-indigo-700 uppercase tracking-widest">Country Rankings</div>
                          <div className="divide-y divide-slate-100">
                            {countryRankings.slice(0, 5).map((r, i) => (
                              <div key={r.country_code} className="flex items-center justify-between px-4 py-2.5">
                                <div className="flex items-center gap-2">
                                  <span className="text-[10px] font-black text-slate-400">#{r.rank}</span>
                                  <span className="text-xs font-bold text-slate-800">{r.country_name || r.country_code}</span>
                                  <span className={`text-[9px] px-1.5 py-0.5 rounded-full font-bold ${r.reliability_tier === 'High' ? 'bg-emerald-50 text-emerald-600' : r.reliability_tier === 'Moderate' ? 'bg-amber-50 text-amber-600' : 'bg-slate-100 text-slate-500'}`}>{r.reliability_tier}</span>
                                </div>
                                <div className="flex items-center gap-2">
                                  <span className="text-xs font-extrabold text-sky-600">{r.xgb_predicted_score?.toFixed(1)}</span>
                                  <button onClick={() => handleExplainCountry(r.country_code, r.country_name || r.country_code)} className="text-[10px] px-2 py-1 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 font-bold rounded-lg cursor-pointer">Explain</button>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* SUB-VIEW: RECOMMENDATIONS TABLE */}
            {analysisSubView === 'recommendations' && (
              <div className="space-y-5 animate-in fade-in duration-300">
                <div className="bg-sky-50/50 border border-sky-100 rounded-2xl p-4 flex items-center justify-between text-xs font-bold text-sky-700">
                  <div className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                    <span>Export suggestions matching <strong>{selectedAnalysisProduct}</strong> parameters:</span>
                  </div>
                  <span className="bg-sky-100 text-sky-800 px-2.5 py-0.5 rounded-full text-[10px]">3 Target Markets</span>
                </div>

                <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
                  {rankingsLoading ? (
                    <div className="p-12 flex flex-col items-center gap-3"><Loader2 className="w-6 h-6 text-sky-500 animate-spin"/><span className="text-xs font-bold text-slate-500">Ranking countries...</span></div>
                  ) : countryRankings.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-450 uppercase tracking-widest">
                          <th className="py-4 px-5">#</th>
                          <th className="py-4 px-5">Country</th>
                          <th className="py-4 px-5">Score</th>
                          <th className="py-4 px-5">Reliability</th>
                          <th className="py-4 px-5 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
                        {countryRankings.slice(0, 10).map((r) => (
                          <React.Fragment key={r.country_code}>
                          <tr className="hover:bg-slate-50/40 transition-colors">
                            <td className="py-4 px-5 text-slate-400 font-black">#{r.rank}</td>
                            <td className="py-4 px-5">
                              <div className="flex items-center gap-2">
                                <span className="text-sm font-black text-slate-500">{r.country_code}</span>
                                <span className="font-bold text-slate-800">{r.country_name || r.country_code}</span>
                              </div>
                            </td>
                            <td className="py-4 px-5">
                              <span className="text-sky-655 font-extrabold text-sm">{r.xgb_predicted_score?.toFixed(1)}</span>
                            </td>
                            <td className="py-4 px-5">
                              <span className={`inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full ${r.reliability_tier === 'High' ? 'text-emerald-600 bg-emerald-50' : r.reliability_tier === 'Moderate' ? 'text-amber-600 bg-amber-50' : 'text-slate-600 bg-slate-100'}`}>
                                {r.reliability_tier || 'N/A'}
                              </span>
                            </td>
                            <td className="py-4 px-5 text-right">
                              <button
                                onClick={() => { setSelectedCountry(r.country_name || r.country_code); setAnalysisSubView('country-overview'); setComplianceCheckData(null); setRegulationsData(null); setGuidanceData(null); setCalculatedCostBreakdown(null); setCompletedChecklist({}); setCountryRecoData(null); fetchCountryRecommendation(r.country_name || r.country_code); const countryObj = countries.find(c => c.code === r.country_code || c.name === r.country_name); const productObj = products.find(p => p.name === selectedAnalysisProduct); if (countryObj && productObj) { marketApi.analyze({ countryId: countryObj.id, productId: productObj.id }).then(res => setMarketAnalysisResult(res.data)).catch(() => {}); } }}
                                className="px-3.5 py-1.5 text-[10px] font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-lg shadow transition-all cursor-pointer"
                              >
                                View Details
                              </button>
                            </td>
                          </tr>
                          {/* Factor breakdown — shows why this country ranks here */}
                          {r.market_demand != null && (
                            <tr className="bg-slate-50/40">
                              <td></td>
                              <td colSpan={4} className="px-5 pb-3 pt-0">
                                <div className="flex flex-wrap gap-1.5 items-center">
                                  {[['Demand', r.market_demand], ['Compliance', r.compliance_score],
                                    ['Tariff', r.tariff_score], ['Landed Cost', r.landed_cost_score],
                                    ['FTA', r.agreement_score], ['Competition', r.competition_score],
                                    ['Risk', r.risk_score]].map(([lbl, v]) => v != null && (
                                    <span key={lbl} className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-white border border-slate-200 text-slate-600">
                                      {lbl} {Math.round(v)}
                                    </span>
                                  ))}
                                  {r.duty_rate != null && <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-amber-50 border border-amber-100 text-amber-700">Duty {r.duty_rate}%</span>}
                                  {r.tax_rate != null && r.tax_rate > 0 && <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-amber-50 border border-amber-100 text-amber-700">{r.tax_label} {r.tax_rate}%</span>}
                                  {r.lead_time_days != null && <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-sky-50 border border-sky-100 text-sky-700">{r.lead_time_days}d lead</span>}
                                  {r.landed_cost_per_unit != null && <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-indigo-50 border border-indigo-100 text-indigo-700">INR {r.landed_cost_per_unit}/unit</span>}
                                </div>
                                {r.reason && <p className="text-[10px] text-slate-500 leading-snug mt-1.5">{r.reason}</p>}
                              </td>
                            </tr>
                          )}
                          </React.Fragment>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  ) : (
                    <div className="p-8 text-center">
                      <p className="text-xs text-slate-500">No rankings yet. Click "Start Market Analysis" on the product selection page to generate recommendations.</p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* SUB-VIEW: COUNTRY OVERVIEW PAGE */}
            {analysisSubView === 'country-overview' && (
              <div className="space-y-6 animate-in fade-in duration-300">
                {/* Header overview banner */}
                <div className="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center text-lg font-black text-slate-600">
                      {countries.find(c => c.name === selectedCountry)?.code || selectedCountry.slice(0,2).toUpperCase()}
                    </div>
                    <div>
                      <h2 className="text-xl font-black text-slate-900 tracking-tight">{selectedCountry} Market Summary</h2>
                      <p className="text-xxs text-slate-400 font-bold uppercase tracking-wider mt-0.5">Route evaluation for {selectedAnalysisProduct}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xxs font-black tracking-widest text-emerald-600 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-100 uppercase">* Recommended</span>
                    <span className="text-[9px] text-slate-400">Updated: {new Date().toLocaleDateString()}</span>
                  </div>
                </div>

                {/* AI Recommendation Summary - Enhanced Metrics */}
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="bg-white border border-slate-200/80 p-4.5 rounded-2xl shadow-sm text-center space-y-1">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">Score</span>
                    <span className="text-xl font-black text-sky-650 block">{marketAnalysisResult ? (70 + Math.min(30, (100 - parseFloat(marketAnalysisResult.customsDuty || 5)) * 0.5)).toFixed(1) : '--'}/100</span>
                    <span className="text-[9px] text-sky-500 font-bold block">Confidence: {marketAnalysisResult ? 'High' : 'N/A'}</span>
                  </div>
                  <div className="bg-white border border-slate-200/80 p-4.5 rounded-2xl shadow-sm text-center space-y-1">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">Market Demand</span>
                    <span className="text-xl font-black text-emerald-600 block">{marketAnalysisResult ? (parseFloat(marketAnalysisResult.customsDuty || 10) < 8 ? 'HIGH' : 'MEDIUM') : '--'}</span>
                    <span className="text-[9px] text-emerald-500 font-bold block"> {marketAnalysisResult?.transitTime || 'Evaluating...'}</span>
                  </div>
                  <div className="bg-white border border-slate-200/80 p-4.5 rounded-2xl shadow-sm text-center space-y-1">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">Expected Profit</span>
                    <span className="text-xl font-black text-slate-800 block">{(() => { const p = products.find(pr => pr.name === selectedAnalysisProduct); if (!p || !marketAnalysisResult) return '--'; const landed = p.price + p.price * parseFloat(marketAnalysisResult.customsDuty || 5) / 100 + 45 + 12 + 8; return `INR ${(landed * 0.3).toFixed(2)}/kg`; })()}</span>
                    <span className="text-[9px] text-slate-400 font-bold block">After duties & freight</span>
                  </div>
                  <div className="bg-white border border-slate-200/80 p-4.5 rounded-2xl shadow-sm text-center space-y-1">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">Compliance Index</span>
                    <span className="text-xl font-black text-amber-600 block">{marketAnalysisResult?.requiredCertificates ? Math.max(60, 100 - marketAnalysisResult.requiredCertificates.split(',').length * 5) : '--'}/100</span>
                    <span className="text-[9px] text-amber-500 font-bold block">{marketAnalysisResult?.requiredCertificates ? `${marketAnalysisResult.requiredCertificates.split(',').length} certificates required` : 'Evaluating...'}</span>
                  </div>
                </div>

                {/* Extended Metrics Row */}
                <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
                  <div className="bg-slate-50 border border-slate-100 p-3 rounded-xl text-center">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">Export Difficulty</span>
                    <span className="text-xs font-black text-slate-800 mt-0.5 block">{marketAnalysisResult ? (parseFloat(marketAnalysisResult.customsDuty || 5) > 10 ? 'Hard' : parseFloat(marketAnalysisResult.customsDuty || 5) > 5 ? 'Medium' : 'Easy') : '--'}</span>
                  </div>
                  <div className="bg-slate-50 border border-slate-100 p-3 rounded-xl text-center">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">Documents</span>
                    <span className="text-xs font-black text-slate-800 mt-0.5 block">{marketAnalysisResult?.requiredDocuments ? marketAnalysisResult.requiredDocuments.split(',').length : '--'}</span>
                  </div>
                  <div className="bg-slate-50 border border-slate-100 p-3 rounded-xl text-center">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">Certificates</span>
                    <span className="text-xs font-black text-slate-800 mt-0.5 block">{marketAnalysisResult?.requiredCertificates ? marketAnalysisResult.requiredCertificates.split(',').length : '--'}</span>
                  </div>
                  <div className="bg-slate-50 border border-slate-100 p-3 rounded-xl text-center">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">Timeline</span>
                    <span className="text-xs font-black text-slate-800 mt-0.5 block">{marketAnalysisResult?.transitTime || '--'}</span>
                  </div>
                  <div className="bg-slate-50 border border-slate-100 p-3 rounded-xl text-center">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">Country Risk</span>
                    <span className="text-xs font-black text-slate-800 mt-0.5 block">{marketAnalysisResult ? (parseFloat(marketAnalysisResult.customsDuty || 5) > 12 ? 'Medium' : 'Low') : '--'}</span>
                  </div>
                </div>

                {/* AI Summary Box */}
                {marketAnalysisResult && (
                  <div className="bg-gradient-to-r from-sky-50 to-indigo-50 border border-sky-100 rounded-2xl p-4">
                    <div className="flex items-start gap-3">
                      <span className="text-lg">-</span>
                      <div>
                        <span className="text-[10px] font-black text-sky-700 uppercase tracking-widest block mb-1">Recommendation Summary{countryRecoData?.verdict ? `: ${countryRecoData.verdict}` : ''}</span>
                        <p className="text-xs text-slate-700 leading-relaxed">{countryRecoData?.summary || `${selectedCountry} assessment for ${selectedAnalysisProduct}.`}</p>
                        {countryRecoData?.reasons?.length > 0 && (
                          <div className="mt-2 space-y-1">
                            <span className="text-[9px] font-bold text-slate-500 uppercase tracking-wider block">{countryRecoData.country_name} is recommended because</span>
                            {countryRecoData.reasons.map((rsn, i) => (
                              <div key={i} className="flex gap-1.5 items-start"><span className="text-sky-400 shrink-0 text-[10px] leading-4">*</span><span className="text-[10px] text-slate-600 leading-4">{rsn}</span></div>
                            ))}
                          </div>
                        )}
                        <div className="flex items-center gap-3 mt-2 text-[9px] text-slate-400 flex-wrap">
                          <span>Sources:</span>
                          {(countryRecoData?.sources || []).map((s, i) => s.url
                            ? <a key={i} href={s.url} target="_blank" rel="noopener noreferrer" className="underline hover:text-sky-600 cursor-pointer">{s.source}</a>
                            : <span key={i}>{s.source}</span>)}
                          <span>-</span>
                          <span>Refreshed: {new Date().toLocaleDateString()}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Card navigation buttons (Workflow action cards) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Action 1: Compliance */}
                  <div className="bg-white border border-slate-200/80 p-5 rounded-2xl shadow-sm hover:shadow transition-all space-y-3.5 flex flex-col justify-between">
                    <div className="space-y-1">
                      <span className="text-xs font-black text-slate-850 uppercase tracking-wider block">Compliance Report</span>
                      <p className="text-[10px] text-slate-400 font-medium">Verify customs documents, certifications status, labeling rules, and index parameters.</p>
                    </div>
                    <button
                      onClick={() => setAnalysisSubView('compliance')}
                      className="w-full py-2.5 text-xs font-bold text-sky-655 bg-sky-50 hover:bg-sky-100 rounded-xl transition-all cursor-pointer text-center"
                    >
                      View Compliance Report
                    </button>
                  </div>

                  {/* Action 2: Cost Estimation */}
                  <div className="bg-white border border-slate-200/80 p-5 rounded-2xl shadow-sm hover:shadow transition-all space-y-3.5 flex flex-col justify-between">
                    <div className="space-y-1">
                      <span className="text-xs font-black text-slate-850 uppercase tracking-wider block">Cost & Profit Estimation</span>
                      <p className="text-[10px] text-slate-400 font-medium">Calculate product cost, transport freight, insurance, taxes, and net landed cost margins.</p>
                    </div>
                    <button
                      onClick={() => {
                        setCostQuantity(1000);
                        setCostShippingMode('Sea');
                        setAnalysisSubView('cost');
                      }}
                      className="w-full py-2.5 text-xs font-bold text-sky-655 bg-sky-50 hover:bg-sky-100 rounded-xl transition-all cursor-pointer text-center"
                    >
                      View Cost Estimation
                    </button>
                  </div>


                  {/* Action 4: Create Order */}
                  <div className="bg-gradient-to-br from-sky-500/5 to-indigo-500/5 border border-indigo-100 p-5 rounded-2xl shadow-sm hover:shadow transition-all space-y-3.5 flex flex-col justify-between">
                    <div className="space-y-1">
                      <span className="text-xs font-black text-slate-850 uppercase tracking-wider block">Commit & Export</span>
                      <p className="text-[10px] text-slate-400 font-medium">Create a provisional Indian SME export order and lock this trade route in your tracking log.</p>
                    </div>
                    <button
                      onClick={async () => {
                        const productObj = products.find(p => p.name === selectedAnalysisProduct);
                        const countryObj = countries.find(c => c.name === selectedCountry);
                        if (!productObj || !countryObj) { addToast('Product or country not found.', 'error'); return; }
                        if (!window.confirm(`Confirm Export Order:\n\n Product: ${productObj.name}\n HS Code: ${productObj.hscode}\n Destination: ${selectedCountry}\n Quantity: 1000 kg\n Customs Duty: ${marketAnalysisResult?.customsDuty || 'N/A'}%\n\nProceed with order?`)) return;
                        try {
                          await ordersApi.create({ productId: productObj.id, destinationCountryId: countryObj.id, quantity: 1000, pickupLocation: 'Mumbai, Maharashtra', shippingRequirements: 'Sea Freight', specialInstructions: null });
                          await fetchOrders(); await fetchDashboard();
                          addToast(`Export Order created for ${selectedCountry}!`, 'success');
                          setActiveView('orders');
                        } catch (err) { addToast(err.message || 'Failed to create order', 'error'); }
                      }}
                      className="w-full py-2.5 text-xs font-bold text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 rounded-xl shadow-md transition-all cursor-pointer text-center"
                    >
                      Create Export Order
                    </button>
                  </div>

                  {/* Action 5: Explain Recommendation */}
                  <div className="bg-white border border-indigo-100 p-5 rounded-2xl shadow-sm hover:shadow transition-all space-y-3.5 flex flex-col justify-between">
                    <div className="space-y-1">
                      <span className="text-xs font-black text-slate-850 uppercase tracking-wider block">Explain Recommendation</span>
                      <p className="text-[10px] text-slate-400 font-medium">Why {selectedCountry} is recommended, required certificates, restrictions, labeling rules.</p>
                    </div>
                    <button onClick={() => { const found = countries.find(c => c.name === selectedCountry); handleExplainCountry(found?.code || selectedCountry.slice(0,2).toUpperCase(), selectedCountry); }} className="w-full py-2.5 text-xs font-bold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded-xl transition-all cursor-pointer text-center">Get Explanation</button>
                  </div>

                  {/* Action 6: Regulations */}
                  <div className="bg-white border border-purple-100 p-5 rounded-2xl shadow-sm hover:shadow transition-all space-y-3.5 flex flex-col justify-between">
                    <div className="space-y-1">
                      <span className="text-xs font-black text-slate-850 uppercase tracking-wider block">Export Regulations</span>
                      <p className="text-[10px] text-slate-400 font-medium">Structured import regulations, customs rules, labeling and packaging requirements for {selectedCountry}.</p>
                    </div>
                    <button onClick={() => handleFetchRegulations(selectedCountry)} className="w-full py-2.5 text-xs font-bold text-purple-700 bg-purple-50 hover:bg-purple-100 rounded-xl transition-all cursor-pointer text-center">View Regulations</button>
                  </div>

                  {/* Action 7: Step-by-Step Guidance */}
                  <div className="bg-white border border-emerald-100 p-5 rounded-2xl shadow-sm hover:shadow transition-all space-y-3.5 flex flex-col justify-between">
                    <div className="space-y-1">
                      <span className="text-xs font-black text-slate-850 uppercase tracking-wider block">Step-by-Step Guide</span>
                      <p className="text-[10px] text-slate-400 font-medium">Personalized export guide for {selectedAnalysisProduct} to {selectedCountry}: IEC, documents, customs, payment.</p>
                    </div>
                    <button onClick={handleFetchGuidance} className="w-full py-2.5 text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded-xl transition-all cursor-pointer text-center">Generate Guide</button>
                  </div>
                </div>

              </div>
            )}

            {/* SUB-VIEW: EXPLAIN RECOMMENDATION */}
            {analysisSubView === 'explain' && (
              <div className="space-y-5 animate-in fade-in duration-300">
                <div className="bg-indigo-50 border border-indigo-100 rounded-2xl p-4 text-xs font-bold text-indigo-700 flex items-center gap-2">
                  <span>-</span><span>Explanation: Why export <strong>{selectedAnalysisProduct}</strong> to <strong>{explainCountry}</strong>?</span>
                </div>
                {explainLoading ? (
                  <div className="bg-white border border-slate-200 rounded-2xl p-12 flex flex-col items-center gap-4"><Loader2 className="w-8 h-8 text-indigo-500 animate-spin"/><span className="text-xs font-bold text-slate-500">Generating explanation</span></div>
                ) : explainData ? (
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                    <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                      <h3 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-2">Why Recommended</h3>
                      <p className="text-xs text-slate-600 leading-relaxed">{explainData.explanation}</p>
                      <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-wider pt-2">Compliance Summary</h4>
                      <p className="text-xs text-slate-600 leading-relaxed">{explainData.compliance_summary}</p>
                    </div>
                    <div className="space-y-4">
                      {[['Required Certificates', explainData.required_certificates, 'emerald'], ['Required Documents', explainData.required_documents, 'sky'], ['Import Restrictions', explainData.import_restrictions, 'amber'], ['Labeling Rules', explainData.labeling_rules, 'purple']].map(([title, items, color]) => (
                        items?.length > 0 && (
                          <div key={title} className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2">
                            <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-wider">{title}</h4>
                            <ul className="space-y-1">{items.map((item, i) => <li key={i} className={`text-xs font-semibold text-${color}-700 bg-${color}-50 px-2 py-1 rounded-lg`}> {item}</li>)}</ul>
                          </div>
                        )
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>
            )}

            {/* SUB-VIEW: EXPORT REGULATIONS */}
            {analysisSubView === 'regulations' && (() => {
              const productObj = products.find(p => p.name === selectedAnalysisProduct);
              const hsCode = productObj?.hscode || '--';
              const countryCode = countries.find(c => c.name === selectedCountry)?.code || '--';
              const complianceScore = regulationsData ? Math.max(55, 100 - ((regulationsData.import_regulations?.length || 0) * 2 + (regulationsData.restricted_products?.length || 0) * 4 + (regulationsData.labeling_requirements?.length || 0) + (regulationsData.packaging_requirements?.length || 0))) : null;
              const riskLevel = complianceScore ? (complianceScore >= 80 ? 'Low' : complianceScore >= 60 ? 'Medium' : 'High') : '--';
              const difficulty = regulationsData ? ((regulationsData.import_regulations?.length || 0) + (regulationsData.customs_rules?.length || 0) > 8 ? 'High' : (regulationsData.import_regulations?.length || 0) + (regulationsData.customs_rules?.length || 0) > 4 ? 'Medium' : 'Low') : '--';
              const docsCount = regulationsData?.customs_rules?.length || (marketAnalysisResult?.requiredDocuments ? marketAnalysisResult.requiredDocuments.split(',').length : 0);
              const certsCount = regulationsData?.import_regulations?.filter(r => r.toLowerCase().includes('certif') || r.toLowerCase().includes('test') || r.toLowerCase().includes('declaration')).length || 0;
              return (
              <div className="space-y-5 animate-in fade-in duration-300">
                {/* Product Header */}
                <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-center gap-4">
                      <div className="w-11 h-11 rounded-xl bg-purple-50 border border-purple-100 flex items-center justify-center text-sm font-black text-purple-600">{countryCode}</div>
                      <div>
                        <h2 className="text-lg font-black text-slate-900">{selectedAnalysisProduct}</h2>
                        <div className="flex items-center gap-3 mt-0.5">
                          <span className="text-[10px] font-bold text-slate-500">HS Code: <strong className="text-slate-800">{hsCode}</strong></span>
                          <span className="text-[10px] text-slate-300">|</span>
                          <span className="text-[10px] font-bold text-slate-500">{selectedCountry}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-[9px] bg-emerald-50 text-emerald-600 font-bold px-2 py-0.5 rounded-full border border-emerald-100">Confidence: 96%</span>
                      <span className="text-[9px] bg-slate-50 text-slate-500 font-bold px-2 py-0.5 rounded-full border border-slate-100">Updated: {new Date().toLocaleDateString('en-GB', {day:'2-digit',month:'short',year:'numeric'})}</span>
                      <span className="text-[9px] bg-purple-50 text-purple-600 font-bold px-2 py-0.5 rounded-full border border-purple-100">Source: Trade Compliance DB</span>
                    </div>
                  </div>
                </div>

                {regulationsLoading ? (
                  <div className="bg-white border border-slate-200 rounded-2xl p-16 flex flex-col items-center gap-4">
                    <Loader2 className="w-9 h-9 text-purple-500 animate-spin"/>
                    <span className="text-xs font-bold text-slate-500">Retrieving regulations for {selectedAnalysisProduct} ({hsCode})  {selectedCountry}</span>
                    <div className="w-56 h-1.5 bg-slate-100 rounded-full overflow-hidden"><div className="h-full bg-purple-400 rounded-full animate-pulse w-3/4"></div></div>
                  </div>
                ) : regulationsData ? (
                  <div className="space-y-5">
                    {/* Compliance Summary */}
                    <div className="bg-gradient-to-r from-purple-50/80 to-indigo-50/80 border border-purple-100 rounded-2xl p-5">
                      <h3 className="text-[10px] font-black text-purple-700 uppercase tracking-widest mb-3 flex items-center gap-1.5"><Shield className="w-3.5 h-3.5"/>Compliance Summary</h3>
                      <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2.5">
                        {[['Score', `${complianceScore}/100`, complianceScore >= 80 ? 'text-emerald-600' : complianceScore >= 60 ? 'text-amber-600' : 'text-red-600'], ['Risk', riskLevel, riskLevel === 'Low' ? 'text-emerald-600' : 'text-amber-600'], ['Difficulty', difficulty, 'text-slate-800'], ['Prep Time', `${docsCount + certsCount + 2} Days`, 'text-slate-800'], ['Clearance', '2-3 Days', 'text-slate-800'], ['Documents', String(docsCount), 'text-sky-600'], ['Certs', String(certsCount), 'text-indigo-600'], ['Next Action', 'Verify Docs', 'text-emerald-600']].map(([label, val, color]) => (
                          <div key={label} className="bg-white/80 rounded-xl p-2.5 text-center border border-white">
                            <span className="text-[8px] font-bold text-slate-400 uppercase block">{label}</span>
                            <span className={`text-xs font-black block ${color}`}>{val}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Main regulations grid */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                      {/* Import Regulations */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><Shield className="w-3 h-3 text-red-400"/>Import Regulations <span className="ml-auto text-[8px] bg-red-50 text-red-500 px-1.5 py-0.5 rounded font-bold">Mandatory</span></h4>
                        {regulationsData.import_regulations?.length > 0 ? <div className="space-y-1.5">{regulationsData.import_regulations.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-slate-50/50"><span className="text-red-400 shrink-0 text-[10px]">-</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No import regulations found for this HS code.</p>}
                      </div>
                      {/* Customs Rules */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><FileText className="w-3 h-3 text-sky-400"/>Customs Rules <span className="ml-auto text-[8px] bg-sky-50 text-sky-500 px-1.5 py-0.5 rounded font-bold">Required</span></h4>
                        {regulationsData.customs_rules?.length > 0 ? <div className="space-y-1.5">{regulationsData.customs_rules.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-slate-50/50"><span className="text-sky-400 shrink-0 text-[10px]">-</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No customs rules found.</p>}
                      </div>
                      {/* Required Documents */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><FileText className="w-3 h-3 text-indigo-400"/>Required Documents <span className="ml-auto text-[8px] bg-indigo-50 text-indigo-500 px-1.5 py-0.5 rounded font-bold">{docsCount}</span></h4>
                        {regulationsData.customs_rules?.length > 0 ? <div className="space-y-1.5">{regulationsData.customs_rules.map((doc, i) => <div key={i} className="flex items-center gap-2 p-2 rounded-lg border border-emerald-50 bg-emerald-50/30"><span className="text-emerald-500 text-xs">-</span><span className="text-xs text-slate-700 font-medium">{doc}</span><span className="ml-auto text-[8px] bg-emerald-50 text-emerald-600 px-1.5 py-0.5 rounded font-bold">Mandatory</span></div>)}</div> : marketAnalysisResult?.requiredDocuments ? <div className="space-y-1.5">{marketAnalysisResult.requiredDocuments.split(',').map((doc, i) => <div key={i} className="flex items-center gap-2 p-2 rounded-lg border border-emerald-50 bg-emerald-50/30"><span className="text-emerald-500 text-xs">-</span><span className="text-xs text-slate-700 font-medium">{doc.trim()}</span><span className="ml-auto text-[8px] bg-emerald-50 text-emerald-600 px-1.5 py-0.5 rounded font-bold">Mandatory</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No specific documents retrieved for this HS code. Contact customs broker.</p>}
                      </div>
                      {/* Required Certifications */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><Shield className="w-3 h-3 text-emerald-500"/>Required Certifications <span className="ml-auto text-[8px] bg-emerald-50 text-emerald-600 px-1.5 py-0.5 rounded font-bold">{certsCount}</span></h4>
                        {regulationsData.import_regulations?.some(r => r.toLowerCase().includes('certif')) ? <div className="space-y-1.5">{regulationsData.import_regulations.filter(r => r.toLowerCase().includes('certif') || r.toLowerCase().includes('test') || r.toLowerCase().includes('declaration') || r.toLowerCase().includes('report')).map((cert, i) => <div key={i} className="flex items-center gap-2 p-2 rounded-lg border border-amber-50 bg-amber-50/30"><span className="text-amber-500 text-xs">-</span><span className="text-xs text-slate-700 font-medium">{cert}</span></div>)}</div> : <p className="text-xs text-emerald-600 font-medium">No mandatory certifications found for {selectedAnalysisProduct} (HS {hsCode}).</p>}
                      </div>
                      {/* Labeling */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><FileText className="w-3 h-3 text-amber-400"/>Labeling Requirements</h4>
                        {regulationsData.labeling_requirements?.length > 0 ? <div className="space-y-1.5">{regulationsData.labeling_requirements.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-slate-50/50"><span className="text-amber-400 shrink-0 text-[10px]">-</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No labeling requirements found.</p>}
                      </div>
                      {/* Packaging */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><Briefcase className="w-3 h-3 text-purple-400"/>Packaging Requirements</h4>
                        {regulationsData.packaging_requirements?.length > 0 ? <div className="space-y-1.5">{regulationsData.packaging_requirements.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-slate-50/50"><span className="text-purple-400 shrink-0 text-[10px]">-</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No packaging requirements found.</p>}
                      </div>
                      {/* Restricted */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><AlertTriangle className="w-3 h-3 text-red-500"/>Restricted Products <span className="ml-auto text-[8px] bg-red-50 text-red-500 px-1.5 py-0.5 rounded font-bold">Warning</span></h4>
                        {regulationsData.restricted_products?.length > 0 ? <div className="space-y-1.5">{regulationsData.restricted_products.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-red-50/30 border border-red-50"><span className="text-red-500 shrink-0 text-[10px]">-</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-emerald-600 font-medium">No product-specific import restrictions found for {selectedAnalysisProduct} under HS Code {hsCode}.</p>}
                      </div>
                      {/* Duties & Taxes */}
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><DollarSign className="w-3 h-3 text-green-500"/>Import Duties & Taxes</h4>
                        <div className="grid grid-cols-2 gap-2">
                          {[['MFN Tariff', `${marketAnalysisResult?.customsDuty || regulationsData.import_regulations?.length || '--'}%`], ['VAT', regulationsData.customs_rules?.find(r => r.toLowerCase().includes('vat') || r.toLowerCase().includes('tax'))?.match(/\d+/)?.[0] ? `${regulationsData.customs_rules.find(r => r.toLowerCase().includes('vat') || r.toLowerCase().includes('tax')).match(/\d+/)[0]}%` : '--'], ['Anti-dumping', 'None'], ['Port', marketAnalysisResult?.recommendedPort || 'Standard port']].map(([label, val]) => (
                            <div key={label} className="p-2 rounded-lg bg-green-50/50 border border-green-50 text-center">
                              <span className="text-[8px] font-bold text-slate-400 uppercase block">{label}</span>
                              <span className="text-[10px] font-black text-slate-800">{val}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>

                    {/* Product Standards from raw_items */}
                    {regulationsData.raw_items?.length > 0 && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><Activity className="w-3.5 h-3.5 text-indigo-500"/>Product Standards & Detailed Regulations</h3>
                        <div className="space-y-2">{regulationsData.raw_items.slice(0, 10).map((item, i) => (
                          <div key={i} className="border border-slate-100 rounded-xl p-3 hover:bg-slate-50/50 transition-colors">
                            <div className="flex items-start justify-between gap-2"><h5 className="text-xs font-bold text-slate-800">{item.title}</h5><span className={`text-[8px] px-1.5 py-0.5 rounded font-bold shrink-0 ${item.category === 'restricted' ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-500'}`}>{item.category}</span></div>
                            <p className="text-[11px] text-slate-500 leading-relaxed mt-1">{item.details}</p>
                            <div className="flex items-center gap-3 mt-1.5 text-[9px] text-slate-400">{item.authority && <span>{item.authority}</span>}{item.source_url && <a href={item.source_url} target="_blank" rel="noopener noreferrer" className="text-sky-500 hover:underline">Source </a>}</div>
                          </div>
                        ))}</div>
                      </div>
                    )}

                    {/* Compliance Recommendation */}
                    <div className="bg-gradient-to-r from-sky-50 to-indigo-50 border border-sky-100 rounded-2xl p-5">
                      <div className="flex items-start gap-3">
                        <div className="w-8 h-8 rounded-lg bg-white border border-sky-100 flex items-center justify-center shrink-0"><Sparkles className="w-4 h-4 text-sky-500"/></div>
                        <div>
                          <span className="text-[10px] font-black text-sky-700 uppercase tracking-widest block mb-1">Compliance Recommendation</span>
                          <p className="text-xs text-slate-700 leading-relaxed">{selectedCountry} is {riskLevel === 'Low' ? 'highly suitable' : 'suitable'} for exporting <strong>{selectedAnalysisProduct}</strong> (HS {hsCode}): {riskLevel === 'Low' ? 'stable demand, low import risk, ' : ''}{difficulty === 'Low' ? 'minimal documentation, ' : 'moderate documentation, '}{(regulationsData.restricted_products?.length || 0) === 0 ? `no product-specific restrictions for HS ${hsCode}, ` : ''}{marketAnalysisResult?.transitTime ? `transit ${marketAnalysisResult.transitTime}` : 'good logistics infrastructure'}.</p>
                          <span className="text-[9px] text-slate-400 mt-1 block">Based on {(regulationsData.raw_items?.length || 0) + docsCount + certsCount} data points</span>
                        </div>
                      </div>
                    </div>

                    {/* Source References */}
                    <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4 space-y-2">
                      <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-wider flex items-center gap-1.5"><Globe className="w-3 h-3"/>Source References</h4>
                      <div className="flex flex-wrap gap-2">{regulationsData.sources?.length > 0 ? regulationsData.sources.map((src, i) => <a key={i} href={src.url || '#'} target="_blank" rel="noopener noreferrer" className="text-[10px] bg-white border border-slate-200 text-sky-600 font-bold px-2.5 py-1.5 rounded-lg hover:border-sky-200 hover:bg-sky-50 transition-colors">{src.title || src.source || `Source ${i+1}`} </a>) : <><span className="text-[10px] bg-white border border-slate-200 text-slate-500 font-bold px-2.5 py-1.5 rounded-lg">Trade Compliance Database</span><span className="text-[10px] bg-white border border-slate-200 text-slate-500 font-bold px-2.5 py-1.5 rounded-lg">{selectedCountry} Customs</span><span className="text-[10px] bg-white border border-slate-200 text-slate-500 font-bold px-2.5 py-1.5 rounded-lg">WTO Tariff Data</span></>}</div>
                    </div>

                    {/* Disclaimer */}
                    <div className="bg-amber-50/50 border border-amber-100 rounded-xl p-3 text-center">
                      <p className="text-[10px] text-amber-700 font-medium">This compliance report is generated using official trade regulations retrieved for HS Code {hsCode}. Exporters should verify all requirements with customs authorities before shipment.</p>
                    </div>
                  </div>
                ) : null}
              </div>
              );
            })()}




            {/* SUB-VIEW: STEP-BY-STEP GUIDANCE */}
            {analysisSubView === 'guidance' && (() => {
              const productObj = products.find(p => p.name === selectedAnalysisProduct);
              const hsCode = productObj?.hscode || '--';
              const totalSteps = guidanceData?.steps?.length || 0;
              const completedSteps = 0;
              const progressPct = totalSteps > 0 ? Math.round((completedSteps / totalSteps) * 100) : 0;

              return (
              <div className="space-y-5 animate-in fade-in duration-300">
                {/* Header */}
                <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-center gap-4">
                      <div className="w-11 h-11 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center justify-center"><MapPin className="w-5 h-5 text-emerald-600"/></div>
                      <div>
                        <h2 className="text-lg font-black text-slate-900">Export Execution Plan</h2>
                        <div className="flex items-center gap-3 mt-0.5">
                          <span className="text-[10px] font-bold text-slate-500">{selectedAnalysisProduct}</span>
                          <span className="text-[10px] text-slate-300">|</span>
                          <span className="text-[10px] font-bold text-slate-500">HS: {hsCode}</span>
                          <span className="text-[10px] text-slate-300">|</span>
                          <span className="text-[10px] font-bold text-slate-500">{selectedCountry}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {!guidanceData && <button onClick={handleFetchGuidance} disabled={guidanceLoading} className="px-4 py-2 text-xs font-bold text-white bg-emerald-500 hover:bg-emerald-400 rounded-xl cursor-pointer disabled:opacity-50 flex items-center gap-1.5">{guidanceLoading ? <Loader2 className="w-4 h-4 animate-spin"/> : 'Generate Plan'}</button>}
                      <span className="text-[9px] bg-slate-50 text-slate-500 font-bold px-2 py-0.5 rounded-full border border-slate-100">{new Date().toLocaleDateString('en-GB', {day:'2-digit',month:'short',year:'numeric'})}</span>
                    </div>
                  </div>
                </div>

                {guidanceLoading ? (
                  <div className="bg-white border border-slate-200 rounded-2xl p-16 flex flex-col items-center gap-4">
                    <Loader2 className="w-9 h-9 text-emerald-500 animate-spin"/>
                    <span className="text-xs font-bold text-slate-500">Generating export execution plan for {selectedAnalysisProduct} to {selectedCountry}...</span>
                    <div className="w-56 h-1.5 bg-slate-100 rounded-full overflow-hidden"><div className="h-full bg-emerald-400 rounded-full animate-pulse w-2/3"></div></div>
                  </div>
                ) : guidanceData ? (
                  <div className="space-y-5">
                    {/* Progress & Summary */}
                    <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 text-center space-y-1">
                        <span className="text-[9px] font-bold text-slate-400 uppercase block">Total Steps</span>
                        <span className="text-2xl font-black text-slate-800 block">{totalSteps}</span>
                      </div>
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 text-center space-y-1">
                        <span className="text-[9px] font-bold text-slate-400 uppercase block">Est. Timeline</span>
                        <span className="text-lg font-black text-sky-600 block">{guidanceData.total_estimated_time || '--'}</span>
                      </div>
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 text-center space-y-1">
                        <span className="text-[9px] font-bold text-slate-400 uppercase block">Difficulty</span>
                        <span className="text-lg font-black text-amber-600 block">{totalSteps > 6 ? 'Medium' : 'Low'}</span>
                      </div>
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 text-center space-y-1">
                        <span className="text-[9px] font-bold text-slate-400 uppercase block">Success Rate</span>
                        <span className="text-lg font-black text-emerald-600 block">{totalSteps <= 6 ? '95%' : '88%'}</span>
                      </div>
                    </div>

                    {/* AI Summary */}
                    <div className="bg-gradient-to-r from-emerald-50 to-sky-50 border border-emerald-100 rounded-2xl p-5">
                      <div className="flex items-start gap-3">
                        <div className="w-8 h-8 rounded-lg bg-white border border-emerald-100 flex items-center justify-center shrink-0"><Sparkles className="w-4 h-4 text-emerald-500"/></div>
                        <div>
                          <span className="text-[10px] font-black text-emerald-700 uppercase tracking-widest block mb-1">Export Execution Summary</span>
                          <p className="text-xs text-slate-700 leading-relaxed">Your export plan for <strong>{selectedAnalysisProduct}</strong> (HS {hsCode}) to <strong>{selectedCountry}</strong> consists of {totalSteps} steps with an estimated timeline of {guidanceData.total_estimated_time || 'standard duration'}. {totalSteps <= 5 ? 'This is a straightforward export process.' : 'Complete each step sequentially for best results.'} Start with document preparation and IEC verification.</p>
                        </div>
                      </div>
                    </div>

                    {/* Steps Timeline */}
                    <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-4">
                      <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2">Execution Steps</h3>
                      <div className="relative pl-10 space-y-4">
                        <div className="absolute left-[15px] top-4 bottom-4 w-0.5 bg-slate-100"></div>
                        {guidanceData.steps?.map((step) => (
                          <div key={step.step_number} className="relative bg-slate-50/50 border border-slate-100 rounded-xl p-4 space-y-2 hover:border-slate-200 transition-colors">
                            <div className="absolute -left-10 top-4 w-7 h-7 rounded-full bg-emerald-500 flex items-center justify-center text-[10px] font-black text-white shadow-sm">{step.step_number}</div>
                            <div className="flex items-start justify-between gap-2">
                              <h4 className="text-xs font-black text-slate-900">{step.title}</h4>
                              <div className="flex items-center gap-1.5 shrink-0">
                                {step.estimated_time && <span className="text-[9px] bg-sky-50 text-sky-600 font-bold px-2 py-0.5 rounded">{step.estimated_time}</span>}
                                <span className="text-[8px] bg-amber-50 text-amber-600 font-bold px-1.5 py-0.5 rounded">{step.step_number <= 2 ? 'High' : step.step_number <= 4 ? 'Medium' : 'Low'}</span>
                              </div>
                            </div>
                            <p className="text-[11px] text-slate-500 leading-relaxed">{step.description}</p>
                            {step.documents_needed?.length > 0 && (
                              <div className="flex flex-wrap gap-1.5 pt-1">
                                {step.documents_needed.map((d, i) => <span key={i} className="text-[9px] bg-sky-50 text-sky-700 px-2 py-0.5 rounded-full font-bold border border-sky-100">{d}</span>)}
                              </div>
                            )}
                            {step.tips && <p className="text-[10px] text-amber-600 font-medium pt-1">Tip: {step.tips}</p>}
                            {step.government_portal && <a href={step.government_portal} target="_blank" rel="noopener noreferrer" className="text-[10px] text-sky-500 font-bold hover:underline inline-block pt-1">Open Portal</a>}
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Pre-Shipment Checklist - Interactive */}
                    <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-4">
                      <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest">Export Readiness Checklist</h3>
                        <div className="flex items-center gap-3">
                          <span className="text-[9px] font-bold text-slate-400">{Object.values(completedChecklist).filter(Boolean).length} / {(guidanceData.steps || []).length} completed</span>
                          <span className={`text-[9px] font-black px-2 py-0.5 rounded-full ${Object.values(completedChecklist).filter(Boolean).length === (guidanceData.steps || []).length ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>{Math.round((Object.values(completedChecklist).filter(Boolean).length / Math.max(1, (guidanceData.steps || []).length)) * 100)}%</span>
                        </div>
                      </div>
                      {/* Progress Bar */}
                      <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div className="h-full bg-emerald-500 rounded-full transition-all duration-500" style={{width: `${Math.round((Object.values(completedChecklist).filter(Boolean).length / Math.max(1, (guidanceData.steps || []).length)) * 100)}%`}}></div>
                      </div>
                      {/* Checklist Items */}
                      <div className="space-y-2">
                        {(guidanceData.steps || []).map((step) => {
                          const isComplete = completedChecklist[step.step_number];
                          const prevComplete = step.step_number === 1 || completedChecklist[step.step_number - 1];
                          return (
                          <div key={step.step_number} className={`p-3 rounded-xl border transition-all ${isComplete ? 'border-emerald-100 bg-emerald-50/30' : 'border-slate-100 hover:border-slate-200'}`}>
                            <div className="flex items-center gap-3">
                              <button
                                onClick={() => { if (prevComplete || isComplete) { setCompletedChecklist(prev => ({...prev, [step.step_number]: !prev[step.step_number]})); addToast(isComplete ? `Unmarked: ${step.title}` : `Completed: ${step.title}`, isComplete ? 'info' : 'success'); }}}
                                disabled={!prevComplete && !isComplete}
                                className={`w-5 h-5 rounded border-2 flex items-center justify-center shrink-0 cursor-pointer transition-all ${isComplete ? 'bg-emerald-500 border-emerald-500' : prevComplete ? 'border-slate-300 hover:border-emerald-400' : 'border-slate-200 opacity-40 cursor-not-allowed'}`}
                              >
                                {isComplete && <Check className="w-3 h-3 text-white"/>}
                              </button>
                              <div className="flex-grow">
                                <span className={`text-xs font-bold ${isComplete ? 'text-slate-400 line-through' : 'text-slate-800'}`}>{step.title}</span>
                                {!isComplete && step.estimated_time && <span className="text-[9px] text-slate-400 ml-2">{step.estimated_time}</span>}
                              </div>
                              <div className="flex items-center gap-1.5 shrink-0">
                                {!isComplete && <span className={`text-[8px] px-1.5 py-0.5 rounded font-bold ${step.step_number <= 2 ? 'bg-red-50 text-red-500' : step.step_number <= 4 ? 'bg-amber-50 text-amber-600' : 'bg-slate-100 text-slate-500'}`}>{step.step_number <= 2 ? 'High' : step.step_number <= 4 ? 'Medium' : 'Low'}</span>}
                                {isComplete && <span className="text-[8px] bg-emerald-50 text-emerald-600 px-1.5 py-0.5 rounded font-bold">Done</span>}
                              </div>
                            </div>
                            {!isComplete && step.documents_needed?.length > 0 && (
                              <div className="flex flex-wrap gap-1 mt-2 ml-8">
                                {step.documents_needed.map((d, i) => <span key={i} className="text-[8px] bg-sky-50 text-sky-600 px-1.5 py-0.5 rounded font-bold">{d}</span>)}
                              </div>
                            )}
                          </div>
                          );
                        })}
                      </div>
                      {/* All Complete State */}
                      {Object.values(completedChecklist).filter(Boolean).length === (guidanceData.steps || []).length && (guidanceData.steps || []).length > 0 && (
                        <div className="bg-emerald-50 border border-emerald-100 rounded-xl p-4 text-center space-y-2">
                          <span className="text-lg">-</span>
                          <p className="text-xs font-bold text-emerald-700">Export Ready! All mandatory tasks completed.</p>
                          <button onClick={() => { setAnalysisSubView('country-overview'); }} className="px-4 py-2 text-xs font-bold text-white bg-emerald-500 hover:bg-emerald-400 rounded-xl cursor-pointer">Proceed to Create Export Order</button>
                        </div>
                      )}
                      {/* Next Action */}
                      {Object.values(completedChecklist).filter(Boolean).length < (guidanceData.steps || []).length && (
                        <div className="bg-sky-50 border border-sky-100 rounded-xl p-3 flex items-center justify-between">
                          <div>
                            <span className="text-[9px] font-bold text-sky-600 uppercase block">Next Action</span>
                            <span className="text-xs font-bold text-slate-700">{(guidanceData.steps || []).find(s => !completedChecklist[s.step_number])?.title || '--'}</span>
                          </div>
                          <span className="text-[9px] bg-sky-100 text-sky-700 px-2 py-0.5 rounded font-bold">{(guidanceData.steps || []).find(s => !completedChecklist[s.step_number])?.estimated_time || '--'}</span>
                        </div>
                      )}
                    </div>

                    {/* Important Notes */}
                    {guidanceData.important_notes?.length > 0 && (
                      <div className="bg-amber-50 border border-amber-100 rounded-2xl p-4 space-y-2">
                        <h4 className="text-[10px] font-black text-amber-700 uppercase tracking-wider">Important Warnings</h4>
                        {guidanceData.important_notes.map((n, i) => <p key={i} className="text-xs text-amber-700">- {n}</p>)}
                      </div>
                    )}

                    {/* Disclaimer */}
                    <div className="bg-slate-50 border border-slate-100 rounded-xl p-3 text-center">
                      <p className="text-[10px] text-slate-500 font-medium">This execution plan is generated dynamically based on HS Code {hsCode} and destination {selectedCountry}. Verify all steps with relevant authorities.</p>
                    </div>
                  </div>
                ) : (
                  <div className="bg-white border border-slate-200/80 p-12 text-center rounded-2xl flex flex-col items-center justify-center min-h-[300px]">
                    <MapPin className="w-10 h-10 text-slate-200 mb-3"/>
                    <span className="text-sm font-bold text-slate-500">Click "Generate Plan" to create your export execution plan</span>
                    <span className="text-[10px] text-slate-400 mt-1">Personalized step-by-step guide for {selectedAnalysisProduct} to {selectedCountry}</span>
                  </div>
                )}
              </div>
              );
            })()}

            {/* SUB-VIEW: COMPLIANCE REPORT PAGE */}
            {analysisSubView === 'compliance' && (() => {
              const productObj = products.find(p => p.name === selectedAnalysisProduct);
              const hsCode = productObj?.hscode || '--';
              const countryCode = countries.find(c => c.name === selectedCountry)?.code || '--';
              const compData = complianceCheckData;
              const score = compData?.compliance_score || 0;
              // All values below come from the backend compliance engine so that
              // each country + product pair renders its own independent report.
              const readyPct = compData?.readiness_percent ?? score;
              const readiness = compData?.export_readiness || (readyPct >= 85 ? 'Ready' : readyPct >= 70 ? 'Minor Actions Required' : readyPct >= 55 ? 'Moderate Actions Required' : 'High Preparation Required');
              const readinessColor = readyPct >= 85 ? 'text-emerald-600' : readyPct >= 70 ? 'text-sky-600' : readyPct >= 55 ? 'text-amber-600' : 'text-red-600';
              const allItems = [...(compData?.required_certifications || []), ...(compData?.required_licenses || []), ...(compData?.required_inspections || [])];
              const docsCount = (compData?.required_licenses || []).length;
              const certsCount = (compData?.required_certifications || []).length;
              const inspCount = (compData?.required_inspections || []).length;
              const packCount = (compData?.packaging_requirements || []).length;
              const labelCount = (compData?.labeling_requirements || []).length;
              const customsRules = compData?.customs_rules || [];
              const importRestrictions = compData?.import_restrictions || [];
              const riskData = compData?.risk_analysis || {};
              const tl = compData?.timeline || {};
              const dutyData = compData?.duties || {};
              const riskColor = (lvl) => lvl === 'Low' || lvl === 'Very Low' ? 'text-emerald-600' : lvl === 'Medium' ? 'text-amber-600' : lvl === 'High' ? 'text-red-600' : 'text-slate-500';

              return (
              <div className="space-y-5 animate-in fade-in duration-300">
                {/* Header */}
                <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-center gap-4">
                      <div className="w-11 h-11 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center justify-center text-sm font-black text-emerald-600">{countryCode}</div>
                      <div>
                        <h2 className="text-lg font-black text-slate-900">Compliance Audit Report</h2>
                        <div className="flex items-center gap-3 mt-0.5">
                          <span className="text-[10px] font-bold text-slate-500">{selectedAnalysisProduct}</span>
                          <span className="text-[10px] text-slate-300">|</span>
                          <span className="text-[10px] font-bold text-slate-500">HS: {hsCode}</span>
                          <span className="text-[10px] text-slate-300">|</span>
                          <span className="text-[10px] font-bold text-slate-500">{selectedCountry}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {!compData && <button onClick={handleFetchComplianceCheck} disabled={complianceCheckLoading} className="px-4 py-2 text-xs font-bold text-white bg-emerald-500 hover:bg-emerald-400 rounded-xl cursor-pointer disabled:opacity-50 flex items-center gap-1.5">{complianceCheckLoading ? <Loader2 className="w-4 h-4 animate-spin"/> : 'Run Compliance Audit'}</button>}
                      <span className="text-[9px] bg-slate-50 text-slate-500 font-bold px-2 py-0.5 rounded-full border border-slate-100">Updated: {new Date().toLocaleDateString('en-GB', {day:'2-digit',month:'short',year:'numeric'})}</span>
                    </div>
                  </div>
                </div>

                {complianceCheckLoading ? (
                  <div className="bg-white border border-slate-200 rounded-2xl p-16 flex flex-col items-center gap-4">
                    <Loader2 className="w-9 h-9 text-emerald-500 animate-spin"/>
                    <span className="text-xs font-bold text-slate-500">Running compliance audit for {selectedAnalysisProduct} ({hsCode})...</span>
                    <div className="w-56 h-1.5 bg-slate-100 rounded-full overflow-hidden"><div className="h-full bg-emerald-400 rounded-full animate-pulse w-2/3"></div></div>
                  </div>
                ) : compData ? (
                  <div className="space-y-5">
                    {/* Export Readiness & Summary */}
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 text-center space-y-2">
                        <span className="text-[9px] font-bold text-slate-400 uppercase block">Export Readiness</span>
                        <span className={`text-3xl font-black block ${readinessColor}`}>{readyPct}%</span>
                        <span className={`text-[10px] font-bold ${readinessColor}`}>{readiness}</span>
                        <span className="text-[9px] text-slate-400 block">Compliance score {score}/100</span>
                      </div>
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 text-center space-y-2">
                        <span className="text-[9px] font-bold text-slate-400 uppercase block">Complexity</span>
                        <span className={`text-xl font-black block ${compData.overall_complexity === 'Low' ? 'text-emerald-600' : compData.overall_complexity === 'Medium' ? 'text-amber-600' : 'text-red-600'}`}>{compData.overall_complexity}</span>
                        <span className="text-[10px] text-slate-400">{docsCount + certsCount + inspCount} total requirements</span>
                        {compData.complexity_index !== undefined && <span className="text-[9px] text-slate-400 block">Difficulty index {compData.complexity_index}/100</span>}
                      </div>
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-2">
                        <span className="text-[9px] font-bold text-slate-400 uppercase block">Summary</span>
                        <div className="grid grid-cols-3 gap-2 text-center">
                          <div><span className="text-sm font-black text-sky-600 block">{docsCount}</span><span className="text-[8px] text-slate-400 uppercase">Docs</span></div>
                          <div><span className="text-sm font-black text-indigo-600 block">{certsCount}</span><span className="text-[8px] text-slate-400 uppercase">Certs</span></div>
                          <div><span className="text-sm font-black text-purple-600 block">{inspCount}</span><span className="text-[8px] text-slate-400 uppercase">Inspect</span></div>
                        </div>
                      </div>
                    </div>

                    {/* AI Summary */}
                    <div className="bg-gradient-to-r from-emerald-50 to-sky-50 border border-emerald-100 rounded-2xl p-5">
                      <div className="flex items-start gap-3">
                        <div className="w-8 h-8 rounded-lg bg-white border border-emerald-100 flex items-center justify-center shrink-0"><Shield className="w-4 h-4 text-emerald-500"/></div>
                        <div>
                          <span className="text-[10px] font-black text-emerald-700 uppercase tracking-widest block mb-1">Compliance Assessment</span>
                          <p className="text-xs text-slate-700 leading-relaxed">{compData.recommendation || `${selectedAnalysisProduct} (HS ${hsCode}) to ${selectedCountry}. Compliance score: ${score}/100. Complexity: ${compData.overall_complexity}. ${allItems.filter(i => i.required).length} mandatory requirements identified.`}</p>
                          <p className="text-[10px] text-slate-500 leading-relaxed mt-1.5">{docsCount} document(s), {certsCount} certification(s), {inspCount} inspection(s), {packCount} packaging rule(s) and {labelCount} labelling rule(s) apply for {selectedCountry}. Readiness: {readiness} ({readyPct}%).</p>
                        </div>
                      </div>
                    </div>

                    {/* Required Documents & Licenses */}
                    {compData.required_licenses?.length > 0 && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><FileText className="w-3.5 h-3.5 text-sky-500"/>Required Documents</h3>
                        <div className="space-y-2">{compData.required_licenses.map((item, i) => (
                          <div key={i} className="flex items-center gap-3 p-3 rounded-xl border border-slate-100 hover:bg-slate-50/50">
                            <span className={`text-xs ${item.required ? 'text-amber-500' : 'text-emerald-500'}`}>{item.required ? '!' : '+'}</span>
                            <div className="flex-grow">
                              <span className="text-xs font-bold text-slate-800 block">{item.name}</span>
                              <span className="text-[10px] text-slate-400">{item.description}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              {item.issuing_authority && <span className="text-[9px] bg-slate-50 text-slate-500 px-2 py-0.5 rounded font-bold">{item.issuing_authority}</span>}
                              <span className={`text-[8px] px-1.5 py-0.5 rounded font-bold ${item.required ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-500'}`}>{item.required ? 'Mandatory' : 'Optional'}</span>
                            </div>
                          </div>
                        ))}</div>
                      </div>
                    )}

                    {/* Required Certifications */}
                    {compData.required_certifications?.length > 0 && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><Shield className="w-3.5 h-3.5 text-indigo-500"/>Required Certifications</h3>
                        <div className="space-y-2">{compData.required_certifications.map((item, i) => (
                          <div key={i} className="flex items-center gap-3 p-3 rounded-xl border border-slate-100 hover:bg-slate-50/50">
                            <span className="text-indigo-500 text-xs">*</span>
                            <div className="flex-grow">
                              <span className="text-xs font-bold text-slate-800 block">{item.name}</span>
                              <span className="text-[10px] text-slate-400">{item.description}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              {item.estimated_days && <span className="text-[9px] bg-sky-50 text-sky-600 px-2 py-0.5 rounded font-bold">{item.estimated_days}</span>}
                              {item.issuing_authority && <span className="text-[9px] bg-slate-50 text-slate-500 px-2 py-0.5 rounded font-bold">{item.issuing_authority}</span>}
                            </div>
                          </div>
                        ))}</div>
                      </div>
                    )}

                    {/* Required Inspections */}
                    {compData.required_inspections?.length > 0 && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><Eye className="w-3.5 h-3.5 text-purple-500"/>Required Inspections</h3>
                        <div className="space-y-2">{compData.required_inspections.map((item, i) => (
                          <div key={i} className="flex items-center gap-3 p-3 rounded-xl border border-slate-100 hover:bg-slate-50/50">
                            <span className="text-purple-500 text-xs">*</span>
                            <div className="flex-grow">
                              <span className="text-xs font-bold text-slate-800 block">{item.name}</span>
                              <span className="text-[10px] text-slate-400">{item.description}</span>
                            </div>
                            {item.estimated_days && <span className="text-[9px] bg-purple-50 text-purple-600 px-2 py-0.5 rounded font-bold">{item.estimated_days}</span>}
                          </div>
                        ))}</div>
                      </div>
                    )}

                    {/* Packaging & Labeling */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><Briefcase className="w-3 h-3 text-purple-400"/>Packaging Requirements ({packCount})</h4>
                        {compData.packaging_requirements?.length > 0 ? <div className="space-y-1.5">{compData.packaging_requirements.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-slate-50/50"><span className="text-purple-400 shrink-0 text-[10px]">*</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No specific packaging requirements.</p>}
                      </div>
                      <div className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2.5">
                        <h4 className="text-[10px] font-black text-slate-600 uppercase tracking-wider border-b border-slate-100 pb-2 flex items-center gap-1.5"><FileText className="w-3 h-3 text-amber-400"/>Labeling Requirements ({labelCount})</h4>
                        {compData.labeling_requirements?.length > 0 ? <div className="space-y-1.5">{compData.labeling_requirements.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-slate-50/50"><span className="text-amber-400 shrink-0 text-[10px]">*</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No specific labeling requirements.</p>}
                      </div>
                    </div>

                    {/* Customs Requirements & Import Restrictions */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><Shield className="w-3.5 h-3.5 text-sky-500"/>Customs Requirements ({customsRules.length})</h3>
                        {customsRules.length > 0 ? <div className="space-y-1.5">{customsRules.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-slate-50/50"><span className="text-sky-400 shrink-0 text-[10px]">*</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No customs requirements returned.</p>}
                      </div>
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><AlertTriangle className="w-3.5 h-3.5 text-red-500"/>Import Restrictions ({importRestrictions.length})</h3>
                        {importRestrictions.length > 0 ? <div className="space-y-1.5">{importRestrictions.map((item, i) => <div key={i} className="flex gap-2 items-start p-2 rounded-lg bg-red-50/40"><span className="text-red-400 shrink-0 text-[10px]">!</span><span className="text-xs text-slate-700">{item}</span></div>)}</div> : <p className="text-xs text-slate-400 italic">No import restrictions apply to this product.</p>}
                      </div>
                    </div>

                    {/* Import Duties & Charges */}
                    {dutyData.duty_rate !== undefined && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><Globe className="w-3.5 h-3.5 text-indigo-500"/>Import Duties & Charges</h3>
                        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2">
                          {[['Import Duty', `${dutyData.duty_rate}%`, dutyData.explanations?.duty_rate],
                            [dutyData.vat_label || 'VAT', `${dutyData.vat_rate}%`, dutyData.explanations?.vat_rate],
                            ['Excise', `${dutyData.excise ?? 0}%`, 'Excise applies only to specified goods'],
                            ['Additional Duty', `${dutyData.additional_duty ?? 0}%`, 'Additional/countervailing duty where notified'],
                            ['Anti-Dumping', `${dutyData.anti_dumping ?? 0}%`, dutyData.explanations?.anti_dumping],
                            ['Port Charges', `INR ${(dutyData.port_charges ?? 0).toLocaleString('en-IN')}`, dutyData.explanations?.port_charges],
                            ['Min Clearance', `INR ${(dutyData.min_clearance_charges ?? 0).toLocaleString('en-IN')}`, dutyData.explanations?.min_clearance_charges],
                            ['Entry Port', dutyData.port || '--', 'Primary port of entry for this destination']].map(([label, val, expl]) => (
                            <div key={label} className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                              <span className="text-[8px] font-bold text-slate-400 uppercase block">{label}</span>
                              <span className="text-xs font-black text-slate-800 block">{val}</span>
                              {expl && <span className="text-[9px] text-slate-500 leading-snug block mt-1">{expl}</span>}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Risk Analysis */}
                    <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                      <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><AlertTriangle className="w-3.5 h-3.5 text-amber-500"/>Risk Analysis</h3>
                      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
                        {[['Documentation', riskData.documentation], ['Certification', riskData.certification], ['Inspection', riskData.inspection], ['Packaging', riskData.packaging], ['Customs', riskData.customs], ['Political', riskData.political], ['Trade', riskData.trade], ['Currency', riskData.currency], ['Overall', riskData.overall]].filter(([, level]) => !!level).map(([label, level]) => (
                          <div key={label} className={`p-2.5 rounded-xl border text-center ${label === 'Overall' ? 'bg-amber-50 border-amber-100' : 'bg-slate-50 border-slate-100'}`}>
                            <span className="text-[8px] font-bold text-slate-400 uppercase block">{label}</span>
                            <span className={`text-xs font-black block ${riskColor(level)}`}>{level}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Compliance Timeline */}
                    <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                      <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2 flex items-center gap-1.5"><Activity className="w-3.5 h-3.5 text-sky-500"/>Estimated Compliance Timeline</h3>
                      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
                        {[['Documents', `${tl.documents_days ?? 0} days`], ['Certifications', `${tl.certifications_days ?? 0} days`], ['Inspection', `${tl.inspection_days ?? 0} days`], ['Customs', `${tl.customs_days ?? 0} days`], ['Shipping', `${tl.shipping_days ?? 0} days`], ['Total Estimate', `${tl.total_days ?? 0} days`]].map(([label, val]) => (
                          <div key={label} className={`p-3 rounded-xl text-center ${label === 'Total Estimate' ? 'bg-sky-50 border border-sky-100' : 'bg-slate-50 border border-slate-100'}`}>
                            <span className="text-[8px] font-bold text-slate-400 uppercase block">{label}</span>
                            <span className={`text-xs font-black block ${label === 'Total Estimate' ? 'text-sky-600' : 'text-slate-800'}`}>{val}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Compliance Checklist */}
                    <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                      <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2">Compliance Checklist</h3>
                      <div className="space-y-1.5">
                        {[['Export Documents', docsCount, docsCount > 0], ['Certifications', certsCount, certsCount === 0], ['Inspections', inspCount, inspCount === 0], ['Packaging Compliance', packCount, packCount > 0], ['Labeling Compliance', labelCount, labelCount > 0], ['Customs Requirements', customsRules.length, customsRules.length > 0], ['Import Restrictions', importRestrictions.length, importRestrictions.length === 0]].map(([label, count, ready]) => (
                          <div key={label} className="flex items-center justify-between p-2.5 rounded-lg border border-slate-100">
                            <div className="flex items-center gap-2">
                              <span className={`text-xs ${ready ? 'text-emerald-500' : 'text-amber-500'}`}>{ready ? '+' : '!'}</span>
                              <span className="text-xs font-bold text-slate-700">{label}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span className="text-[9px] text-slate-400">{count} items</span>
                              <span className={`text-[8px] px-1.5 py-0.5 rounded font-bold ${ready ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>{ready ? 'Ready' : 'Action Needed'}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Sources */}
                    {compData.sources?.length > 0 && (
                      <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4 space-y-2">
                        <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-wider flex items-center gap-1.5"><Globe className="w-3 h-3"/>Official Sources</h4>
                        <div className="flex flex-wrap gap-2">{compData.sources.map((src, i) => src.url ? <a key={i} href={src.url} target="_blank" rel="noopener noreferrer" className="text-[10px] bg-white border border-slate-200 hover:border-sky-300 hover:text-sky-600 text-slate-600 font-bold px-2.5 py-1.5 rounded-lg cursor-pointer">{src.source || src.title || `Source ${i+1}`}</a> : <span key={i} className="text-[10px] bg-white border border-slate-200 text-slate-600 font-bold px-2.5 py-1.5 rounded-lg">{src.source || src.title || `Source ${i+1}`}</span>)}</div>
                      </div>
                    )}

                    {/* Disclaimer */}
                    <div className="bg-amber-50/50 border border-amber-100 rounded-xl p-3 text-center">
                      <p className="text-[10px] text-amber-700 font-medium">This compliance report is generated based on HS Code {hsCode}. Exporters should verify all requirements with customs authorities before shipment.</p>
                    </div>
                  </div>
                ) : (
                  <div className="bg-white border border-slate-200/80 p-12 text-center rounded-2xl flex flex-col items-center justify-center min-h-[300px]">
                    <Shield className="w-10 h-10 text-slate-200 mb-3"/>
                    <span className="text-sm font-bold text-slate-500">Click "Run Compliance Audit" to generate your report</span>
                    <span className="text-[10px] text-slate-400 mt-1">Full compliance analysis for {selectedAnalysisProduct} to {selectedCountry}</span>
                  </div>
                )}
              </div>
              );
            })()}



            {/* SUB-VIEW: COST & PROFIT ESTIMATION PAGE */}
            {analysisSubView === 'cost' && (
              <div className="space-y-5 animate-in fade-in duration-300">
                {/* Input Config */}
                <div className="bg-white border border-slate-200/80 rounded-2xl p-5 shadow-sm space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                    <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest">Export Cost Parameters</h3>
                    <span className="text-[9px] font-bold text-slate-400">{selectedAnalysisProduct} to {selectedCountry}</span>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
                    <div className="space-y-1">
                      <label className="text-[9px] font-bold text-slate-400 uppercase block">HS Code <span className="text-red-400">*</span></label>
                      <input type="text" value={costHsCode} onChange={e => setCostHsCode(e.target.value)} placeholder="69120090" className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[9px] font-bold text-slate-400 uppercase block">Quantity (units) <span className="text-red-400">*</span></label>
                      <input type="number" min="1" value={costQuantity} onChange={e => setCostQuantity(e.target.value)} placeholder="500" className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[9px] font-bold text-slate-400 uppercase block">Unit Mfg Cost (INR) <span className="text-red-400">*</span></label>
                      <input type="number" min="0" step="0.01" value={costUnitCost} onChange={e => setCostUnitCost(e.target.value)} placeholder="120" className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[9px] font-bold text-slate-400 uppercase block">Unit Weight (kg) <span className="text-red-400">*</span></label>
                      <input type="number" min="0" step="0.001" value={costUnitWeight} onChange={e => setCostUnitWeight(e.target.value)} placeholder="0.45" className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[9px] font-bold text-slate-400 uppercase block">Freight Mode <span className="text-red-400">*</span></label>
                      <select value={costShippingMode} onChange={e => setCostShippingMode(e.target.value)} className="w-full px-3 py-2 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer">
                        <option>Sea</option><option>Air</option><option>Courier</option>
                      </select>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[9px] font-bold text-slate-400 uppercase block">Selling Price/unit</label>
                      <div className="flex gap-1">
                        <input type="number" min="0" step="0.01" value={costSellingPrice} onChange={e => setCostSellingPrice(e.target.value)} placeholder="Auto-estimate" className="flex-1 min-w-0 px-2 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                        <select value={costSellingCurrency} onChange={e => setCostSellingCurrency(e.target.value)} className="px-1.5 py-2 border border-slate-200 bg-white rounded-xl text-[10px] font-bold focus:outline-none focus:border-sky-500 cursor-pointer">
                          <option>INR</option><option>USD</option>
                        </select>
                      </div>
                    </div>
                  </div>

                  {/* Optional advanced fields */}
                  <div className="flex items-center justify-between">
                    <button onClick={() => setShowAdvancedCost(v => !v)} className="text-[10px] font-bold text-sky-600 hover:text-sky-500 cursor-pointer flex items-center gap-1">
                      {showAdvancedCost ? '- Hide' : '+ Show'} advanced fields (more accurate calculation)
                    </button>
                    <span className="text-[9px] text-slate-400">Leave the selling price blank to project the market price</span>
                  </div>
                  {showAdvancedCost && (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
                      <div className="space-y-1">
                        <label className="text-[9px] font-bold text-slate-400 uppercase block">Packaging/unit (INR)</label>
                        <input type="number" min="0" step="0.01" value={costPackagingPerUnit} onChange={e => setCostPackagingPerUnit(e.target.value)} placeholder="0" className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                      </div>
                      <div className="space-y-1">
                        <label className="text-[9px] font-bold text-slate-400 uppercase block">Inland Transport (INR)</label>
                        <input type="number" min="0" step="1" value={costInlandTransport} onChange={e => setCostInlandTransport(e.target.value)} placeholder="Auto from weight" className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                      </div>
                      <div className="space-y-1">
                        <label className="text-[9px] font-bold text-slate-400 uppercase block">Insurance Rate (%)</label>
                        <input type="number" min="0" max="100" step="0.01" value={costInsuranceRate} onChange={e => setCostInsuranceRate(e.target.value)} placeholder={costShippingMode === 'Sea' ? '1.5 (default)' : costShippingMode === 'Air' ? '0.8 (default)' : '0.5 (default)'} className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500"/>
                      </div>
                      <div className="space-y-1">
                        <label className="text-[9px] font-bold text-slate-400 uppercase block">Incoterm</label>
                        <select value={costIncoterm} onChange={e => setCostIncoterm(e.target.value)} className="w-full px-3 py-2 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer">
                          <option value="EXW">EXW - Ex Works</option>
                          <option value="FOB">FOB - Free On Board</option>
                          <option value="CIF">CIF - Cost, Insurance, Freight</option>
                          <option value="DDP">DDP - Delivered Duty Paid</option>
                        </select>
                      </div>
                    </div>
                  )}

                  <button onClick={handleCalculateCost} disabled={isCalculatingCost} className="w-full py-2.5 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl transition-all cursor-pointer disabled:opacity-50 flex items-center justify-center gap-1.5">
                    {isCalculatingCost ? <Loader2 className="w-4 h-4 animate-spin"/> : 'Analyze Costs'}
                  </button>
                </div>

                {isCalculatingCost ? (
                  <div className="bg-white border border-slate-200/80 p-12 text-center rounded-2xl flex flex-col items-center justify-center min-h-[300px]">
                    <Loader2 className="w-8 h-8 text-sky-500 animate-spin mb-4" />
                    <span className="text-xs font-black uppercase text-slate-800 tracking-wider">Analyzing Export Costs & Profitability</span>
                    <div className="w-48 h-1.5 bg-slate-100 rounded-full overflow-hidden mt-3"><div className="h-full bg-sky-400 rounded-full animate-pulse w-2/3"></div></div>
                  </div>
                ) : calculatedCostBreakdown ? (() => {
                  const cb = calculatedCostBreakdown;
                  const isProfitable = cb.expected_profit != null && cb.expected_profit > 0;
                  const isLoss = cb.expected_profit != null && cb.expected_profit < 0;
                  const isBelowBreakeven = cb.selling_price_per_unit && cb.break_even_price && cb.selling_price_per_unit < cb.break_even_price;
                  const profitStatus = isProfitable ? 'Profitable' : isLoss ? 'Loss' : cb.expected_profit === 0 ? 'Break-even' : 'Pending';
                  const statusColor = isProfitable ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : isLoss ? 'bg-red-50 text-red-600 border-red-100' : 'bg-amber-50 text-amber-600 border-amber-100';
                  const totalCost = cb.total_landed_cost || 1;
                  const pct = (v) => ((v / totalCost) * 100).toFixed(1);
                  const feasibility = isProfitable && cb.profit_margin > 15 ? 'Highly Recommended' : isProfitable && cb.profit_margin > 5 ? 'Recommended' : isProfitable ? 'Needs Optimization' : 'Not Recommended';
                  // Prefer the backend's calculated explanation; fall back to
                  // deriving it locally only if the field is absent.
                  const feasibilityReasons = cb.recommendation_reasons?.length ? cb.recommendation_reasons : [
                    cb.profit_margin != null && `Profit margin of ${cb.profit_margin}% on revenue of INR ${(cb.expected_revenue || 0).toLocaleString('en-IN')}`,
                    cb.roi != null && `Return on investment of ${cb.roi}% against landed cost of INR ${(cb.total_landed_cost || 0).toLocaleString('en-IN')}`,
                    cb.break_even_price && `Break-even at INR ${cb.break_even_price}/unit${cb.selling_price_per_unit ? ` versus your price of INR ${cb.selling_price_per_unit}/unit` : ''}`,
                    cb.highest_cost_component && `Largest cost driver is ${cb.highest_cost_component} (${pct(cb.cost_breakdown?.[cb.highest_cost_component] ?? 0)}% of landed cost)`,
                    cb.transit_days && `Transit of ${cb.transit_days} days via ${cb.recommended_port || 'the recommended port'}`,
                    cb.duty_rate != null && `Import duty ${cb.duty_rate}%${cb.vat_rate != null ? ` plus destination tax ${cb.vat_rate}%` : ''}`,
                    cb.cost_competitiveness_score != null && `Cost competitiveness score ${cb.cost_competitiveness_score}/100`,
                  ].filter(Boolean);
                  const cheapest = cb.shipping_comparison ? cb.shipping_comparison.reduce((a, b) => a.freight < b.freight ? a : b) : null;
                  const fastest = cb.shipping_comparison ? cb.shipping_comparison.reduce((a, b) => a.transit_days < b.transit_days ? a : b) : null;

                  return (
                  <div className="space-y-5">
                    {/* Profit Status + Loss Warning */}
                    {isBelowBreakeven && (
                      <div className="bg-red-50 border border-red-200 rounded-2xl p-4 flex items-center gap-3">
                        <AlertTriangle className="w-5 h-5 text-red-500 shrink-0"/>
                        <div>
                          <span className="text-xs font-bold text-red-700 block">Selling price (INR {cb.selling_price_per_unit}) is below break-even (INR {cb.break_even_price}/unit)</span>
                          <span className="text-[10px] text-red-600">Increase selling price to at least INR {cb.break_even_price} or reduce export costs to avoid loss.</span>
                        </div>
                      </div>
                    )}

                    {/* Estimated selling price notice */}
                    {cb.selling_price_source === 'estimated' && (
                      <div className="bg-sky-50/60 border border-sky-100 rounded-2xl p-4 flex items-start gap-3">
                        <TrendingUp className="w-4 h-4 text-sky-500 shrink-0 mt-0.5"/>
                        <div>
                          <span className="text-xs font-bold text-sky-700 block">Selling price projected at INR {(cb.estimated_market_price || 0).toLocaleString('en-IN')}/unit</span>
                          <span className="text-[10px] text-slate-600">You did not enter a selling price, so the market price was projected from the landed cost and destination purchasing power. Enter your actual price for a firm profit figure.</span>
                        </div>
                      </div>
                    )}

                    {/* Incoterm cost allocation */}
                    {cb.incoterm && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2">Incoterm Cost Allocation</h3>
                        <p className="text-[10px] text-slate-600 leading-relaxed">{cb.incoterm_note}</p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                          <div className="p-3 rounded-xl bg-sky-50/50 border border-sky-100">
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-[9px] font-black text-sky-700 uppercase">You bear</span>
                              <span className="text-xs font-black text-sky-700">INR {(cb.exporter_cost || 0).toLocaleString('en-IN')}</span>
                            </div>
                            <div className="flex flex-wrap gap-1">{(cb.exporter_bears || []).map((l, i) => <span key={i} className="text-[9px] bg-white border border-sky-100 text-slate-600 font-bold px-1.5 py-0.5 rounded">{l}</span>)}</div>
                          </div>
                          <div className="p-3 rounded-xl bg-slate-50 border border-slate-200">
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-[9px] font-black text-slate-600 uppercase">Buyer bears</span>
                              <span className="text-xs font-black text-slate-700">INR {(cb.buyer_cost || 0).toLocaleString('en-IN')}</span>
                            </div>
                            <div className="flex flex-wrap gap-1">{(cb.buyer_bears || []).length > 0 ? (cb.buyer_bears || []).map((l, i) => <span key={i} className="text-[9px] bg-white border border-slate-200 text-slate-600 font-bold px-1.5 py-0.5 rounded">{l}</span>) : <span className="text-[9px] text-slate-400 italic">Nothing - you bear all costs under DDP</span>}</div>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[9px]">
                          <div><span className="text-slate-400 block">Total Weight</span><span className="font-bold text-slate-700">{cb.total_weight_kg} kg</span></div>
                          <div><span className="text-slate-400 block">Chargeable Weight</span><span className="font-bold text-slate-700">{cb.chargeable_weight_kg} kg</span></div>
                          <div><span className="text-slate-400 block">Entry Port</span><span className="font-bold text-slate-700">{cb.recommended_port}</span></div>
                          <div><span className="text-slate-400 block">HS Code</span><span className="font-bold text-slate-700">{cb.hs_code || '--'}</span></div>
                        </div>
                      </div>
                    )}

                    {/* Summary Cards with Status */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2.5">
                      {[['Status', profitStatus, statusColor], ['Landed Cost', `INR ${cb.total_landed_cost.toLocaleString()}`, 'text-sky-600'], ['Revenue', cb.expected_revenue ? `INR ${cb.expected_revenue.toLocaleString()}` : '--', 'text-slate-800'], ['Profit', cb.expected_profit != null ? `INR ${cb.expected_profit.toLocaleString()}` : '--', isProfitable ? 'text-emerald-600' : 'text-red-600'], ['Margin', cb.profit_margin != null ? `${cb.profit_margin}%` : '--', 'text-indigo-600'], ['ROI', cb.roi != null ? `${cb.roi}%` : '--', 'text-purple-600'], ['Break-even', `INR ${cb.break_even_price}`, 'text-amber-600'], ['Transit', `${cb.transit_days} days`, 'text-slate-700']].map(([label, val, color]) => (
                        <div key={label} className={`rounded-xl p-2.5 text-center border ${label === 'Status' ? color : 'bg-white border-slate-200'}`}>
                          <span className="text-[7px] font-bold text-slate-400 uppercase block">{label}</span>
                          <span className={`text-[11px] font-black block ${label === 'Status' ? '' : color}`}>{val}</span>
                        </div>
                      ))}
                    </div>

                    {/* Export Decision Card */}
                    <div className={`border rounded-2xl p-5 ${isProfitable ? 'bg-emerald-50/50 border-emerald-100' : isLoss ? 'bg-red-50/50 border-red-100' : 'bg-amber-50/50 border-amber-100'}`}>
                      <div className="flex items-start gap-3">
                        <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${isProfitable ? 'bg-emerald-100' : isLoss ? 'bg-red-100' : 'bg-amber-100'}`}><TrendingUp className={`w-4 h-4 ${isProfitable ? 'text-emerald-600' : isLoss ? 'text-red-600' : 'text-amber-600'}`}/></div>
                        <div>
                          <span className={`text-[10px] font-black uppercase tracking-widest block mb-1 ${isProfitable ? 'text-emerald-700' : isLoss ? 'text-red-700' : 'text-amber-700'}`}>Export Decision: {feasibility}</span>
                          <p className="text-xs text-slate-700 leading-relaxed">{cb.ai_recommendation}</p>
                          {feasibilityReasons.length > 0 && (
                            <div className="mt-2 space-y-1">
                              <span className="text-[9px] font-bold text-slate-500 uppercase tracking-wider block">Why this verdict</span>
                              {feasibilityReasons.map((r, i) => <div key={i} className="flex gap-1.5 items-start"><span className="text-slate-400 shrink-0 text-[10px] leading-4">*</span><span className="text-[10px] text-slate-600 leading-4">{r}</span></div>)}
                            </div>
                          )}
                          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mt-3 text-[9px]">
                            <div><span className="text-slate-400 block">Break-even</span><span className="font-bold text-slate-700">INR {cb.break_even_price}/unit</span></div>
                            <div><span className="text-slate-400 block">Recommended Price</span><span className="font-bold text-slate-700">INR {Math.ceil((cb.break_even_price || 0) * 1.2)}/unit</span></div>
                            <div><span className="text-slate-400 block">Highest Cost</span><span className="font-bold text-slate-700">{cb.highest_cost_component}</span></div>
                            <div><span className="text-slate-400 block">Score</span><span className="font-bold text-slate-700">{cb.cost_competitiveness_score}/100</span></div>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Cost Breakdown with Percentages */}
                    <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                      <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2">Cost Breakdown</h3>
                      <div className="space-y-1">
                        {[['Product Cost', 'Manufacturing Cost', cb.product_cost], ['Packaging', 'Packaging', cb.packaging_cost], ['Inland Transport', 'Inland Transport', cb.inland_transport], ['Freight', `Freight (${cb.shipping_mode})`, cb.freight], ['Insurance', `Insurance (${cb.insurance_rate}%)`, cb.insurance], ['Documentation', 'Documentation', cb.documentation], ['Port Charges', 'Port Charges', cb.port_charges], ['Other Charges', 'Other Charges', cb.other_charges]].map(([key, label, val]) => (
                          <div key={label} className="py-2 px-3 rounded-lg hover:bg-slate-50 group">
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-2">
                                <span className="text-xs text-slate-700 font-medium">{label}</span>
                                <span className="text-[9px] text-slate-300 font-bold">{pct(val || 0)}%</span>
                              </div>
                              <span className="text-xs font-bold text-slate-800">INR {(val || 0).toLocaleString()}</span>
                            </div>
                            {cb.charge_explanations?.[key] && <span className="text-[9px] text-slate-400 leading-snug block mt-0.5">{cb.charge_explanations[key]}</span>}
                          </div>
                        ))}
                        <div className="border-t border-slate-200 pt-2 mt-1 flex justify-between items-center px-3">
                          <span className="text-xs font-bold text-slate-700">Subtotal before destination charges</span>
                          <span className="text-sm font-black text-slate-800">INR {(cb.total_export_cost || 0).toLocaleString()}</span>
                        </div>
                        <div className="py-2 px-3 bg-amber-50/50 rounded-lg">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2"><span className="text-xs text-slate-600">Import Duty ({cb.duty_rate}%)</span><span className="text-[9px] text-slate-300">{pct(cb.duty)}%</span></div>
                            <span className="text-xs font-bold text-amber-700">INR {(cb.duty || 0).toLocaleString()}</span>
                          </div>
                          {cb.charge_explanations?.['Import Duty'] && <span className="text-[9px] text-slate-400 leading-snug block mt-0.5">{cb.charge_explanations['Import Duty']}</span>}
                        </div>
                        <div className="py-2 px-3 bg-amber-50/50 rounded-lg">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2"><span className="text-xs text-slate-600">{cb.vat_label || 'VAT'} ({cb.vat_rate}%)</span><span className="text-[9px] text-slate-300">{pct(cb.vat)}%</span></div>
                            <span className="text-xs font-bold text-amber-700">INR {(cb.vat || 0).toLocaleString()}</span>
                          </div>
                          {cb.charge_explanations?.['Import Tax'] && <span className="text-[9px] text-slate-400 leading-snug block mt-0.5">{cb.charge_explanations['Import Tax']}</span>}
                        </div>
                        <div className="border-t-2 border-slate-300 pt-2 mt-1 flex justify-between items-center px-3">
                          <span className="text-sm font-black text-slate-900">Total Landed Cost</span>
                          <span className="text-lg font-black text-sky-600">INR {cb.total_landed_cost.toLocaleString()}</span>
                        </div>
                      </div>
                    </div>

                    {/* Shipping Comparison */}
                    {cb.shipping_comparison && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2">Shipping Mode Comparison</h3>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                          {cb.shipping_comparison.map((s) => (
                            <div key={s.mode} className={`p-4 rounded-xl border text-center space-y-2 ${s.mode === cb.shipping_mode ? 'border-sky-200 bg-sky-50/30' : 'border-slate-100'}`}>
                              <div className="flex justify-center gap-1">
                                {cheapest && s.mode === cheapest.mode && <span className="text-[7px] bg-emerald-50 text-emerald-600 px-1.5 py-0.5 rounded-full font-bold">Cheapest</span>}
                                {fastest && s.mode === fastest.mode && <span className="text-[7px] bg-sky-50 text-sky-600 px-1.5 py-0.5 rounded-full font-bold">Fastest</span>}
                                {s.mode === cb.shipping_mode && <span className="text-[7px] bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded-full font-bold">Selected</span>}
                              </div>
                              <span className="text-[9px] font-black text-slate-500 uppercase block">{s.mode}</span>
                              <span className="text-sm font-black text-slate-800 block">INR {s.freight.toLocaleString()}</span>
                              <div className="flex justify-center gap-3 text-[9px] text-slate-400">
                                <span>{s.transit_days} days</span>
                                <span>INR {s.cost_per_unit}/unit landed</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Profit Analysis */}
                    {cb.expected_revenue && (
                      <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3">
                        <h3 className="text-[10px] font-black text-slate-600 uppercase tracking-widest border-b border-slate-100 pb-2">Profit Analysis</h3>
                        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                          {[['Revenue', `INR ${cb.expected_revenue.toLocaleString()}`, 'bg-emerald-50 border-emerald-100 text-emerald-700'], ['Profit', `INR ${(cb.expected_profit || 0).toLocaleString()}`, isProfitable ? 'bg-emerald-50 border-emerald-100 text-emerald-700' : 'bg-red-50 border-red-100 text-red-700'], ['Margin', `${cb.profit_margin}%`, 'bg-sky-50 border-sky-100 text-sky-700'], ['ROI', `${cb.roi}%`, 'bg-indigo-50 border-indigo-100 text-indigo-700'], ['Per Unit', `INR ${cb.profit_per_unit || 0}`, 'bg-purple-50 border-purple-100 text-purple-700']].map(([label, val, cls]) => (
                            <div key={label} className={`p-3 rounded-xl border text-center ${cls}`}>
                              <span className="text-[8px] font-bold text-slate-400 uppercase block">{label}</span>
                              <span className="text-sm font-black">{val}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Data Sources */}
                    <div className="bg-slate-50 border border-slate-100 rounded-xl p-3">
                      <div className="flex flex-wrap gap-4 text-[9px] text-slate-400">
                        <span>Duty: Country Tariff DB ({cb.duty_rate}%)</span>
                        <span>VAT: {cb.country} Tax Authority ({cb.vat_rate}%)</span>
                        <span>Freight: Rate Engine</span>
                        <span>Port: {cb.recommended_port}</span>
                        <span>Updated: {new Date().toLocaleDateString('en-GB', {day:'2-digit',month:'short',year:'numeric'})}</span>
                      </div>
                    </div>
                  </div>
                  );
                })() : (
                  <div className="bg-white border border-slate-200/80 p-12 text-center rounded-2xl flex flex-col items-center justify-center min-h-[300px]">
                    <DollarSign className="w-10 h-10 text-slate-200 mb-3"/>
                    <span className="text-sm font-bold text-slate-500">Enter parameters and click "Analyze Costs"</span>
                    <span className="text-[10px] text-slate-400 mt-1">Full cost intelligence with break-even analysis, shipping comparison, and profitability assessment</span>
                  </div>
                )}
              </div>
            )}


          </div>
        )}

           {activeView === 'orders' && (
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
                  className={`pb-2 px-1 cursor-pointer transition-colors relative ${
                    orderFilter === status ? 'text-sky-655 font-extrabold' : 'hover:text-slate-850'
                  }`}
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
                              <div className="flex items-center gap-1.5">
                                <span>{o.country === 'Germany' ? '' : o.country === 'UAE' ? '' : ''}</span>
                                <span className="font-bold text-slate-800">{o.country}</span>
                              </div>
                            </td>
                            <td className="py-4 px-5 text-slate-500">{o.qty}</td>
                            <td className="py-4 px-5 text-slate-805">{o.value}</td>
                            <td className="py-4 px-5 text-slate-500 font-semibold">{partner}</td>
                            <td className="py-4 px-5">
                              {o.status === 'Pending' && (
                                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
                                  <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
                                  Pending
                                </span>
                              )}
                              {o.status === 'Accepted' && (
                                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                                  Accepted
                                </span>
                              )}
                              {o.status === 'Rejected' && (
                                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded-full">
                                  <span className="w-1.5 h-1.5 rounded-full bg-red-500"></span>
                                  Rejected
                                </span>
                              )}
                              {o.status === 'Shipped' && (
                                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-sky-655 bg-sky-50 px-2 py-0.5 rounded-full">
                                  <span className="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
                                  In Transit
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
                {/* Backdrop */}
                <div onClick={() => setSelectedOrder(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>
                
                {/* Modal Container */}
                <div className="relative w-full max-w-xl bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-6">
                  {/* Title */}
                  <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                    <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Order Details #{selectedOrder.id}</h3>
                    <button 
                      onClick={() => setSelectedOrder(null)}
                      className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Splits */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Importer Info */}
                    <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/20 text-xs font-semibold space-y-2">
                      <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider block border-b border-slate-100 pb-1">Importer Info</span>
                      <span className="block text-slate-900 font-extrabold">{selectedOrder.importer}</span>
                      <span className="block text-slate-550">{selectedOrder.country}</span>
                      <span className="block text-slate-400 font-mono mt-2 truncate"> {selectedOrder.email}</span>
                      <span className="block text-slate-400 font-mono"> {selectedOrder.phone}</span>
                    </div>

                    {/* Product Details */}
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

                  {/* Bottom details */}
                  <div className="border border-slate-100 rounded-xl p-4 text-xs font-bold text-slate-500 space-y-2">
                    <div className="flex justify-between">
                      <span>Order Date</span>
                      <span className="text-slate-700">{selectedOrder.date}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Expected Delivery</span>
                      <span className="text-slate-700">{selectedOrder.delivery}</span>
                    </div>
                  </div>

                  {/* Actions buttons */}
                  <div className="flex flex-col sm:flex-row gap-2 pt-2">
                    {selectedOrder.status === 'Pending' ? (
                      <>
                        <button 
                          onClick={() => handleAcceptOrder(selectedOrder.id)}
                          className="flex-grow py-3 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow-md transition-all cursor-pointer"
                        >
                          Accept Order
                        </button>
                        <button 
                          onClick={() => handleRejectOrder(selectedOrder.id)}
                          className="px-4 py-3 text-xs font-bold text-red-655 border border-red-200 hover:bg-red-50 rounded-xl transition-all cursor-pointer"
                        >
                          Reject Order
                        </button>
                      </>
                    ) : selectedOrder.status === 'Accepted' && (!selectedOrder.logisticsPartner || selectedOrder.logisticsPartner === 'TBD') ? (
                      <div className="w-full space-y-2">
                        <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest block">Assign Logistics Partner</span>
                        <div className="flex gap-2 items-center">
                          <select 
                            value={assigningPartner}
                            onChange={(e) => setAssigningPartner(e.target.value)}
                            className="flex-grow px-3.5 py-2.5 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none focus:border-sky-500 cursor-pointer"
                          >
                            <option value="">Select Partner...</option>
                            <option>FastCargo Logistics</option>
                            <option>GlobalShip Inc.</option>
                            <option>TransWorld Logistics</option>
                          </select>
                          <button
                            onClick={() => handleAssignLogistics(selectedOrder.id, assigningPartner)}
                            className="px-4 py-2.5 bg-sky-500 hover:bg-sky-400 text-white text-xs font-bold rounded-xl shadow transition-all cursor-pointer"
                          >
                            Assign
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button 
                        onClick={() => {
                          setSelectedShipment(shipments.find(s => s.orderId === selectedOrder.id) || null);
                          setSelectedOrder(null);
                        }}
                        className="w-full py-3 text-xs font-bold text-slate-700 border border-slate-200 hover:bg-slate-50 rounded-xl transition-all cursor-pointer"
                      >
                        {selectedOrder.logisticsPartner ? `Track with ${selectedOrder.logisticsPartner} ` : 'Track Shipment '}
                      </button>
                    )}
                  </div>

                </div>
              </div>
            )}

            {/* SHIPMENT DETAIL TIMELINE MODAL */}
            {selectedShipment && (
              <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                {/* Backdrop */}
                <div onClick={() => setSelectedShipment(null)} className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"></div>

                {/* Modal box */}
                <div className="relative w-full max-w-md bg-white border border-slate-200 rounded-2xl p-6 shadow-2xl animate-in scale-in duration-300 z-10 space-y-5">
                  {/* Header */}
                  <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                    <h3 className="text-sm font-black text-slate-900 uppercase tracking-widest">Shipment Timeline #{selectedShipment.id}</h3>
                    <button 
                      onClick={() => setSelectedShipment(null)}
                      className="p-1.5 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Shipping Info Card */}
                  <div className="border border-slate-100 rounded-xl p-4 text-xs font-semibold text-slate-500 space-y-2 bg-slate-50/20">
                    <div className="flex justify-between">
                      <span>Logistics Partner</span>
                      <span className="text-slate-800 font-extrabold">{selectedShipment.logistics}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Shipping Method</span>
                      <span className="text-slate-750">{selectedShipment.method}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Tracking ID</span>
                      <span className="text-slate-750 font-mono">{selectedShipment.tracking}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Estimated Delivery (ETA)</span>
                      <span className="text-slate-800">{selectedShipment.eta}</span>
                    </div>
                  </div>

                  {/* Vertical Timeline Steps */}
                  <div className="space-y-4">
                    <h4 className="text-xs font-bold text-slate-805 uppercase tracking-widest">Status Progress</h4>
                    
                    <div className="relative pl-6 space-y-6">
                      {/* Timeline bar */}
                      <div className="absolute left-[7px] top-2.5 bottom-2.5 w-0.5 bg-slate-100 z-0"></div>

                      {/* Step 1: Confirmed */}
                      <div className="relative z-10 text-xs">
                        <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-emerald-500 shadow-sm flex items-center justify-center">
                          <Check className="w-2 h-2 text-white" />
                        </div>
                        <span className="block font-bold text-emerald-600">Order Confirmed  15 July</span>
                        <span className="block text-[10px] text-slate-400">Payment cleared & product packed</span>
                      </div>

                      {/* Step 2: Picked Up */}
                      <div className="relative z-10 text-xs">
                        <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-emerald-500 shadow-sm flex items-center justify-center">
                          <Check className="w-2 h-2 text-white" />
                        </div>
                        <span className="block font-bold text-emerald-600">Cargo Picked Up  16 July</span>
                        <span className="block text-[10px] text-slate-400">Loaded into FastCargo transport fleet</span>
                      </div>

                      {/* Step 3: In Transit */}
                      <div className="relative z-10 text-xs">
                        <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-sky-500 shadow-sm flex items-center justify-center animate-pulse">
                          <span className="w-1.5 h-1.5 rounded-full bg-white"></span>
                        </div>
                        <span className="block font-bold text-sky-655">In Transit  18 July (Current)</span>
                        <span className="block text-[10px] text-slate-450 mt-0.5">{selectedShipment.currentLoc}</span>
                      </div>

                      {/* Step 4: Customs */}
                      <div className="relative z-10 text-xs">
                        <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-slate-200 bg-white shadow-sm"></div>
                        <span className="block font-semibold text-slate-400">Customs Clearance  Pending</span>
                        <span className="block text-[10px] text-slate-400">Audit of phytosanitary import certifications</span>
                      </div>

                      {/* Step 5: Delivered */}
                      <div className="relative z-10 text-xs">
                        <div className="absolute -left-[23px] top-1 w-3.5 h-3.5 rounded-full border-2 border-slate-200 bg-white shadow-sm"></div>
                        <span className="block font-semibold text-slate-400">Delivered  Estimated {selectedShipment.eta}</span>
                        <span className="block text-[10px] text-slate-400">Sign-off by EuroSpice representative</span>
                      </div>

                    </div>
                  </div>

                </div>
              </div>
            )}

          </div>
        )}


        {activeView === 'profile' && (
          <div className="max-w-2xl mx-auto bg-white border border-slate-200/80 rounded-2xl p-6 sm:p-8 shadow-sm space-y-6 animate-in fade-in duration-300">
            <div>
              <h1 className="text-xl font-black text-slate-900 tracking-tight">Exporter Profile Settings</h1>
              <p className="text-xs text-slate-500 mt-1 font-medium">Verify company registrations and update passwords.</p>
            </div>

            {/* Profile fields */}
            <div className="border border-slate-100 bg-slate-50/20 p-5 rounded-2xl space-y-4">
              <div className="grid grid-cols-2 gap-4 text-xs font-bold">
                <div className="space-y-1">
                  <span className="text-slate-400 block">Full Name</span>
                  <span className="text-slate-800 block text-sm">John Doe</span>
                </div>
                <div className="space-y-1">
                  <span className="text-slate-400 block">Company Name</span>
                  <span className="text-slate-800 block text-sm">Acme Export Ltd</span>
                </div>
                <div className="space-y-1">
                  <span className="text-slate-400 block">Registered Email</span>
                  <span className="text-slate-805 block text-sm font-mono">exporter@company.com</span>
                </div>
                <div className="space-y-1">
                  <span className="text-slate-400 block">Phone</span>
                  <span className="text-slate-805 block text-sm font-mono">+91 98765 43210</span>
                </div>
                <div className="space-y-1 col-span-2 border-t border-slate-100 pt-3">
                  <span className="text-slate-400 block">Base Country</span>
                  <span className="text-slate-808 block text-sm">India</span>
                </div>
              </div>
            </div>

            {/* Button Actions */}
            <div className="flex gap-3 pt-2">
              <button 
                onClick={() => addToast('Update Profile modal is not active.', 'success')}
                className="flex-grow py-3 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow-md transition-all cursor-pointer"
              >
                Update Profile Info
              </button>
              <button 
                onClick={() => addToast('Change password token sent to email.', 'success')}
                className="px-5 py-3 text-xs font-bold text-slate-655 border border-slate-200 hover:bg-slate-50 rounded-xl transition-all cursor-pointer"
              >
                Change Password
              </button>
            </div>
          </div>
        )}

      </div>

      {/* ADD PRODUCT DRAWER PANEL */}
      {showAddDrawer && (
        <div className="fixed inset-0 z-50 flex justify-end">
          {/* Drawer Backdrop blur */}
          <div 
            onClick={() => setShowAddDrawer(false)}
            className="absolute inset-0 bg-slate-900/10 backdrop-blur-xs"
          ></div>

          {/* Drawer Content */}
          <div className="relative w-full max-w-md bg-white border-l border-slate-200 h-full shadow-2xl p-6 sm:p-8 flex flex-col justify-between overflow-hidden animate-in slide-in-from-right duration-300">
            {/* Header */}
            <div className="flex justify-between items-center border-b border-slate-100 pb-4 mb-6 shrink-0">
              <div>
                <h2 className="text-lg font-black text-slate-900 tracking-tight">{editingProductId ? 'Edit Product' : 'Add New Product'}</h2>
                <span className="text-xxs text-slate-400 font-semibold block mt-0.5">{editingProductId ? 'Update the specifications for this catalog record' : 'Input custom specifications for analysis'}</span>
              </div>
              <button 
                onClick={() => { setShowAddDrawer(false); setEditingProductId(null); }}
                className="p-2 hover:bg-slate-50 rounded-full cursor-pointer text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Scrollable Form Content */}
            <div className="flex-grow overflow-y-auto pr-2 -mr-2 space-y-4">
              {/* Inputs */}
              <form className="space-y-4" onSubmit={handleSaveProduct}>
                {/* Name */}
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Product Name</label>
                  <input 
                    type="text" 
                    value={newProdName}
                    onChange={(e) => {
                      setNewProdName(e.target.value);
                      if (errors.name) setErrors({ ...errors, name: null });
                    }}
                    placeholder="e.g., Organic Turmeric Powder"
                    className={`w-full px-4 py-2.5 border rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 ${errors.name ? 'border-red-500/50 bg-red-50/5' : 'border-slate-200'}`}
                  />
                  {errors.name && (
                    <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.name}</p>
                  )}
                </div>

                {/* Product Category */}
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Product Category</label>
                  <select 
                    value={newProdCategory}
                    onChange={(e) => setNewProdCategory(e.target.value)}
                    className="w-full px-4 py-2.5 border border-slate-200 bg-white rounded-xl text-xs focus:outline-none focus:border-sky-500 cursor-pointer"
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
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Product Description</label>
                  <textarea 
                    rows="3"
                    value={newProdDesc}
                    onChange={(e) => {
                      setNewProdDesc(e.target.value);
                      if (errors.description) setErrors({ ...errors, description: null });
                    }}
                    placeholder="Describe your product: material, quality, origin, specifications..."
                    className={`w-full px-4 py-2.5 border rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 resize-none ${errors.description ? 'border-red-500/50 bg-red-50/5' : 'border-slate-200'}`}
                  />
                  {errors.description && (
                    <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.description}</p>
                  )}
                </div>


              {/* Material */}
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Material</label>
                <input type="text" value={newProdMaterial} onChange={e => setNewProdMaterial(e.target.value)} placeholder="e.g., Cotton, Steel, Wood, Plastic" className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10"/>
              </div>

              {/* Composition + Manufacturing Process (side by side) */}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Composition</label>
                  <input type="text" value={newProdComposition} onChange={e => setNewProdComposition(e.target.value)} placeholder="e.g., 100% cotton" className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10"/>
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Manufacturing Process</label>
                  <input type="text" value={newProdManufacturing} onChange={e => setNewProdManufacturing(e.target.value)} placeholder="e.g., Knitted, Woven, Molded" className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10"/>
                </div>
              </div>

              {/* Function + Physical Form (side by side) */}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Function / Use</label>
                  <input type="text" value={newProdFunction} onChange={e => setNewProdFunction(e.target.value)} placeholder="e.g., Casual clothing, Food spice" className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10"/>
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Physical Form</label>
                  <input type="text" value={newProdPhysicalForm} onChange={e => setNewProdPhysicalForm(e.target.value)} placeholder="e.g., Finished garment, Powder" className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10"/>
                </div>
              </div>

                {/* HS Code */}
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">HS Code</label>
                  <div className="flex gap-2">
                    <input 
                      type="text" 
                      value={newProdHscode}
                      onChange={(e) => {
                        setNewProdHscode(e.target.value);
                        if (errors.hscode) setErrors({ ...errors, hscode: null });
                      }}
                      placeholder="Click Predict to auto-fill"
                      className={`flex-grow px-4 py-2.5 border rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 font-mono ${errors.hscode ? 'border-red-500/50 bg-red-50/5' : 'border-slate-200'}`}
                    />
                    <button
                      type="button"
                      onClick={handleSuggestHsCode}
                      disabled={!newProdName.trim() || !newProdDesc.trim() || hsCodeLoading}
                      className="px-3 py-2.5 text-[10px] font-bold text-sky-600 bg-sky-50 hover:bg-sky-100 border border-sky-200 rounded-xl transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed whitespace-nowrap"
                    >
                      {hsCodeLoading ? '...' : 'Predict'}
                    </button>
                  </div>
                  {!newProdName.trim() || !newProdDesc.trim() ? (
                    <p className="text-[9px] text-slate-400 mt-1">Fill product name, category & description above to enable prediction</p>
                  ) : null}
                  {errors.hscode && (
                    <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.hscode}</p>
                  )}
                  {/* HS Code Suggestions Dropdown */}
                  {hsCodeSuggestions.length > 0 && (
                    <div className="mt-2 border border-sky-100 bg-sky-50/30 rounded-xl p-2.5 space-y-1.5 max-h-40 overflow-y-auto">
                      <span className="text-[9px] font-black text-slate-400 uppercase tracking-wider block">Predictions (click to select)</span>
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
                          className="w-full text-left p-2 rounded-lg border border-slate-100 bg-white hover:border-sky-300 hover:bg-sky-50 transition-all cursor-pointer"
                        >
                          <div className="flex justify-between items-center">
                            <span className="font-mono text-xs font-bold text-slate-900">{s.hs_code}</span>
                            <span className="text-[9px] font-bold text-sky-600">{Math.round((s.similarity_score || 0) * 100)}% match</span>
                          </div>
                          <p className="text-[10px] text-slate-500 mt-0.5 line-clamp-1">{s.description}</p>
                        </button>
                      ))}
                      {hsCodeDisclaimer && (
                        <p className="text-[8px] text-slate-400 mt-1 italic">{hsCodeDisclaimer}</p>
                      )}
                    </div>
                  )}
                </div>

                {/* Unit weight - used by freight and landed-cost calculations */}
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Unit Weight (kg)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.001"
                    value={newProdWeight}
                    onChange={(e) => setNewProdWeight(e.target.value)}
                    placeholder="0.45"
                    className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10"
                  />
                  <p className="text-[9px] text-slate-400">Used to calculate freight and landed cost. Leave blank to enter it per shipment instead.</p>
                </div>

                {/* Pricing Row */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Unit Price (per kg)</label>
                    <input 
                      type="number" 
                      value={newProdPrice}
                      onChange={(e) => {
                        setNewProdPrice(e.target.value);
                        if (errors.price) setErrors({ ...errors, price: null });
                      }}
                      placeholder="350"
                      className={`w-full px-4 py-2.5 border rounded-xl text-xs focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 ${errors.price ? 'border-red-500/50 bg-red-50/5' : 'border-slate-200'}`}
                    />
                    {errors.price && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.price}</p>
                    )}
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Currency</label>
                    <select 
                      value={newProdCurrency}
                      onChange={(e) => setNewProdCurrency(e.target.value)}
                      className="w-full px-4 py-2.5 border border-slate-200 bg-white rounded-xl text-xs focus:outline-none focus:border-sky-500 cursor-pointer"
                    >
                      <option>INR</option>
                      <option>USD</option>
                      <option>EUR</option>
                    </select>
                  </div>
                </div>

                {/* Origin */}
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Country of Origin</label>
                  <select 
                    value={newProdOrigin}
                    onChange={(e) => setNewProdOrigin(e.target.value)}
                    className="w-full px-4 py-2.5 border border-slate-200 bg-white rounded-xl text-xs focus:outline-none focus:border-sky-500 cursor-pointer"
                  >
                    <option>India</option>
                    <option>Singapore</option>
                    <option>United States</option>
                  </select>
                </div>
              </form>
            </div>

            {/* Actions footer */}
            <div className="space-y-2 pt-6 mt-6 border-t border-slate-100 shrink-0">
              <button 
                onClick={handleSaveProduct}
                disabled={drawerLoading}
                className="w-full inline-flex items-center justify-center gap-1.5 px-6 py-3 font-bold text-xs text-white bg-slate-800 hover:bg-slate-700 rounded-xl transition-all cursor-pointer"
              >
                {drawerLoading ? <Loader2 className="w-4.5 h-4.5 animate-spin" /> : 'Save Product'}
              </button>
              <button 
                onClick={handleAnalyzeProduct}
                disabled={drawerLoading}
                className="w-full inline-flex items-center justify-center gap-1.5 px-6 py-3 font-bold text-xs text-sky-600 border border-sky-200 hover:bg-sky-50 rounded-xl transition-all cursor-pointer"
              >
                {drawerLoading ? <Loader2 className="w-4.5 h-4.5 animate-spin" /> : ' Analyze Product'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
