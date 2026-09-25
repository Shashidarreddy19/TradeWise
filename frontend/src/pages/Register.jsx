import React, { useState, useCallback } from 'react';
import { Eye, EyeOff, User, Building, Mail, Phone, Globe, Lock, Briefcase, Truck, ShieldCheck, ArrowLeft, ArrowRight, Loader2, Check, X, Sparkles } from 'lucide-react';
import { authApi, setToken, setUser } from '../services';
import ThemeToggle from '../components/ThemeToggle';

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

  // Real-time duplicate checking
  const [checkingEmail, setCheckingEmail] = useState(false);
  const [checkingPhone, setCheckingPhone] = useState(false);

  const handleEmailBlur = async () => {
    const trimmed = email.trim().toLowerCase();
    if (!trimmed || !EMAIL_REGEX.test(trimmed)) return;
    try {
      setCheckingEmail(true);
      const res = await authApi.checkEmail(trimmed);
      const available = res?.data?.available ?? res?.available;
      if (available === false) {
        setErrors(prev => ({ ...prev, email: res?.data?.message || res?.message || 'This email is already registered.' }));
      }
    } catch {
      // Network or API check fallback gracefully
    } finally {
      setCheckingEmail(false);
    }
  };

  const handlePhoneBlur = async () => {
    const clean = phone.trim().replace(/[\s\-]/g, '');
    if (!clean) return;
    try {
      setCheckingPhone(true);
      const res = await authApi.checkPhone(clean);
      const available = res?.data?.available ?? res?.available;
      if (available === false) {
        setErrors(prev => ({ ...prev, phone: res?.data?.message || res?.message || 'This phone number is already registered.' }));
      }
    } catch {
      // Network or API check fallback gracefully
    } finally {
      setCheckingPhone(false);
    }
  };

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
        // Map backend field names to frontend error keys
        const mapped = {};
        Object.entries(fieldErrors).forEach(([key, msg]) => {
          if (key === 'gstNumber') mapped.exporterGst = msg;
          else if (key === 'businessType') mapped.exporterType = msg;
          else if (key === 'productCategories') mapped.exporterCategories = msg;
          else if (key === 'productDescription') mapped.exporterProducts = msg;
          else if (key === 'exportExperience') mapped.exporterExp = msg;
          else mapped[key] = msg;
        });
        setErrors(prev => ({ ...prev, ...mapped }));
        // Navigate to the step containing the first errored field
        const step1Fields = ['name', 'email', 'phone', 'password', 'confirmPassword', 'country', 'companyName'];
        const step2Fields = ['exporterGst', 'exporterType', 'exporterCategories', 'exporterProducts', 'exporterExp'];
        const hasStep1Error = Object.keys(mapped).some(f => step1Fields.includes(f));
        const hasStep2Error = Object.keys(mapped).some(f => step2Fields.includes(f));
        if (hasStep1Error) setStep(1);
        else if (hasStep2Error) setStep(2);
      }
      const errorMsg = err.data?.data
        ? Object.values(err.data.data).join('. ')
        : (err.message || 'Registration failed. Please try again.');
      triggerToast('error', errorMsg);
      setShake(true);
      setTimeout(() => setShake(false), 500);
    } finally {
      setLoading(false);
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col relative font-sans overflow-hidden">

      {/* Background Subtle Warm Glow */}
      <div className="absolute inset-0 pointer-events-none z-0">
        <div className="absolute top-[10%] right-[10%] w-[500px] h-[500px] bg-primary/5 rounded-full blur-[120px]"></div>
      </div>

      {/* Toast Alert */}
      {toast && (
        <div className={`fixed top-6 right-6 z-50 flex items-center gap-2.5 px-4 py-3 rounded-xl border backdrop-blur-md shadow-lg animate-scale-in text-xs font-semibold ${
          toast.type === 'success'
            ? 'bg-card border-emerald-500/30 text-emerald-600 dark:text-emerald-400'
            : 'bg-card border-destructive/30 text-destructive'
        }`}>
          <div className={`w-2 h-2 rounded-full ${toast.type === 'success' ? 'bg-emerald-500' : 'bg-destructive'}`}></div>
          <span>{toast.message}</span>
        </div>
      )}

      {/* Top Bar with Back button and ThemeToggle */}
      <div className="absolute top-6 left-6 right-6 flex items-center justify-between z-30">
        <button
          onClick={() => onNavigate('/')}
          className="btn-outline text-xs px-3.5 py-1.5 cursor-pointer"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Back to Home
        </button>

        <ThemeToggle variant="simple" />
      </div>

      {/* Main Split Content */}
      <div className="flex-grow grid grid-cols-1 lg:grid-cols-12 min-h-screen relative z-10 pt-16 lg:pt-0">

        {/* Left Side: Editorial Banner */}
        <div className="hidden lg:flex lg:col-span-6 bg-muted/30 border-r border-border flex-col items-center justify-center p-12 relative overflow-hidden">

          {/* Subtle geometric circles */}
          <div className="absolute w-[400px] h-[400px] border border-border/60 rounded-full z-0"></div>
          <div className="absolute w-[550px] h-[550px] border border-dashed border-border/40 rounded-full z-0 animate-pulse-slow"></div>

          {/* Central Claude-style Aesthetic Illustration & Narrative */}
          <div className="relative z-10 flex flex-col items-center max-w-md text-center space-y-4">
            
            <div className="w-14 h-14 rounded-2xl bg-primary/10 text-primary border border-primary/20 flex items-center justify-center shadow-xs">
              {role === 'logistics' ? <Truck className="w-7 h-7" /> : <Globe className="w-7 h-7" />}
            </div>

            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary border border-primary/20 text-[10px] font-semibold tracking-wider uppercase">
              {role === 'logistics' ? 'Logistics Partner Onboarding' : 'Global Exporter Onboarding'}
            </div>

            <h2 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-foreground leading-tight">
              {role === 'logistics'
                ? 'Global Freight & Dispatch Network'
                : 'Compliance Intelligence Built for Global Growth'}
            </h2>

            <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed max-w-sm">
              {role === 'logistics'
                ? '"TradeBridge connected our logistics fleet with verified international exporters, streamlining route dispatch and customs clearance documentation."'
                : '"TradeBridge allowed us to index new custom tariffs and clear our freight compliance audits in record time."'}
            </p>

            <div className="pt-2 flex items-center gap-3 text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-primary" />
                Verified Network
              </span>
              <span>•</span>
              <span className="flex items-center gap-1.5">
                <Check className="w-3.5 h-3.5 text-primary" />
                Zero Setup Fees
              </span>
              <span>•</span>
              <span className="flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-primary" />
                AI-Powered
              </span>
            </div>
          </div>
        </div>

        {/* Right Side: Step-by-Step Form Column */}
        <div className="lg:col-span-6 flex flex-col items-center justify-center p-6 sm:p-12 relative overflow-y-auto max-h-screen">

          {/* Registration Card Wrapper */}
          <div className={`w-full max-w-lg bg-card border border-border rounded-2xl shadow-sm p-7 sm:p-8 transition-transform ${
            shake ? 'animate-[shake_0.4s_ease-in-out]' : ''
          }`}>

            {/* Step Indicator Header */}
            <div className="mb-6">
              <div className="flex items-center justify-between max-w-xs mx-auto relative mb-3">
                {/* Connection Line Background */}
                <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 h-0.5 bg-border z-0"></div>

                {/* Connection Line Active */}
                <div
                  className="absolute left-0 top-1/2 -translate-y-1/2 h-0.5 bg-primary transition-all duration-300 z-0"
                  style={{ width: step === 1 ? '0%' : step === 2 ? '50%' : '100%' }}
                ></div>

                {/* Step 1 Circle */}
                <button
                  onClick={() => step > 1 && setStep(1)}
                  className={`relative z-10 w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs border transition-all cursor-pointer ${
                    step === 1
                      ? 'bg-primary border-primary text-primary-foreground shadow-xs'
                      : step > 1
                        ? 'bg-emerald-600 border-emerald-600 text-white'
                        : 'bg-card border-border text-muted-foreground'
                  }`}
                >
                  {step > 1 ? <Check className="w-4 h-4" /> : '1'}
                </button>

                {/* Step 2 Circle */}
                <button
                  onClick={() => step > 2 && setStep(2)}
                  className={`relative z-10 w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs border transition-all cursor-pointer ${
                    step === 2
                      ? 'bg-primary border-primary text-primary-foreground shadow-xs'
                      : step > 2
                        ? 'bg-emerald-600 border-emerald-600 text-white'
                        : 'bg-card border-border text-muted-foreground'
                  }`}
                >
                  {step > 2 ? <Check className="w-4 h-4" /> : '2'}
                </button>

                {/* Step 3 Circle */}
                <div
                  className={`relative z-10 w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs border transition-all ${
                    step === 3
                      ? 'bg-primary border-primary text-primary-foreground shadow-xs'
                      : 'bg-card border-border text-muted-foreground'
                  }`}
                >
                  3
                </div>
              </div>

              <div className="flex justify-between max-w-xs mx-auto text-[10px] font-semibold text-muted-foreground uppercase tracking-wider text-center px-1">
                <span className={step >= 1 ? 'text-primary font-bold' : ''}>Basic</span>
                <span className={step >= 2 ? 'text-primary font-bold' : ''}>Details</span>
                <span className={step >= 3 ? 'text-primary font-bold' : ''}>Confirm</span>
              </div>
            </div>

            {/* ==================================================== */}
            {/* STEP 1 CONTENT: BASIC DETAILS & ROLE SELECTION */}
            {/* ==================================================== */}
            {step === 1 && (
              <div className="space-y-5 animate-slide-in-right">

                <div>
                  <h2 className="font-display text-xl font-bold text-foreground tracking-tight">Create Your Account</h2>
                  <p className="text-xs text-muted-foreground mt-0.5">Step 1 of 3 · Profile and Role Settings</p>
                </div>

                {/* Double-Column Fields Grid */}
                <div className="grid grid-cols-2 gap-x-4 gap-y-3.5">

                  {/* Full Name */}
                  <div className="col-span-1 space-y-1.5 relative">
                    <label className="text-xs font-medium text-foreground block">Full Name</label>
                    <div className="relative flex items-center">
                      <User className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                      <input
                        type="text"
                        value={fullName}
                        onChange={(e) => {
                          setFullName(e.target.value);
                          if (errors.fullName) setErrors({ ...errors, fullName: null });
                        }}
                        placeholder="John Doe"
                        className={`input-claude pl-10 pr-3.5 ${errors.fullName ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                      />
                    </div>
                    {errors.fullName && (
                      <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.fullName}</p>
                    )}
                  </div>

                  {/* Company Name */}
                  <div className="col-span-1 space-y-1.5 relative">
                    <label className="text-xs font-medium text-foreground block">Company Name</label>
                    <div className="relative flex items-center">
                      <Building className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                      <input
                        type="text"
                        value={companyName}
                        onChange={(e) => {
                          setCompanyName(e.target.value);
                          if (errors.companyName) setErrors({ ...errors, companyName: null });
                        }}
                        placeholder="Acme Ltd"
                        className={`input-claude pl-10 pr-3.5 ${errors.companyName ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                      />
                    </div>
                    {errors.companyName && (
                      <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.companyName}</p>
                    )}
                  </div>

                  {/* Email Address */}
                  <div className="col-span-1 space-y-1.5 relative">
                    <div className="flex justify-between items-center">
                      <label className="text-xs font-medium text-foreground block">Email</label>
                      {checkingEmail && <span className="text-[10px] text-primary font-medium">Checking...</span>}
                    </div>
                    <div className="relative flex items-center">
                      <Mail className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                      <input
                        type="email"
                        value={email}
                        onBlur={handleEmailBlur}
                        onChange={(e) => {
                          setEmail(e.target.value);
                          if (errors.email) setErrors({ ...errors, email: null });
                        }}
                        placeholder="name@company.com"
                        className={`input-claude pl-10 pr-3.5 ${errors.email ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                      />
                    </div>
                    {errors.email && (
                      <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.email}</p>
                    )}
                  </div>

                  {/* Phone Number */}
                  <div className="col-span-1 space-y-1.5 relative">
                    <div className="flex justify-between items-center">
                      <label className="text-xs font-medium text-foreground block">Phone Number</label>
                      {checkingPhone && <span className="text-[10px] text-primary font-medium">Checking...</span>}
                    </div>
                    <div className="relative flex items-center">
                      <Phone className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                      <input
                        type="tel"
                        value={phone}
                        onBlur={handlePhoneBlur}
                        onChange={(e) => {
                          setPhone(e.target.value);
                          if (errors.phone) setErrors({ ...errors, phone: null });
                        }}
                        placeholder="+91 9876543210"
                        className={`input-claude pl-10 pr-3.5 ${errors.phone ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                      />
                    </div>
                    {errors.phone && (
                      <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.phone}</p>
                    )}
                  </div>

                  {/* Country */}
                  <div className="col-span-2 space-y-1.5 relative">
                    <label className="text-xs font-medium text-foreground block">Base Country</label>
                    <div className="relative flex items-center">
                      <Globe className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                      <select
                        value={country}
                        onChange={(e) => setCountry(e.target.value)}
                        className="input-claude pl-10 pr-8 cursor-pointer appearance-none"
                      >
                        <option value="">Select Country</option>
                        {COUNTRY_LIST.map(c => (
                          <option key={c.code} value={c.name}>{c.name}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  {/* Password */}
                  <div className="col-span-1 space-y-1.5 relative">
                    <label className="text-xs font-medium text-foreground block">Password</label>
                    <div className="relative flex items-center">
                      <Lock className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => {
                          setPassword(e.target.value);
                          if (errors.password) setErrors({ ...errors, password: null });
                        }}
                        placeholder="Min 8 chars"
                        className={`input-claude pl-10 pr-10 ${errors.password ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 p-1 text-muted-foreground hover:text-foreground cursor-pointer rounded-md focus:outline-none transition-colors"
                      >
                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    {errors.password && (
                      <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.password}</p>
                    )}
                  </div>

                  {/* Confirm Password */}
                  <div className="col-span-1 space-y-1.5 relative">
                    <label className="text-xs font-medium text-foreground block">Verify Password</label>
                    <div className="relative flex items-center">
                      <Lock className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                      <input
                        type={showConfirmPassword ? 'text' : 'password'}
                        value={confirmPassword}
                        onChange={(e) => {
                          setConfirmPassword(e.target.value);
                          if (errors.confirmPassword) setErrors({ ...errors, confirmPassword: null });
                        }}
                        placeholder="Verify password"
                        className={`input-claude pl-10 pr-10 ${errors.confirmPassword ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="absolute right-3 p-1 text-muted-foreground hover:text-foreground cursor-pointer rounded-md focus:outline-none transition-colors"
                      >
                        {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    {errors.confirmPassword && (
                      <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.confirmPassword}</p>
                    )}
                  </div>

                  {/* Live password strength checklist spanning both columns symmetrically */}
                  {password.length > 0 && (
                    <div className="col-span-2 bg-muted/50 border border-border rounded-xl p-3 space-y-1.5">
                      <div className="grid grid-cols-2 gap-x-3 gap-y-1">
                        {PASSWORD_RULES.map(rule => {
                          const passed = rule.test(password);
                          return (
                            <div key={rule.id} className="flex items-center gap-1.5">
                              {passed
                                ? <Check className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                                : <X className="w-3.5 h-3.5 text-muted-foreground/50 shrink-0" />}
                              <span className={`text-[10px] font-medium leading-tight ${passed ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'}`}>
                                {rule.label}
                              </span>
                            </div>
                          );
                        })}
                        {confirmPassword.length > 0 && (
                          <div className="flex items-center gap-1.5">
                            {password === confirmPassword
                              ? <Check className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                              : <X className="w-3.5 h-3.5 text-destructive shrink-0" />}
                            <span className={`text-[10px] font-medium leading-tight ${password === confirmPassword ? 'text-emerald-600 dark:text-emerald-400' : 'text-destructive'}`}>
                              {password === confirmPassword ? 'Passwords match' : 'Passwords do not match'}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                {/* Role Selection Horizontal Cards */}
                <div className="space-y-2">
                  <label className="text-xs font-medium text-foreground">Select Account Role</label>
                  <div className="grid grid-cols-2 gap-3">
                    {/* Exporter Card */}
                    <button
                      type="button"
                      onClick={() => setRole('exporter')}
                      className={`p-3.5 rounded-xl border transition-all cursor-pointer flex flex-col items-center justify-center text-center gap-1.5 ${
                        role === 'exporter'
                          ? 'border-primary bg-primary/10 text-primary shadow-xs'
                          : 'border-border bg-card text-muted-foreground hover:text-foreground hover:border-border/80'
                      }`}
                    >
                      <Briefcase className="w-5 h-5" />
                      <span className="text-xs font-semibold">Exporter</span>
                    </button>

                    {/* Logistics Card */}
                    <button
                      type="button"
                      onClick={() => setRole('logistics')}
                      className={`p-3.5 rounded-xl border transition-all cursor-pointer flex flex-col items-center justify-center text-center gap-1.5 ${
                        role === 'logistics'
                          ? 'border-primary bg-primary/10 text-primary shadow-xs'
                          : 'border-border bg-card text-muted-foreground hover:text-foreground hover:border-border/80'
                      }`}
                    >
                      <Truck className="w-5 h-5" />
                      <span className="text-xs font-semibold">Logistics</span>
                    </button>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex justify-between items-center pt-4 border-t border-border">
                  <button
                    type="button"
                    onClick={() => onNavigate('/login')}
                    className="text-xs font-medium text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                  >
                    Already have an account? <span className="text-primary font-semibold hover:underline">Sign In</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleNextStep}
                    disabled={!fullName || !companyName || !email || !phone || !password || !confirmPassword}
                    className="btn-primary px-6 py-2.5 text-xs font-semibold cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <span>Continue</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>

              </div>
            )}

            {/* ==================================================== */}
            {/* STEP 2 CONTENT: ROLE SPECIFIC INFORMATION */}
            {/* ==================================================== */}
            {step === 2 && (
              <div className="space-y-5 animate-slide-in-right">

                <div>
                  <h2 className="font-display text-xl font-bold text-foreground tracking-tight">Tell Us About Your Business</h2>
                  <p className="text-xs text-muted-foreground mt-0.5">Step 2 of 3 · {role === 'exporter' ? 'Exporter Details' : 'Logistics Partner Details'}</p>
                </div>

                {/* RENDER EXPORTER DETAILS */}
                {role === 'exporter' && (
                  <div className="space-y-4">
                    {/* GST / Business Registration No */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">
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
                        className={`input-claude ${errors.exporterGst ? 'border-destructive' : ''}`}
                      />
                      {errors.exporterGst && <p className="text-[11px] text-destructive font-medium">{errors.exporterGst}</p>}
                    </div>

                    {/* Business Type */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">Business Type</label>
                      <div className="grid grid-cols-3 gap-2">
                        {['Manufacturer', 'Trader', 'Both'].map((t) => (
                          <button
                            key={t}
                            type="button"
                            onClick={() => { setExporterType(t); if (errors.exporterType) setErrors({ ...errors, exporterType: null }); }}
                            className={`py-2 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                              exporterType === t
                                ? 'bg-primary text-primary-foreground border-primary shadow-xs'
                                : 'bg-card border-border text-muted-foreground hover:text-foreground hover:bg-accent'
                            }`}
                          >
                            {t}
                          </button>
                        ))}
                      </div>
                      {errors.exporterType && <p className="text-[11px] text-destructive font-medium mt-1">{errors.exporterType}</p>}
                    </div>

                    {/* Product Category Multi-Select (max 3) */}
                    <div className="space-y-1.5">
                      <div className="flex items-center justify-between">
                        <label className="text-xs font-medium text-foreground">
                          Product Categories <span className="text-muted-foreground font-normal">(select 1-3)</span>
                        </label>
                        <span className="text-[11px] font-semibold text-primary">{exporterCategories.length}/3 selected</span>
                      </div>
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
                              className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-all cursor-pointer flex items-center gap-1.5 ${
                                active
                                  ? 'bg-primary border-primary text-primary-foreground shadow-xs'
                                  : atMax
                                    ? 'bg-muted/40 border-border text-muted-foreground/40 cursor-not-allowed'
                                    : 'bg-card border-border text-foreground hover:bg-accent'
                              }`}
                            >
                              <span>{cat}</span>
                              {active && <X className="w-3 h-3 text-primary-foreground" />}
                            </button>
                          );
                        })}
                      </div>
                      {errors.exporterCategories && <p className="text-[11px] text-destructive font-medium mt-1">{errors.exporterCategories}</p>}

                      {/* "Others" specification field */}
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
                            className={`input-claude ${errors.otherCategoryText ? 'border-destructive' : ''}`}
                          />
                          {errors.otherCategoryText && <p className="text-[11px] text-destructive font-medium mt-1">{errors.otherCategoryText}</p>}
                        </div>
                      )}
                    </div>

                    {/* Primary Products Description */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">
                        Primary Products Description <span className="text-muted-foreground font-normal">(10-500 chars)</span>
                      </label>
                      <input
                        type="text"
                        value={exporterProducts}
                        onChange={(e) => {
                          setExporterProducts(e.target.value);
                          if (errors.exporterProducts) setErrors({ ...errors, exporterProducts: null });
                        }}
                        placeholder="e.g. Premium ceramic coffee mugs for cafes and restaurants"
                        maxLength={500}
                        className={`input-claude ${errors.exporterProducts ? 'border-destructive' : ''}`}
                      />
                      <div className="flex justify-between items-center">
                        {errors.exporterProducts && <p className="text-[11px] text-destructive font-medium">{errors.exporterProducts}</p>}
                        <span className="text-[10px] text-muted-foreground ml-auto">{exporterProducts.trim().length}/500</span>
                      </div>
                    </div>

                    {/* Export Experience */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">Export Experience</label>
                      <div className="grid grid-cols-3 gap-2">
                        {['Beginner', 'Intermediate', 'Experienced'].map((exp) => (
                          <button
                            key={exp}
                            type="button"
                            onClick={() => { setExporterExp(exp); if (errors.exporterExp) setErrors({ ...errors, exporterExp: null }); }}
                            className={`py-2 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                              exporterExp === exp
                                ? 'bg-primary text-primary-foreground border-primary shadow-xs'
                                : 'bg-card border-border text-muted-foreground hover:text-foreground hover:bg-accent'
                            }`}
                          >
                            {exp}
                          </button>
                        ))}
                      </div>
                      {errors.exporterExp && <p className="text-[11px] text-destructive font-medium mt-1">{errors.exporterExp}</p>}
                    </div>
                  </div>
                )}

                {/* RENDER LOGISTICS DETAILS */}
                {role === 'logistics' && (
                  <div className="space-y-4">
                    {/* Service Types (max 6) */}
                    <div className="space-y-1.5">
                      <div className="flex items-center justify-between">
                        <label className="text-xs font-medium text-foreground">
                          Service Types Offered <span className="text-muted-foreground font-normal">(select 1-6)</span>
                        </label>
                        <span className="text-[11px] font-semibold text-primary">{logisticsServices.length}/6 selected</span>
                      </div>
                      <div className="grid grid-cols-3 gap-2">
                        {['Air Freight', 'Sea Freight', 'Road Transport', 'Rail Transport', 'Customs Clearance', 'Door-to-Door Delivery', 'Warehousing', 'Cargo Insurance', 'Express Courier'].map((svc) => {
                          const active = logisticsServices.includes(svc);
                          const atMax = logisticsServices.length >= 6 && !active;
                          return (
                            <button
                              key={svc}
                              type="button"
                              disabled={atMax}
                              onClick={() => { toggleSelection(logisticsServices, setLogisticsServices, svc, 6); if (errors.logisticsServices) setErrors({ ...errors, logisticsServices: null }); }}
                              className={`py-2 px-2 rounded-xl text-xs font-medium border transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                                active
                                  ? 'bg-primary text-primary-foreground border-primary shadow-xs'
                                  : atMax
                                    ? 'bg-muted/40 border-border text-muted-foreground/40 cursor-not-allowed'
                                    : 'bg-card border-border text-muted-foreground hover:text-foreground hover:bg-accent'
                              }`}
                            >
                              <span>{svc}</span>
                              {active && <Check className="w-3.5 h-3.5 text-primary-foreground shrink-0" />}
                            </button>
                          );
                        })}
                      </div>
                      {errors.logisticsServices && <p className="text-[11px] text-destructive font-medium mt-1">{errors.logisticsServices}</p>}
                    </div>

                    {/* Service Regions (max 5) */}
                    <div className="space-y-1.5">
                      <div className="flex items-center justify-between">
                        <label className="text-xs font-medium text-foreground">
                          Service Regions <span className="text-muted-foreground font-normal">(select 1-5)</span>
                        </label>
                        <span className="text-[11px] font-semibold text-primary">{logisticsRegions.length}/5 selected</span>
                      </div>
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
                              className={`py-2 px-2 rounded-xl text-xs font-medium border transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                                active
                                  ? 'bg-primary text-primary-foreground border-primary shadow-xs'
                                  : atMax
                                    ? 'bg-muted/40 border-border text-muted-foreground/40 cursor-not-allowed'
                                    : 'bg-card border-border text-muted-foreground hover:text-foreground hover:bg-accent'
                              }`}
                            >
                              <span>{reg}</span>
                              {active && <Check className="w-3.5 h-3.5 text-primary-foreground shrink-0" />}
                            </button>
                          );
                        })}
                      </div>
                      {errors.logisticsRegions && <p className="text-[11px] text-destructive font-medium mt-1">{errors.logisticsRegions}</p>}
                    </div>

                    {/* Business Registration Number */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">Business Registration Number</label>
                      <input
                        type="text"
                        value={logisticsRegNo}
                        onChange={(e) => {
                          setLogisticsRegNo(e.target.value.toUpperCase());
                          if (errors.logisticsRegNo) setErrors({ ...errors, logisticsRegNo: null });
                        }}
                        placeholder="Enter GSTIN / Business Registration Number"
                        maxLength={25}
                        className={`input-claude ${errors.logisticsRegNo ? 'border-destructive' : ''}`}
                      />
                      {errors.logisticsRegNo && <p className="text-[11px] text-destructive font-medium mt-1">{errors.logisticsRegNo}</p>}
                    </div>

                    {/* Years of Experience */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">Years of Experience</label>
                      <select
                        value={logisticsExperience}
                        onChange={(e) => { setLogisticsExperience(e.target.value); if (errors.logisticsExperience) setErrors({ ...errors, logisticsExperience: null }); }}
                        className={`input-claude cursor-pointer ${errors.logisticsExperience ? 'border-destructive' : ''}`}
                      >
                        <option value="">Select experience</option>
                        <option value="Less than 1 year">Less than 1 year</option>
                        <option value="1-3 years">1-3 years</option>
                        <option value="3-5 years">3-5 years</option>
                        <option value="5-10 years">5-10 years</option>
                        <option value="10+ years">10+ years</option>
                      </select>
                      {errors.logisticsExperience && <p className="text-[11px] text-destructive font-medium mt-1">{errors.logisticsExperience}</p>}
                    </div>

                    {/* Live Tracking Support */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">Live Tracking Support</label>
                      <div className="flex gap-4">
                        {['Yes', 'No'].map((opt) => (
                          <label key={opt} className="flex items-center gap-2 cursor-pointer select-none">
                            <input
                              type="radio"
                              name="trackingSupport"
                              value={opt}
                              checked={logisticsTracking === opt}
                              onChange={(e) => { setLogisticsTracking(e.target.value); if (errors.logisticsTracking) setErrors({ ...errors, logisticsTracking: null }); }}
                              className="w-4 h-4 accent-primary cursor-pointer"
                            />
                            <span className="text-xs font-medium text-foreground">{opt}</span>
                          </label>
                        ))}
                      </div>
                      {errors.logisticsTracking && <p className="text-[11px] text-destructive font-medium mt-1">{errors.logisticsTracking}</p>}
                    </div>

                    {/* Cargo Insurance Support */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-medium text-foreground">Cargo Insurance Support</label>
                      <div className="flex gap-4">
                        {['Yes', 'No'].map((opt) => (
                          <label key={opt} className="flex items-center gap-2 cursor-pointer select-none">
                            <input
                              type="radio"
                              name="cargoInsurance"
                              value={opt}
                              checked={logisticsInsurance === opt}
                              onChange={(e) => { setLogisticsInsurance(e.target.value); if (errors.logisticsInsurance) setErrors({ ...errors, logisticsInsurance: null }); }}
                              className="w-4 h-4 accent-primary cursor-pointer"
                            />
                            <span className="text-xs font-medium text-foreground">{opt}</span>
                          </label>
                        ))}
                      </div>
                      {errors.logisticsInsurance && <p className="text-[11px] text-destructive font-medium mt-1">{errors.logisticsInsurance}</p>}
                    </div>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex justify-between items-center pt-5 border-t border-border">
                  <button
                    type="button"
                    onClick={handlePrevStep}
                    className="btn-outline text-xs px-4 py-2 cursor-pointer"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                    Back
                  </button>
                  <button
                    type="button"
                    onClick={handleNextStep}
                    className="btn-primary text-xs px-6 py-2.5 cursor-pointer"
                  >
                    <span>Continue</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>

              </div>
            )}

            {/* ==================================================== */}
            {/* STEP 3 CONTENT: CONFIRMATION & REVIEW */}
            {/* ==================================================== */}
            {step === 3 && (
              <div className="space-y-5 animate-slide-in-right">

                <div>
                  <h2 className="font-display text-xl font-bold text-foreground tracking-tight">Review & Confirm</h2>
                  <p className="text-xs text-muted-foreground mt-0.5">Step 3 of 3 · Verify Profile Details</p>
                </div>

                {/* Summary Card: Account Information */}
                <div className="bg-muted/40 border border-border p-4 sm:p-5 rounded-2xl space-y-3">
                  <div className="flex justify-between items-center border-b border-border pb-2">
                    <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Account Information</h3>
                    <button
                      type="button"
                      onClick={() => setStep(1)}
                      className="text-xs font-semibold text-primary hover:underline cursor-pointer"
                    >
                      Edit Profile
                    </button>
                  </div>

                  {/* Info Fields */}
                  <div className="space-y-2 text-xs font-medium">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Full Name</span>
                      <span className="text-foreground font-semibold">{fullName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Company</span>
                      <span className="text-foreground font-semibold">{companyName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Email</span>
                      <span className="text-foreground font-semibold">{email}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Phone</span>
                      <span className="text-foreground font-semibold">{phone}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Base Country</span>
                      <span className="text-foreground font-semibold">{country}</span>
                    </div>
                    <div className="flex justify-between border-t border-border pt-2">
                      <span className="text-muted-foreground font-semibold uppercase text-[10px]">Assigned Role</span>
                      <span className="text-primary font-bold capitalize text-xs">{role}</span>
                    </div>
                  </div>
                </div>

                {/* Summary Card: Business & Logistics Profile */}
                <div className="bg-muted/40 border border-border p-4 sm:p-5 rounded-2xl space-y-3">
                  <div className="flex justify-between items-center border-b border-border pb-2">
                    <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      {role === 'exporter' ? 'Exporter Profile' : 'Logistics Partner Profile'}
                    </h3>
                    <button
                      type="button"
                      onClick={() => setStep(2)}
                      className="text-xs font-semibold text-primary hover:underline cursor-pointer"
                    >
                      Edit Details
                    </button>
                  </div>

                  <div className="space-y-2 text-xs font-medium">
                    {role === 'exporter' ? (
                      <>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Registration / GST</span>
                          <span className="text-foreground font-semibold">{exporterGst || 'N/A'}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Business Type</span>
                          <span className="text-foreground font-semibold">{exporterType || 'N/A'}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Categories</span>
                          <span className="text-foreground font-semibold text-right max-w-[200px] truncate">
                            {exporterCategories.join(', ') || 'None selected'}
                            {exporterCategories.includes('Others') && otherCategoryText ? ` (${otherCategoryText})` : ''}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Experience</span>
                          <span className="text-foreground font-semibold">{exporterExp || 'N/A'}</span>
                        </div>
                        {exporterProducts && (
                          <div className="flex flex-col gap-1 border-t border-border pt-2">
                            <span className="text-muted-foreground text-[10px]">Primary Products</span>
                            <span className="text-foreground text-[11px] leading-relaxed line-clamp-2">{exporterProducts}</span>
                          </div>
                        )}
                      </>
                    ) : (
                      <>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Registration No</span>
                          <span className="text-foreground font-semibold">{logisticsRegNo || 'N/A'}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Experience</span>
                          <span className="text-foreground font-semibold">{logisticsExperience || 'N/A'}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Services</span>
                          <span className="text-foreground font-semibold text-right max-w-[200px] truncate">{logisticsServices.join(', ') || 'None'}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Regions</span>
                          <span className="text-foreground font-semibold text-right max-w-[200px] truncate">{logisticsRegions.join(', ') || 'None'}</span>
                        </div>
                        <div className="flex justify-between border-t border-border pt-2">
                          <span className="text-muted-foreground">Live Tracking</span>
                          <span className={`text-xs font-bold ${logisticsTracking === 'Yes' ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'}`}>{logisticsTracking}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Cargo Insurance</span>
                          <span className={`text-xs font-bold ${logisticsInsurance === 'Yes' ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'}`}>{logisticsInsurance}</span>
                        </div>
                      </>
                    )}
                  </div>
                </div>

                {/* Submit Form */}
                <form onSubmit={handleRegisterSubmit} className="space-y-4">

                  {/* Checkbox agreement */}
                  <div className="flex flex-col gap-1">
                    <label className="flex items-start gap-2.5 text-xs text-muted-foreground cursor-pointer select-none">
                      <input
                        type="checkbox"
                        checked={agreed}
                        onChange={(e) => {
                          setAgreed(e.target.checked);
                          if (errors.agreed) setErrors({ ...errors, agreed: null });
                        }}
                        className="mt-0.5 w-4 h-4 rounded border-border bg-card accent-primary cursor-pointer"
                      />
                      <span>
                        I agree to the{' '}
                        <span className="text-primary font-medium hover:underline">Terms of Service</span>
                        {' '}and{' '}
                        <span className="text-primary font-medium hover:underline">Privacy Policy</span>.
                      </span>
                    </label>
                    {errors.agreed && (
                      <p className="text-[11px] text-destructive font-medium mt-1">{errors.agreed}</p>
                    )}
                  </div>

                  {/* Navigation and Register Actions */}
                  <div className="flex justify-between items-center pt-4 border-t border-border">
                    <button
                      type="button"
                      onClick={handlePrevStep}
                      className="btn-outline text-xs px-4 py-2 cursor-pointer"
                    >
                      <ArrowLeft className="w-3.5 h-3.5" />
                      Back
                    </button>

                    <button
                      type="submit"
                      disabled={loading}
                      className="btn-primary px-6 py-2.5 text-xs font-semibold cursor-pointer disabled:opacity-50"
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
