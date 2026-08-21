import React, { useState, useCallback } from 'react';
import { Eye, EyeOff, User, Building, Mail, Phone, Globe, Lock, Briefcase, Truck, ShieldCheck, ArrowLeft, ArrowRight, Loader2, Check, X } from 'lucide-react';
import { authApi, setToken, setUser } from '../services/api';

// ── Validation helpers (pure functions, no side effects) ─────────────────────

const NAME_REGEX = /^[A-Za-z][A-Za-z '.\-]{1,58}[A-Za-z.]$/;
const COMPANY_REGEX = /^[A-Za-z0-9][A-Za-z0-9 &.\-]{0,98}[A-Za-z0-9.&]$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const INDIA_PHONE_REGEX = /^[6-9]\d{9}$/;
const GSTIN_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/;

const PASSWORD_RULES = [
  { id: 'length', label: 'At least 8 characters', test: (p) => p.length >= 8 },
  { id: 'upper', label: 'One uppercase letter', test: (p) => /[A-Z]/.test(p) },
  { id: 'lower', label: 'One lowercase letter', test: (p) => /[a-z]/.test(p) },
  { id: 'digit', label: 'One number', test: (p) => /\d/.test(p) },
  { id: 'special', label: 'One special character', test: (p) => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?~`]/.test(p) },
];

// Canonical export category list. Must stay in sync with:
//   backend/.../DataSeeder.java  -> seedCategories()
//   frontend/src/pages/Exporter.jsx -> deriveProductCategory()
const PRODUCT_CATEGORIES = [
  'Agricultural Products', 'Spices', 'Food Products', 'Processed Foods', 'Marine Products',
  'Textiles', 'Apparel & Garments', 'Home Textiles',
  'Leather Products', 'Footwear', 'Handicrafts',
  'Ceramics & Pottery', 'Glassware', 'Jewellery & Gems',
  'Chemicals', 'Cosmetics & Personal Care', 'Pharmaceuticals',
  'Plastics & Rubber', 'Electronics', 'Engineering Goods',
  'Machinery', 'Automotive Components', 'Furniture & Wood', 'Others',
];

const COUNTRY_LIST = [
  { name: 'India', code: 'IN' },
  { name: 'Germany', code: 'DE' },
  { name: 'Singapore', code: 'SG' },
  { name: 'Japan', code: 'JP' },
  { name: 'UAE', code: 'AE' },
  { name: 'United States', code: 'US' },
  { name: 'United Kingdom', code: 'GB' },
  { name: 'Australia', code: 'AU' },
  { name: 'Canada', code: 'CA' },
  { name: 'France', code: 'FR' },
  { name: 'Netherlands', code: 'NL' },
  { name: 'Saudi Arabia', code: 'SA' },
  { name: 'Indonesia', code: 'ID' },
  { name: 'Brazil', code: 'BR' },
  { name: 'South Africa', code: 'ZA' },
];

export default function Register({ onNavigate }) {
  const [step, setStep] = useState(1); // 1, 2, 3
  const [role, setRole] = useState('exporter'); // 'exporter', 'logistics'

  // Step 1: Basic Info Form States
  const [fullName, setFullName] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [country, setCountry] = useState('India');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Show/Hide Passwords
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Step 2: Role-Specific Details States
  // Exporter Specifics
  const [exporterGst, setExporterGst] = useState('');
  const [exporterType, setExporterType] = useState(''); // Manufacturer, Trader, Both
  const [exporterCategories, setExporterCategories] = useState([]);
  const [otherCategoryText, setOtherCategoryText] = useState('');
  const [exporterProducts, setExporterProducts] = useState('');
  const [exporterExp, setExporterExp] = useState(''); // Beginner, Intermediate, Experienced

  // Logistics Specifics
  const [logisticsServices, setLogisticsServices] = useState([]);
  const [logisticsRegions, setLogisticsRegions] = useState([]);
  const [logisticsRegNo, setLogisticsRegNo] = useState('');
  const [logisticsExperience, setLogisticsExperience] = useState('');
  const [logisticsTracking, setLogisticsTracking] = useState('No');
  const [logisticsInsurance, setLogisticsInsurance] = useState('No');

  // Step 3: Terms & Conditions
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false); // prevent double-submit
  const [toast, setToast] = useState(null); // { type: 'success'|'error', message: '' }
  const [shake, setShake] = useState(false);
  const [errors, setErrors] = useState({});

  const triggerToast = (type, message) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  };



  // ── VALIDATION ──────────────────────────────────────────────────────────────

  const validateStep1 = () => {
    const newErrors = {};

    // Full Name: 3-60, alpha + space + apostrophe + period
    const trimmedName = fullName.trim().replace(/\s{2,}/g, ' ');
    if (!trimmedName) {
      newErrors.fullName = 'Please enter your full name.';
    } else if (trimmedName.length < 3) {
      newErrors.fullName = 'Full name must be at least 3 characters.';
    } else if (trimmedName.length > 60) {
      newErrors.fullName = 'Full name must not exceed 60 characters.';
    } else if (!NAME_REGEX.test(trimmedName)) {
      newErrors.fullName = "Full name may only contain letters, spaces, apostrophe (') and period (.).";
    }

    // Company Name: 2-100, alphanum + space + & + - + .
    const trimmedCompany = companyName.trim().replace(/\s{2,}/g, ' ');
    if (!trimmedCompany) {
      newErrors.companyName = 'Company name is required.';
    } else if (trimmedCompany.length < 2) {
      newErrors.companyName = 'Company name must be at least 2 characters.';
    } else if (trimmedCompany.length > 100) {
      newErrors.companyName = 'Company name must not exceed 100 characters.';
    } else if (!COMPANY_REGEX.test(trimmedCompany)) {
      newErrors.companyName = 'Company name may only contain letters, numbers, spaces, &, - and period.';
    }

    // Email
    const trimmedEmail = email.trim().toLowerCase();
    if (!trimmedEmail) {
      newErrors.email = 'Email address is required.';
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      newErrors.email = 'Please enter a valid email address.';
    }

    // Phone: country-aware
    const cleanPhone = phone.trim().replace(/[\s\-]/g, '');
    if (!cleanPhone) {
      newErrors.phone = 'Phone number is required.';
    } else if (country === 'India') {
      // Extract the 10-digit local number. Only strip prefix if it's literally
      // +91 or 91 followed by a digit in [6-9] (i.e. the country code, not
      // part of the subscriber number).
      let digits = cleanPhone;
      if (/^\+91[6-9]/.test(digits)) digits = digits.slice(3);
      else if (/^91[6-9]/.test(digits) && digits.length === 12) digits = digits.slice(2);
      else if (digits.startsWith('0')) digits = digits.substring(1);
      if (!/^[6-9]\d{9}$/.test(digits)) {
        newErrors.phone = 'Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8 or 9.';
      }
    } else {
      // International: 7-15 digits
      const intDigits = cleanPhone.replace(/^\+/, '');
      if (!/^\d{7,15}$/.test(intDigits)) {
        newErrors.phone = 'Please enter a valid phone number (7-15 digits).';
      }
    }

    // Country
    if (!country) {
      newErrors.country = 'Please select your base country.';
    }

    // Password strength
    const pwdFails = PASSWORD_RULES.filter(r => !r.test(password));
    if (!password) {
      newErrors.password = 'Password is required.';
    } else if (password.length > 64) {
      newErrors.password = 'Password must not exceed 64 characters.';
    } else if (pwdFails.length > 0) {
      newErrors.password = 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character.';
    }

    // Confirm password
    if (!confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password.';
    } else if (password !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validateStep2 = () => {
    const newErrors = {};
    if (role === 'exporter') {
      // GST / Business Registration Number
      const gst = exporterGst.trim().toUpperCase();
      if (!gst) {
        newErrors.exporterGst = 'Please enter a valid GST or Business Registration Number.';
      } else if (country === 'India') {
        if (!GSTIN_REGEX.test(gst)) {
          newErrors.exporterGst = 'Please enter a valid 15-character GSTIN (e.g. 29AAAAA0000A1Z1).';
        }
      } else {
        if (gst.length < 5 || gst.length > 30) {
          newErrors.exporterGst = 'Business Registration Number must be 5-30 characters.';
        }
      }

      // Business Type
      if (!exporterType) {
        newErrors.exporterType = 'Please select a business type.';
      }

      // Product Categories: 1-3
      if (exporterCategories.length === 0) {
        newErrors.exporterCategories = 'Please select at least one product category.';
      } else if (exporterCategories.length > 3) {
        newErrors.exporterCategories = 'You may select a maximum of 3 product categories.';
      }

      // "Others" requires specification
      if (exporterCategories.includes('Others')) {
        const otherTrimmed = otherCategoryText.trim();
        if (!otherTrimmed || otherTrimmed.length < 3) {
          newErrors.otherCategoryText = 'Please specify the product category (minimum 3 characters).';
        } else if (otherTrimmed.length > 50) {
          newErrors.otherCategoryText = 'Category specification must not exceed 50 characters.';
        }
      }

      // Product Description: 10-500
      const desc = exporterProducts.trim();
      if (!desc) {
        newErrors.exporterProducts = 'Please describe your primary products.';
      } else if (desc.length < 10) {
        newErrors.exporterProducts = 'Product description must be at least 10 characters.';
      } else if (desc.length > 500) {
        newErrors.exporterProducts = 'Product description must not exceed 500 characters.';
      }

      // Export Experience
      if (!exporterExp) {
        newErrors.exporterExp = 'Please select your export experience level.';
      }
    }
    // Logistics: validate all required fields
    if (role === 'logistics') {
      // Service Types: 1-4
      if (logisticsServices.length === 0) {
        newErrors.logisticsServices = 'Select at least one service.';
      } else if (logisticsServices.length > 4) {
        newErrors.logisticsServices = 'You can select up to 4 services.';
      }
      // Service Regions: 1-5
      if (logisticsRegions.length === 0) {
        newErrors.logisticsRegions = 'Select at least one service region.';
      } else if (logisticsRegions.length > 5) {
        newErrors.logisticsRegions = 'You can select up to 5 regions.';
      }
      // Business Registration Number: 8-20, uppercase alphanumeric
      const regNo = logisticsRegNo.trim().toUpperCase();
      if (!regNo) {
        newErrors.logisticsRegNo = 'Business Registration Number is required.';
      } else if (regNo.length < 8 || regNo.length > 20) {
        newErrors.logisticsRegNo = 'Enter a valid Business Registration Number.';
      } else if (!/^[A-Z0-9]+$/.test(regNo)) {
        newErrors.logisticsRegNo = 'Enter a valid Business Registration Number.';
      }
      // Years of Experience
      if (!logisticsExperience) {
        newErrors.logisticsExperience = 'Please select your experience.';
      }
      // Live Tracking
      if (!logisticsTracking) {
        newErrors.logisticsTracking = 'Please choose whether you provide live tracking.';
      }
      // Cargo Insurance
      if (!logisticsInsurance) {
        newErrors.logisticsInsurance = 'Please choose whether you provide cargo insurance.';
      }
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNextStep = () => {
    if (step === 1) {
      if (validateStep1()) {
        setStep(2);
      } else {
        setShake(true);
        setTimeout(() => setShake(false), 500);
        triggerToast('error', 'Please correct the validation errors in Step 1.');
      }
    } else if (step === 2) {
      if (validateStep2()) {
        setStep(3);
      } else {
        setShake(true);
        setTimeout(() => setShake(false), 500);
        triggerToast('error', 'Please complete the business registration details.');
      }
    }
  };

  const handlePrevStep = () => {
    if (step > 1) {
      setStep(step - 1);
    }
  };

  // Multi-select toggle with max-3 enforcement for categories
  const toggleSelection = (list, setList, item, maxItems = 99) => {
    if (list.includes(item)) {
      setList(list.filter(i => i !== item));
    } else {
      if (list.length >= maxItems) return; // silently ignore if at max
      setList([...list, item]);
    }
  };



  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    if (!agreed) {
      setErrors(prev => ({ ...prev, agreed: 'You must agree to the Terms of Service & Privacy Policy' }));
      triggerToast('error', 'You must agree to the Terms and Conditions to proceed.');
      return;
    }
    if (submitting) return; // prevent double-submit

    setLoading(true);
    setSubmitting(true);

    // Build the RegisterRequest payload matching the backend DTO
    const payload = {
      name: fullName.trim().replace(/\s{2,}/g, ' '),
      email: email.trim().toLowerCase(),
      password,
      confirmPassword,
      phone: phone.trim().replace(/[\s\-]/g, ''),
      country,
      role: role === 'logistics' ? 'LOGISTICS' : 'EXPORTER',
      companyName: companyName.trim().replace(/\s{2,}/g, ' '),
      // Exporter-specific fields
      ...(role === 'exporter' && {
        gstNumber: exporterGst.trim().toUpperCase(),
        businessType: exporterType,
        productCategories: exporterCategories,
        otherCategoryDescription: exporterCategories.includes('Others') ? otherCategoryText.trim() : null,
        productDescription: exporterProducts.trim(),
        exportExperience: exporterExp,
        iecCode: '',
        address: '',
      }),
      // Logistics-specific fields
      ...(role === 'logistics' && {
        serviceArea: logisticsRegions.join(', '),
        fleetSize: logisticsExperience,
        gstNumber: logisticsRegNo.trim().toUpperCase(),
        services: logisticsServices,
        regions: logisticsRegions,
        businessRegistrationNumber: logisticsRegNo.trim().toUpperCase(),
        experience: logisticsExperience,
        trackingSupport: logisticsTracking === 'Yes',
        cargoInsurance: logisticsInsurance === 'Yes',
      }),
    };

    try {
      const response = await authApi.register(payload);
      const authData = response.data;

      // Store token and user info
      setToken(authData.token);
      setUser({
        userId: authData.userId,
        name: authData.name,
        email: authData.email,
        role: authData.role,
      });

      triggerToast('success', 'Account created successfully! Redirecting...');
      setTimeout(() => {
        const dashPath = authData.role === 'LOGISTICS' ? '/logistics' : '/exporter';
        window.history.replaceState({}, '', dashPath);
        onNavigate(dashPath);
      }, 800);
    } catch (err) {
      // If the backend returns field-level errors, display them
      if (err.data && typeof err.data === 'object' && err.data.data) {
        const fieldErrors = err.data.data;
        setErrors(prev => ({ ...prev, ...fieldErrors }));
        // Navigate to the step containing the first errored field
        const step1Fields = ['name', 'email', 'phone', 'password', 'confirmPassword', 'country', 'companyName'];
        const hasStep1Error = Object.keys(fieldErrors).some(f => step1Fields.includes(f));
        if (hasStep1Error && step !== 1) setStep(1);
      }
      triggerToast('error', err.message || 'Registration failed. Please try again.');
      setShake(true);
      setTimeout(() => setShake(false), 500);
    } finally {
      setLoading(false);
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-700 flex flex-col relative font-sans overflow-hidden">

      {/* Background Animated Gradient Overlay */}
      <div className="absolute inset-0 pointer-events-none z-0">
        <div className="absolute top-[10%] right-[10%] w-[600px] h-[600px] bg-blue-500/5 rounded-full blur-[120px] animate-pulse"></div>
        <div className="absolute bottom-[10%] left-[10%] w-[600px] h-[600px] bg-emerald-500/5 rounded-full blur-[120px] animate-float"></div>
        <div className="absolute inset-0 opacity-[0.4]" style={{
          backgroundImage: `radial-gradient(#e2e8f0 1.5px, transparent 1.5px)`,
          backgroundSize: '24px 24px'
        }}></div>
      </div>

      {/* Toast Alert */}
      {toast && (
        <div className={`fixed top-6 right-6 z-50 flex items-center gap-3 px-5 py-4 rounded-xl border backdrop-blur-md shadow-2xl animate-in fade-in slide-in-from-top-4 duration-300 ${toast.type === 'success'
            ? 'bg-emerald-50 border border-emerald-250 text-slate-800 shadow-emerald-500/5'
            : 'bg-red-50 border border-red-200 text-red-800 shadow-red-500/5'
          }`}>
          <div className={`w-2.5 h-2.5 rounded-full ${toast.type === 'success' ? 'bg-emerald-500' : 'bg-red-500'}`}></div>
          <span className="text-sm font-semibold">{toast.message}</span>
        </div>
      )}

      {/* Back button */}
      <div className="absolute top-6 left-6 z-30">
        <button
          onClick={() => onNavigate('/')}
          className="inline-flex items-center gap-2 text-sm font-semibold text-slate-655 hover:text-slate-900 transition-all bg-white border border-slate-200/80 backdrop-blur-md px-4 py-2 rounded-xl cursor-pointer shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Home
        </button>
      </div>

      {/* Main Split Content */}
      <div className="flex-grow grid grid-cols-1 lg:grid-cols-12 min-h-screen relative z-10">

        {/* Left Side: Modern Graphic & Testimonial (60%) */}
        <div className="hidden lg:flex lg:col-span-7 bg-slate-50/50 border-r border-slate-200/80 flex-col items-center justify-center p-12 relative overflow-hidden">

          {/* Subtle geometric circles */}
          <div className="absolute w-[400px] h-[400px] border border-slate-200/60 rounded-full z-0"></div>
          <div className="absolute w-[550px] h-[550px] border border-dashed border-slate-200/50 rounded-full z-0 animate-spin-slow"></div>

          {/* Central Flat Style Human Illustration */}
          <div className="relative z-10 flex flex-col items-center max-w-lg text-center">

            {/* SVG Illustration Container */}
            <div className="w-full max-w-[340px] aspect-square flex items-center justify-center relative mb-8">
              <svg viewBox="0 0 300 300" className="w-full h-full">
                {/* Background decorative grid bubble */}
                <circle cx="150" cy="150" r="100" fill="#f1f5f9" fillOpacity="0.8" stroke="#cbd5e1" strokeWidth="1" strokeDasharray="4,4" />
                <circle cx="150" cy="150" r="80" fill="#e2e8f0" fillOpacity="0.5" />

                {/* Floating abstract elements */}
                {/* 1. Globe outline */}
                <g className="animate-float" style={{ animationDelay: '1s' }}>
                  <circle cx="80" cy="90" r="22" fill="#ffffff" stroke="#3b82f6" strokeWidth="1" strokeOpacity="0.4" />
                  <path d="M68 90 A 22 22 0 0 0 92 90 M80 68 A 22 22 0 0 0 80 112" fill="none" stroke="#3b82f6" strokeWidth="1.2" strokeOpacity="0.6" />
                </g>

                {/* 2. Checked Document */}
                <g className="animate-[float_5s_ease-in-out_infinite]" style={{ animationDelay: '2.5s' }}>
                  <rect x="200" y="80" width="30" height="38" rx="3" fill="#ffffff" stroke="#10b981" strokeWidth="1.2" className="shadow-sm" />
                  <line x1="206" y1="92" x2="216" y2="92" stroke="#10b981" strokeWidth="1.5" />
                  <line x1="206" y1="100" x2="224" y2="100" stroke="#94a3b8" strokeWidth="1" />
                  <line x1="206" y1="108" x2="218" y2="108" stroke="#94a3b8" strokeWidth="1" />
                  <circle cx="222" cy="92" r="3" fill="#10b981" />
                </g>

                {/* 3. Container Ship silhouette */}
                <g className="animate-float" style={{ animationDelay: '0.2s' }}>
                  <path d="M 60 210 L 105 210 L 98 220 L 67 220 Z" fill="#3b82f6" />
                  <rect x="70" y="200" width="8" height="10" fill="#10b981" />
                  <rect x="80" y="202" width="8" height="8" fill="#3b82f6" />
                  <rect x="90" y="205" width="8" height="5" fill="#64748b" />
                  <line x1="50" y1="223" x2="115" y2="223" stroke="#3b82f6" strokeWidth="1.5" strokeDasharray="3,3" />
                </g>

                {/* Human/Businessperson vector */}
                <g>
                  {/* Suit torso */}
                  <path d="M 110 240 L 190 240 L 175 190 L 125 190 Z" fill="#475569" stroke="#334155" strokeWidth="1" />
                  {/* Tie */}
                  <path d="M 146 190 L 154 190 L 152 210 L 148 210 Z" fill="#3b82f6" />
                  {/* Collars */}
                  <path d="M 130 190 L 142 200 L 148 190 M 170 190 L 158 200 L 152 190" fill="none" stroke="#f1f5f9" strokeWidth="1.5" />

                  {/* Head skin */}
                  <circle cx="150" cy="165" r="18" fill="#cbd5e1" />
                  {/* Hair */}
                  <path d="M 132 165 Q 150 142 168 165 C 168 152 132 152 132 165" fill="#334155" />

                  {/* Tablet device */}
                  <rect x="135" y="195" width="38" height="26" rx="2.5" fill="#0f172a" stroke="#3b82f6" strokeWidth="1.5" className="animate-pulse" />
                  <rect x="141" y="201" width="26" height="14" fill="#3b82f6" fillOpacity="0.1" />
                  {/* Green glowing indicator on device */}
                  <circle cx="154" cy="208" r="2.5" fill="#10b981" />

                  {/* Hands */}
                  <path d="M 125 210 Q 130 205 136 210 M 175 210 Q 170 205 164 210" stroke="#cbd5e1" strokeWidth="3" strokeLinecap="round" fill="none" />
                </g>
              </svg>
            </div>

            {/* Title / Quote Text */}
            <h2 className="text-xl font-bold tracking-tight text-slate-850 mb-3">
              Compliance Intelligence Built for Global Growth
            </h2>
            <p className="text-sm text-slate-500 font-medium leading-relaxed max-w-sm">
              "Trade allowed us to index new custom tariffs and clear our freight compliance audits in record time."
            </p>
            <div className="mt-4 text-xs font-bold text-indigo-600 tracking-wider uppercase">
              Exporter, Textile Council backing
            </div>
          </div>
        </div>

        {/* Right Side: Step-by-Step Form Column (40%) */}
        <div className="lg:col-span-5 flex flex-col justify-center py-8 px-6 sm:px-8 relative overflow-y-auto max-h-screen">

          {/* Registration Card Wrapper */}
          <div className={`w-full max-w-lg bg-white border border-slate-200/80 backdrop-blur-xl rounded-2xl shadow-xl p-6 sm:p-7 transition-transform ${shake ? 'animate-[shake_0.4s_ease-in-out]' : ''
            }`}>

            {/* Step Indicator Header */}
            <div className="mb-6">
              <div className="flex items-center justify-between max-w-xs mx-auto relative mb-3">
                {/* Connection Line Background */}
                <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 h-0.5 bg-slate-200 z-0"></div>

                {/* Connection Line Active */}
                <div
                  className="absolute left-0 top-1/2 -translate-y-1/2 h-0.5 bg-sky-500 transition-all duration-300 z-0"
                  style={{ width: step === 1 ? '0%' : step === 2 ? '50%' : '100%' }}
                ></div>

                {/* Step 1 Circle */}
                <button
                  onClick={() => step > 1 && setStep(1)}
                  className={`relative z-10 w-7 h-7 rounded-full flex items-center justify-center font-bold text-[11px] border transition-all cursor-pointer ${step === 1
                      ? 'bg-sky-500 border-sky-400 text-white shadow-lg shadow-sky-500/20'
                      : step > 1
                        ? 'bg-emerald-500 border-emerald-400 text-white'
                        : 'bg-white border-slate-200 text-slate-400'
                    }`}
                >
                  {step > 1 ? <Check className="w-3.5 h-3.5" /> : '1'}
                </button>

                {/* Step 2 Circle */}
                <button
                  onClick={() => step > 2 && setStep(2)}
                  className={`relative z-10 w-7 h-7 rounded-full flex items-center justify-center font-bold text-[11px] border transition-all cursor-pointer ${step === 2
                      ? 'bg-sky-500 border-sky-400 text-white shadow-lg shadow-sky-500/20'
                      : step > 2
                        ? 'bg-emerald-500 border-emerald-400 text-white'
                        : 'bg-white border-slate-200 text-slate-400'
                    }`}
                >
                  {step > 2 ? <Check className="w-3.5 h-3.5" /> : '2'}
                </button>

                {/* Step 3 Circle */}
                <div
                  className={`relative z-10 w-7 h-7 rounded-full flex items-center justify-center font-bold text-[11px] border transition-all ${step === 3
                      ? 'bg-sky-500 border-sky-400 text-white shadow-lg shadow-sky-500/20'
                      : 'bg-white border-slate-200 text-slate-400'
                    }`}
                >
                  3
                </div>
              </div>

              <div className="flex justify-between max-w-xs mx-auto text-[9px] font-bold text-slate-450 uppercase tracking-widest text-center px-1">
                <span className={step >= 1 ? 'text-sky-600 font-extrabold' : ''}>Basic</span>
                <span className={step >= 2 ? 'text-sky-600 font-extrabold' : ''}>Details</span>
                <span className={step >= 3 ? 'text-sky-600 font-extrabold' : ''}>Confirm</span>
              </div>
            </div>

            {/* ==================================================== */}
            {/* STEP 1 CONTENT: BASIC DETAILS & ROLE SELECTION */}
            {/* ==================================================== */}
            {step === 1 && (
              <div className="space-y-5 animate-slide-in-right">

                <div>
                  <h2 className="text-lg font-bold text-slate-900 tracking-tight">Create Your Account</h2>
                  <p className="text-xxs text-slate-500 font-medium">Step 1 of 3 · Profile and Role Settings</p>
                </div>

                {/* Double-Column Fields Grid */}
                <div className="grid grid-cols-2 gap-x-4 gap-y-3.5">

                  {/* Full Name */}
                  <div className="col-span-1 space-y-1 relative">
                    <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Full Name</label>
                    <div className="relative">
                      <User className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                      <input
                        type="text"
                        value={fullName}
                        onChange={(e) => {
                          setFullName(e.target.value);
                          if (errors.fullName) setErrors({ ...errors, fullName: null });
                        }}
                        placeholder="John Doe"
                        className={`w-full pl-9 pr-3 py-2 bg-white border rounded-xl text-xs text-slate-805 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.fullName ? 'border-red-500/50' : 'border-slate-200'
                          }`}
                      />
                    </div>
                    {errors.fullName && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.fullName}</p>
                    )}
                  </div>

                  {/* Company Name */}
                  <div className="col-span-1 space-y-1 relative">
                    <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Company Name</label>
                    <div className="relative">
                      <Building className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                      <input
                        type="text"
                        value={companyName}
                        onChange={(e) => {
                          setCompanyName(e.target.value);
                          if (errors.companyName) setErrors({ ...errors, companyName: null });
                        }}
                        placeholder="Acme Ltd"
                        className={`w-full pl-9 pr-3 py-2 bg-white border rounded-xl text-xs text-slate-805 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.companyName ? 'border-red-500/50' : 'border-slate-200'
                          }`}
                      />
                    </div>
                    {errors.companyName && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.companyName}</p>
                    )}
                  </div>

                  {/* Email Address */}
                  <div className="col-span-1 space-y-1 relative">
                    <label className="text-[10px] font-bold text-slate-555 uppercase tracking-widest">Email</label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => {
                          setEmail(e.target.value);
                          if (errors.email) setErrors({ ...errors, email: null });
                        }}
                        placeholder="name@company.com"
                        className={`w-full pl-9 pr-3 py-2 bg-white border rounded-xl text-xs text-slate-805 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.email ? 'border-red-500/50' : 'border-slate-200'
                          }`}
                      />
                    </div>
                    {errors.email && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.email}</p>
                    )}
                  </div>

                  {/* Phone Number */}
                  <div className="col-span-1 space-y-1 relative">
                    <label className="text-[10px] font-bold text-slate-555 uppercase tracking-widest">Phone Number</label>
                    <div className="relative">
                      <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                      <input
                        type="tel"
                        value={phone}
                        onChange={(e) => {
                          setPhone(e.target.value);
                          if (errors.phone) setErrors({ ...errors, phone: null });
                        }}
                        placeholder="+91 98765"
                        className={`w-full pl-9 pr-3 py-2 bg-white border rounded-xl text-xs text-slate-805 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.phone ? 'border-red-500/50' : 'border-slate-200'
                          }`}
                      />
                    </div>
                    {errors.phone && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.phone}</p>
                    )}
                  </div>

                  {/* Country */}
                  <div className="col-span-2 space-y-1 relative">
                    <label className="text-[10px] font-bold text-slate-555 uppercase tracking-widest">Base Country</label>
                    <div className="relative">
                      <Globe className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                      <select
                        value={country}
                        onChange={(e) => setCountry(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-700 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 cursor-pointer"
                      >
                        <option value="">Select Country</option>
                        {COUNTRY_LIST.map(c => (
                          <option key={c.code} value={c.name}>{c.name}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  {/* Password */}
                  <div className="col-span-1 space-y-1 relative">
                    <label className="text-[10px] font-bold text-slate-555 uppercase tracking-widest">Password</label>
                    <div className="relative">
                      <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => {
                          setPassword(e.target.value);
                          if (errors.password) setErrors({ ...errors, password: null });
                        }}
                        placeholder="Min 8 chars"
                        className={`w-full pl-9 pr-9 py-2 bg-white border rounded-xl text-xs text-slate-805 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.password ? 'border-red-500/50' : 'border-slate-200'
                          }`}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                      >
                        {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                    </div>
                    {errors.password && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.password}</p>
                    )}
                    {/* Live password strength checklist */}
                    {password.length > 0 && (
                      <div className="mt-1.5 space-y-0.5">
                        {PASSWORD_RULES.map(rule => (
                          <div key={rule.id} className="flex items-center gap-1.5">
                            {rule.test(password)
                              ? <Check className="w-3 h-3 text-emerald-500" />
                              : <X className="w-3 h-3 text-slate-300" />}
                            <span className={`text-[9px] font-medium ${rule.test(password) ? 'text-emerald-600' : 'text-slate-400'}`}>{rule.label}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Confirm Password */}
                  <div className="col-span-1 space-y-1 relative">
                    <label className="text-[10px] font-bold text-slate-555 uppercase tracking-widest">Verify Password</label>
                    <div className="relative">
                      <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                      <input
                        type={showConfirmPassword ? 'text' : 'password'}
                        value={confirmPassword}
                        onChange={(e) => {
                          setConfirmPassword(e.target.value);
                          if (errors.confirmPassword) setErrors({ ...errors, confirmPassword: null });
                        }}
                        placeholder="Verify password"
                        className={`w-full pl-9 pr-9 py-2 bg-white border rounded-xl text-xs text-slate-805 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.confirmPassword ? 'border-red-500/50' : 'border-slate-200'
                          }`}
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-655 cursor-pointer"
                      >
                        {showConfirmPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                    </div>
                    {errors.confirmPassword && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.confirmPassword}</p>
                    )}
                  </div>
                </div>

                {/* Role Selection Horizontal Cards */}
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Select Account Role</label>
                  <div className="grid grid-cols-2 gap-2.5">
                    {/* Exporter Card */}
                    <div
                      onClick={() => setRole('exporter')}
                      className={`relative p-2.5 rounded-xl border transition-all cursor-pointer flex flex-col items-center justify-between text-center ${role === 'exporter'
                          ? 'border-sky-500 bg-sky-50/40 text-slate-900 scale-[1.02] shadow-sm'
                          : 'border-slate-200 bg-white/60 text-slate-500 hover:border-slate-300'
                        }`}
                    >
                      <Briefcase className={`w-5 h-5 ${role === 'exporter' ? 'text-sky-500' : 'text-slate-400'}`} />
                      <span className="text-xxs font-bold mt-1">Exporter</span>
                    </div>

                    {/* Logistics Card */}
                    <div
                      onClick={() => setRole('logistics')}
                      className={`relative p-2.5 rounded-xl border transition-all cursor-pointer flex flex-col items-center justify-between text-center ${role === 'logistics'
                          ? 'border-sky-500 bg-sky-50/40 text-slate-900 scale-[1.02] shadow-sm'
                          : 'border-slate-200 bg-white/60 text-slate-500 hover:border-slate-300'
                        }`}
                    >
                      <Truck className={`w-5 h-5 ${role === 'logistics' ? 'text-sky-500' : 'text-slate-400'}`} />
                      <span className="text-xxs font-bold mt-1">Logistics</span>
                    </div>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex justify-between items-center pt-4 border-t border-slate-200">
                  <button
                    type="button"
                    onClick={() => onNavigate('/login')}
                    className="text-xs font-semibold text-slate-500 hover:text-sky-655 transition-colors cursor-pointer"
                  >
                    Already have an account? Sign In
                  </button>
                  <button
                    type="button"
                    onClick={handleNextStep}
                    disabled={!fullName || !companyName || !email || !phone || !password || !confirmPassword}
                    className="inline-flex items-center gap-1.5 px-6 py-2.5 font-bold text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 hover:scale-103 shadow-md rounded-xl transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
                  >
                    Continue
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>

              </div>
            )}

            {/* ==================================================== */}
            {/* STEP 2 CONTENT: ROLE SPECIFIC INFORMATION */}
            {/* ==================================================== */}
            {step === 2 && (
              <div className="space-y-6 animate-slide-in-right">

                <div>
                  <h2 className="text-lg font-bold text-slate-900 tracking-tight">Tell Us About Your Business</h2>
                  <p className="text-xs text-slate-500 mt-1 font-medium">Step 2 of 3 · {role === 'exporter' ? 'Exporter Details' : 'Logistics Partner Details'}</p>
                </div>

                {/* RENDER EXPORTER DETAILS */}
                {role === 'exporter' && (
                  <div className="space-y-5">
                    {/* GST / Business Registration No */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                        {country === 'India' ? 'GST Number (GSTIN)' : 'Business Registration Number'}
                      </label>
                      <input
                        type="text"
                        value={exporterGst}
                        onChange={(e) => {
                          setExporterGst(e.target.value);
                          if (errors.exporterGst) setErrors({ ...errors, exporterGst: null });
                        }}
                        placeholder={country === 'India' ? '29AAAAA0000A1Z1' : 'Registration number'}
                        maxLength={country === 'India' ? 15 : 30}
                        className={`w-full px-4 py-2 bg-white border rounded-xl text-xs placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.exporterGst ? 'border-red-500/50' : 'border-slate-200'
                          }`}
                      />
                      {errors.exporterGst && <p className="text-xs text-red-500 font-semibold">{errors.exporterGst}</p>}
                    </div>

                    {/* Business Type */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Business Type</label>
                      <div className="grid grid-cols-3 gap-2.5">
                        {['Manufacturer', 'Trader', 'Both'].map((t) => (
                          <button
                            key={t}
                            type="button"
                            onClick={() => { setExporterType(t); if (errors.exporterType) setErrors({ ...errors, exporterType: null }); }}
                            className={`py-2 rounded-xl text-xs font-bold border transition-all cursor-pointer ${exporterType === t
                                ? 'bg-sky-500 text-white border-sky-400 shadow-md shadow-sky-500/10 scale-102'
                                : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                              }`}
                          >
                            {t}
                          </button>
                        ))}
                      </div>
                      {errors.exporterType && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.exporterType}</p>}
                    </div>

                    {/* Product Category Multi-Select (max 3) */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Product Categories <span className="text-slate-400 normal-case">(select 1-3)</span></label>
                      <div className="flex flex-wrap gap-2">
                        {PRODUCT_CATEGORIES.map((cat) => {
                          const active = exporterCategories.includes(cat);
                          const atMax = exporterCategories.length >= 3 && !active;
                          return (
                            <button
                              key={cat}
                              type="button"
                              onClick={() => toggleSelection(exporterCategories, setExporterCategories, cat, 3)}
                              disabled={atMax}
                              className={`px-3 py-1 rounded-full text-xs font-bold border transition-all cursor-pointer flex items-center gap-1.5 ${active
                                  ? 'bg-gradient-to-r from-sky-500 to-indigo-600 border-sky-400 text-white shadow-md scale-102'
                                  : atMax
                                    ? 'bg-slate-50 border-slate-100 text-slate-300 cursor-not-allowed'
                                    : 'bg-white border-slate-200 text-slate-650 hover:bg-slate-50 hover:border-slate-300'
                                }`}
                            >
                              <span>{cat}</span>
                              {active && <X className="w-3 h-3 text-white" />}
                            </button>
                          );
                        })}
                      </div>
                      {errors.exporterCategories && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.exporterCategories}</p>}

                      {/* "Others" specification field — only shown when Others is selected */}
                      {exporterCategories.includes('Others') && (
                        <div className="mt-2">
                          <input
                            type="text"
                            value={otherCategoryText}
                            onChange={(e) => {
                              setOtherCategoryText(e.target.value);
                              if (errors.otherCategoryText) setErrors({ ...errors, otherCategoryText: null });
                            }}
                            placeholder="Specify your product category (3-50 chars)"
                            maxLength={50}
                            className={`w-full px-4 py-2 bg-white border rounded-xl text-xs placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.otherCategoryText ? 'border-red-500/50' : 'border-slate-200'}`}
                          />
                          {errors.otherCategoryText && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.otherCategoryText}</p>}
                        </div>
                      )}
                    </div>

                    {/* Primary Products Description */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-550 uppercase tracking-widest">Primary Products Description <span className="text-slate-400 normal-case">(10-500 chars)</span></label>
                      <input
                        type="text"
                        value={exporterProducts}
                        onChange={(e) => {
                          setExporterProducts(e.target.value);
                          if (errors.exporterProducts) setErrors({ ...errors, exporterProducts: null });
                        }}
                        placeholder="e.g. Premium ceramic coffee mugs for cafes and restaurants"
                        maxLength={500}
                        className={`w-full px-4 py-2.5 bg-white border rounded-xl text-xs placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.exporterProducts ? 'border-red-500/50' : 'border-slate-200'}`}
                      />
                      <div className="flex justify-between items-center">
                        {errors.exporterProducts && <p className="text-[10px] text-red-500 font-semibold">{errors.exporterProducts}</p>}
                        <span className="text-[9px] text-slate-400 ml-auto">{exporterProducts.trim().length}/500</span>
                      </div>
                    </div>

                    {/* Export Experience */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Export Experience</label>
                      <div className="grid grid-cols-3 gap-2.5">
                        {['Beginner', 'Intermediate', 'Experienced'].map((exp) => (
                          <button
                            key={exp}
                            type="button"
                            onClick={() => { setExporterExp(exp); if (errors.exporterExp) setErrors({ ...errors, exporterExp: null }); }}
                            className={`py-2 rounded-xl text-xs font-bold border transition-all cursor-pointer ${exporterExp === exp
                                ? 'bg-sky-500 text-white border-sky-400 shadow-md shadow-sky-500/10 scale-102'
                                : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                              }`}
                          >
                            {exp}
                          </button>
                        ))}
                      </div>
                      {errors.exporterExp && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.exporterExp}</p>}
                    </div>
                  </div>
                )}



                {/* RENDER LOGISTICS DETAILS */}
                {role === 'logistics' && (
                  <div className="space-y-5">
                    {/* Service Types (max 4) */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-555 uppercase tracking-widest">Service Types Offered <span className="text-slate-400 normal-case">(select 1-4)</span></label>
                      <div className="grid grid-cols-3 gap-2">
                        {['Air Freight', 'Sea Freight', 'Road Transport', 'Rail Transport', 'Customs Brokerage', 'Customs Clearance', 'Warehousing'].map((svc) => {
                          const active = logisticsServices.includes(svc);
                          const atMax = logisticsServices.length >= 4 && !active;
                          return (
                            <button
                              key={svc}
                              type="button"
                              disabled={atMax}
                              onClick={() => { toggleSelection(logisticsServices, setLogisticsServices, svc, 4); if (errors.logisticsServices) setErrors({ ...errors, logisticsServices: null }); }}
                              className={`py-1.5 px-1 rounded-xl text-xxs font-bold border transition-all cursor-pointer flex items-center justify-center gap-1.5 ${active
                                  ? 'bg-sky-500 text-white border-sky-400 shadow-md scale-102'
                                  : atMax
                                    ? 'bg-slate-50 border-slate-100 text-slate-300 cursor-not-allowed'
                                    : 'bg-white border-slate-200 text-slate-550 hover:bg-slate-50'
                                }`}
                            >
                              <span>{svc}</span>
                            </button>
                          );
                        })}
                      </div>
                      {errors.logisticsServices && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.logisticsServices}</p>}
                    </div>

                    {/* Service Regions (max 5) */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Service Regions <span className="text-slate-400 normal-case">(select 1-5)</span></label>
                      <div className="grid grid-cols-3 gap-2">
                        {['India (Domestic)', 'Southeast Asia', 'East Asia', 'Middle East', 'Europe', 'North America', 'South America', 'Africa', 'Oceania'].map((reg) => {
                          const active = logisticsRegions.includes(reg);
                          const atMax = logisticsRegions.length >= 5 && !active;
                          return (
                            <button
                              key={reg}
                              type="button"
                              disabled={atMax}
                              onClick={() => { toggleSelection(logisticsRegions, setLogisticsRegions, reg, 5); if (errors.logisticsRegions) setErrors({ ...errors, logisticsRegions: null }); }}
                              className={`py-1.5 px-1 rounded-xl text-xxs font-bold border transition-all cursor-pointer flex items-center justify-center gap-1.5 ${active
                                  ? 'bg-sky-500 text-white border-sky-400 shadow-md scale-102'
                                  : atMax
                                    ? 'bg-slate-50 border-slate-100 text-slate-300 cursor-not-allowed'
                                    : 'bg-white border-slate-200 text-slate-555 hover:bg-slate-50'
                                }`}
                            >
                              <span>{reg}</span>
                            </button>
                          );
                        })}
                      </div>
                      {errors.logisticsRegions && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.logisticsRegions}</p>}
                    </div>

                    {/* Business Registration Number */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Business Registration Number</label>
                      <input
                        type="text"
                        value={logisticsRegNo}
                        onChange={(e) => {
                          setLogisticsRegNo(e.target.value.toUpperCase());
                          if (errors.logisticsRegNo) setErrors({ ...errors, logisticsRegNo: null });
                        }}
                        placeholder="Enter GSTIN / Business Registration Number"
                        maxLength={20}
                        className={`w-full px-4 py-2 bg-white border rounded-xl text-xs placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 ${errors.logisticsRegNo ? 'border-red-500/50' : 'border-slate-200'}`}
                      />
                      {errors.logisticsRegNo && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.logisticsRegNo}</p>}
                    </div>

                    {/* Years of Experience */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Years of Experience</label>
                      <select
                        value={logisticsExperience}
                        onChange={(e) => { setLogisticsExperience(e.target.value); if (errors.logisticsExperience) setErrors({ ...errors, logisticsExperience: null }); }}
                        className={`w-full px-4 py-2 bg-white border rounded-xl text-xs text-slate-700 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all duration-200 cursor-pointer ${errors.logisticsExperience ? 'border-red-500/50' : 'border-slate-200'}`}
                      >
                        <option value="">Select experience</option>
                        <option value="Less than 1 year">Less than 1 year</option>
                        <option value="1-3 years">1-3 years</option>
                        <option value="3-5 years">3-5 years</option>
                        <option value="5-10 years">5-10 years</option>
                        <option value="10+ years">10+ years</option>
                      </select>
                      {errors.logisticsExperience && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.logisticsExperience}</p>}
                    </div>

                    {/* Live Tracking Support */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Live Tracking Support</label>
                      <div className="flex gap-4">
                        {['Yes', 'No'].map((opt) => (
                          <label key={opt} className="flex items-center gap-2 cursor-pointer select-none">
                            <input
                              type="radio"
                              name="trackingSupport"
                              value={opt}
                              checked={logisticsTracking === opt}
                              onChange={(e) => { setLogisticsTracking(e.target.value); if (errors.logisticsTracking) setErrors({ ...errors, logisticsTracking: null }); }}
                              className="w-3.5 h-3.5 accent-sky-500 cursor-pointer"
                            />
                            <span className="text-xs font-semibold text-slate-600">{opt}</span>
                          </label>
                        ))}
                      </div>
                      {errors.logisticsTracking && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.logisticsTracking}</p>}
                    </div>

                    {/* Cargo Insurance Support */}
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Cargo Insurance Support</label>
                      <div className="flex gap-4">
                        {['Yes', 'No'].map((opt) => (
                          <label key={opt} className="flex items-center gap-2 cursor-pointer select-none">
                            <input
                              type="radio"
                              name="cargoInsurance"
                              value={opt}
                              checked={logisticsInsurance === opt}
                              onChange={(e) => { setLogisticsInsurance(e.target.value); if (errors.logisticsInsurance) setErrors({ ...errors, logisticsInsurance: null }); }}
                              className="w-3.5 h-3.5 accent-sky-500 cursor-pointer"
                            />
                            <span className="text-xs font-semibold text-slate-600">{opt}</span>
                          </label>
                        ))}
                      </div>
                      {errors.logisticsInsurance && <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.logisticsInsurance}</p>}
                    </div>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex justify-between items-center pt-6 border-t border-slate-200">
                  <button
                    type="button"
                    onClick={handlePrevStep}
                    className="inline-flex items-center gap-1 px-4 py-2 text-xs font-bold border border-slate-200 bg-white/50 rounded-xl hover:text-slate-900 hover:border-slate-300 transition-all cursor-pointer"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                    Back
                  </button>
                  <button
                    type="button"
                    onClick={handleNextStep}
                    disabled={role === 'logistics' && (!logisticsServices.length || !logisticsRegions.length || !logisticsRegNo.trim() || !logisticsExperience)}
                    className="inline-flex items-center gap-1.5 px-6 py-2 font-bold text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 hover:scale-103 shadow-md rounded-xl transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
                  >
                    Continue
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>

              </div>
            )}

            {/* ==================================================== */}
            {/* STEP 3 CONTENT: CONFIRMATION & REVIEW */}
            {/* ==================================================== */}
            {step === 3 && (
              <div className="space-y-6 animate-slide-in-right">

                <div>
                  <h2 className="text-lg font-bold text-slate-900 tracking-tight">Review & Confirm</h2>
                  <p className="text-xs text-slate-500 mt-1 font-medium">Step 3 of 3 · Verify Profile Details</p>
                </div>

                {/* Summary Card */}
                <div className="bg-slate-50 border border-slate-200/80 p-5 rounded-2xl space-y-3.5">
                  <div className="flex justify-between items-center border-b border-slate-200 pb-2">
                    <h3 className="text-xs font-bold text-slate-500 uppercase tracking-widest">Account Summary</h3>
                    <button
                      type="button"
                      onClick={() => setStep(1)}
                      className="text-xs font-bold text-sky-600 hover:underline cursor-pointer"
                    >
                      Edit Profile
                    </button>
                  </div>

                  {/* Info Fields */}
                  <div className="space-y-2.5 text-xs font-semibold">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Full Name</span>
                      <span className="text-slate-800">{fullName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Company</span>
                      <span className="text-slate-800">{companyName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Email</span>
                      <span className="text-slate-800">{email}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Phone</span>
                      <span className="text-slate-800">{phone}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Base Country</span>
                      <span className="text-slate-800">{country}</span>
                    </div>
                    <div className="flex justify-between border-t border-slate-200 pt-2.5">
                      <span className="text-slate-400 font-bold uppercase text-[10px]">Assigned Role</span>
                      <span className="text-sky-600 font-extrabold capitalize text-[10px]">{role}</span>
                    </div>
                  </div>
                </div>

                {/* Submit Form */}
                <form onSubmit={handleRegisterSubmit} className="space-y-5">

                  {/* Checkbox agreement */}
                  <div className="flex flex-col gap-1">
                    <div className="flex items-start">
                      <label className="flex items-start gap-2.5 text-xs font-semibold text-slate-500 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={agreed}
                          onChange={(e) => {
                            setAgreed(e.target.checked);
                            if (errors.agreed) setErrors({ ...errors, agreed: null });
                          }}
                          className="mt-0.5 w-4 h-4 rounded border-slate-200 bg-white accent-sky-500 focus:ring-0 cursor-pointer"
                        />
                        <span>
                          I agree to the{' '}
                          <span className="text-sky-655 hover:text-sky-500 underline">Terms of Service</span>
                          {' '}and{' '}
                          <span className="text-sky-655 hover:text-sky-500 underline">Privacy Policy</span>.
                        </span>
                      </label>
                    </div>
                    {errors.agreed && (
                      <p className="text-[10px] text-red-500 font-semibold mt-1">{errors.agreed}</p>
                    )}
                  </div>

                  {/* Navigation and Register Actions */}
                  <div className="flex justify-between items-center pt-4 border-t border-slate-200">
                    <button
                      type="button"
                      onClick={handlePrevStep}
                      className="inline-flex items-center gap-1 px-4 py-2 text-xs font-bold border border-slate-200 bg-white/50 rounded-xl hover:text-slate-900 hover:border-slate-350 transition-all cursor-pointer"
                    >
                      <ArrowLeft className="w-3.5 h-3.5" />
                      Back
                    </button>

                    <button
                      type="submit"
                      disabled={loading}
                      className="inline-flex items-center justify-center gap-2 px-6 py-2.5 font-bold text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 hover:scale-103 active:scale-98 shadow-lg shadow-sky-500/10 rounded-xl transition-all disabled:opacity-50 cursor-pointer animate-[pulseGlow_3s_infinite]"
                    >
                      {loading ? (
                        <>
                          <Loader2 className="w-4 h-4 animate-spin" />
                          <span>Creating Account...</span>
                        </>
                      ) : (
                        <>
                          <span>Create Account</span>
                          <ShieldCheck className="w-4 h-4" />
                        </>
                      )}
                    </button>
                  </div>

                </form>

              </div>
            )}

          </div>
        </div>

      </div>

      {/* Embedded Styles for shake and pulses */}
      <style>{`
        @keyframes shake {
          0%, 100% { transform: translateX(0); }
          20%, 60% { transform: translateX(-6px); }
          40%, 80% { transform: translateX(6px); }
        }
        @keyframes pulseGlow {
          0% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.2); }
          50% { box-shadow: 0 0 0 10px rgba(59, 130, 246, 0); }
          100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
        }
        @keyframes slideInRight {
          from { opacity: 0; transform: translate3d(24px, 0, 0); }
          to { opacity: 1; transform: translate3d(0, 0, 0); }
        }
        .animate-slide-in-right {
          animation: slideInRight 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
      `}</style>
    </div>
  );
}
