import React, { useState } from 'react';
import { Eye, EyeOff, Mail, Lock, ArrowLeft, Loader2, Sparkles, Shield, Globe, Truck, X, Check } from 'lucide-react';
import { authApi, setToken, setUser } from '../services';
import ThemeToggle from '../components/ThemeToggle';

export default function Login({ onNavigate }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [role, setRole] = useState('exporter'); // 'exporter', 'logistics'
  const [rememberMe, setRememberMe] = useState(false);

  // Validation & Error States
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [shake, setShake] = useState(false);
  const [toast, setToast] = useState(null); // { type: 'success'|'error', message: '' }

  const triggerToast = (type, message) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  };

  const handleValidation = () => {
    const newErrors = {};
    const trimmedEmail = (email || '').trim().toLowerCase();
    if (!trimmedEmail) {
      newErrors.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(trimmedEmail)) {
      newErrors.email = 'Please enter a valid email address.';
    }

    if (!password) {
      newErrors.password = 'Password is required.';
    } else if (password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    if (!handleValidation()) {
      setShake(true);
      setTimeout(() => setShake(false), 500);
      triggerToast('error', 'Please correct the highlighted form errors.');
      return;
    }

    setLoading(true);

    try {
      const response = await authApi.login({
        email: email.trim().toLowerCase(),
        password,
      });
      const authData = response.data;

      setToken(authData.token, rememberMe);
      setUser({
        userId: authData.userId,
        name: authData.name,
        email: authData.email,
        role: authData.role,
      }, rememberMe);

      if (rememberMe) {
        sessionStorage.removeItem('trade_token');
        sessionStorage.removeItem('trade_user');
      } else {
        localStorage.removeItem('trade_token');
        localStorage.removeItem('trade_user');
      }

      triggerToast('success', 'Authenticated successfully! Redirecting...');
      setTimeout(() => {
        const dashPath = authData.role === 'LOGISTICS' ? '/logistics' : '/exporter';
        window.history.replaceState({}, '', dashPath);
        onNavigate(dashPath);
      }, 800);
    } catch (err) {
      const msg = err.message || 'Invalid email or password. Please try again.';
      triggerToast('error', msg);
      setShake(true);
      setTimeout(() => setShake(false), 500);
    } finally {
      setLoading(false);
    }
  };

  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotSubmitted, setForgotSubmitted] = useState(false);
  const [forgotLoading, setForgotLoading] = useState(false);

  const handleForgotSubmit = (e) => {
    e.preventDefault();
    if (!forgotEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(forgotEmail.trim())) {
      triggerToast('error', 'Please enter a valid email address.');
      return;
    }
    setForgotLoading(true);
    setTimeout(() => {
      setForgotLoading(false);
      setForgotSubmitted(true);
      triggerToast('success', 'Password reset link sent to your email.');
    }, 900);
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col relative font-sans overflow-hidden">

      {/* Subtle Background Glow */}
      <div className="absolute inset-0 pointer-events-none z-0">
        <div className="absolute top-[12%] left-[20%] w-[500px] h-[500px] bg-primary/5 rounded-full blur-[120px]"></div>
      </div>

      {/* Toast Notification */}
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

      {/* Top Navbar Actions */}
      <div className="absolute top-6 left-6 right-6 flex items-center justify-between z-30">
        <button
          onClick={() => onNavigate('/')}
          className="btn-outline text-xs px-3.5 py-1.5 cursor-pointer flex items-center gap-1.5"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Home</span>
        </button>

        <ThemeToggle variant="simple" />
      </div>

      {/* Main Split Content */}
      <div className="flex-grow grid grid-cols-1 lg:grid-cols-12 min-h-screen relative z-10 pt-16 lg:pt-0">

        {/* Left Side: Editorial Banner */}
        <div className="hidden lg:flex lg:col-span-6 bg-muted/30 border-r border-border flex-col items-center justify-center p-12 relative overflow-hidden">
          
          {/* Subtle concentric circles */}
          <div className="absolute w-[380px] h-[380px] border border-border/60 rounded-full pointer-events-none"></div>
          <div className="absolute w-[500px] h-[500px] border border-dashed border-border/40 rounded-full pointer-events-none animate-pulse-slow"></div>

          <div className="relative z-10 flex flex-col items-center max-w-md text-center space-y-4">
            
            <div className="w-14 h-14 rounded-2xl bg-primary/10 text-primary border border-primary/20 flex items-center justify-center shadow-xs">
              {role === 'logistics' ? <Truck className="w-7 h-7" /> : <Globe className="w-7 h-7" />}
            </div>

            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary border border-primary/20 text-[10px] font-semibold tracking-wider uppercase">
              {role === 'logistics' ? 'Logistics Fleet Portal' : 'Exporter Intelligence Portal'}
            </div>

            <h2 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-foreground leading-tight">
              {role === 'logistics'
                ? 'Multimodal Logistics & Freight Dispatch'
                : 'Compliance Intelligence Built for Indian SMEs'}
            </h2>

            <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed max-w-sm">
              {role === 'logistics'
                ? 'Bid on export shipments, issue proposals, coordinate customs documents, and manage door-to-door milestones in real-time.'
                : 'Instantly access verified import tariffs, HS classifications, and deterministic landed cost calculations.'}
            </p>

            <div className="pt-2 flex items-center gap-3 text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <Shield className="w-3.5 h-3.5 text-primary" />
                ICEGATE Verified
              </span>
              <span>•</span>
              <span className="flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-primary" />
                AI-Powered HS Classifier
              </span>
            </div>
          </div>
        </div>

        {/* Right Side: Login Form */}
        <div className="lg:col-span-6 flex flex-col items-center justify-center p-6 sm:p-12 relative">

          {/* Form Card wrapper */}
          <div className={`w-full max-w-md bg-card border border-border rounded-2xl p-7 sm:p-9 shadow-sm transition-transform ${
            shake ? 'animate-[shake_0.4s_ease-in-out]' : ''
          }`}>

            {/* Header info */}
            <div className="mb-6 space-y-1.5 text-left">
              <h1 className="font-display text-2xl font-bold text-foreground tracking-tight">Welcome Back</h1>
              <p className="text-xs text-muted-foreground leading-relaxed">
                {role === 'logistics'
                  ? 'Sign in to access your freight dispatch and proposal dashboard'
                  : 'Sign in to access your export intelligence and tariff dashboard'}
              </p>
            </div>

            {/* Role Selector Tabs */}
            <div className="grid grid-cols-2 gap-1.5 bg-muted p-1 rounded-xl border border-border mb-6 select-none">
              <button
                type="button"
                onClick={() => setRole('exporter')}
                className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                  role === 'exporter' ? 'bg-card text-primary shadow-xs border border-border' : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <Globe className="w-3.5 h-3.5" />
                <span>Exporter</span>
              </button>
              <button
                type="button"
                onClick={() => setRole('logistics')}
                className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                  role === 'logistics' ? 'bg-card text-primary shadow-xs border border-border' : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <Truck className="w-3.5 h-3.5" />
                <span>Logistics Partner</span>
              </button>
            </div>

            {/* Login form */}
            <form onSubmit={handleLoginSubmit} className="space-y-4">

              {/* Email Input Field */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-foreground block">
                  Email Address
                </label>
                <div className="relative flex items-center">
                  <Mail className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      if (errors.email) setErrors({ ...errors, email: null });
                    }}
                    placeholder="name@company.com"
                    autoComplete="email"
                    className={`input-claude h-10 pl-10 pr-3.5 text-sm rounded-lg ${errors.email ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                  />
                </div>
                {errors.email && (
                  <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.email}</p>
                )}
              </div>

              {/* Password Input Field */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-foreground block">
                  Password
                </label>
                <div className="relative flex items-center">
                  <Lock className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      if (errors.password) setErrors({ ...errors, password: null });
                    }}
                    placeholder="••••••••"
                    autoComplete="current-password"
                    className={`input-claude h-10 pl-10 pr-10 text-sm rounded-lg ${errors.password ? 'border-destructive focus:border-destructive focus:ring-destructive/20' : ''}`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 p-1 text-muted-foreground hover:text-foreground transition-colors cursor-pointer rounded-md focus:outline-none"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-[11px] text-destructive font-medium mt-1 animate-fade-in">{errors.password}</p>
                )}
              </div>

              {/* Remember Me & Forgot Password Row */}
              <div className="flex items-center justify-between pt-1">
                <label className="flex items-center gap-2 cursor-pointer select-none text-xs text-muted-foreground hover:text-foreground">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="w-4 h-4 rounded border-border text-primary accent-primary cursor-pointer"
                  />
                  <span>Remember me</span>
                </label>

                <button
                  type="button"
                  onClick={() => {
                    setForgotEmail(email);
                    setForgotSubmitted(false);
                    setShowForgotModal(true);
                  }}
                  className="text-xs font-semibold text-primary hover:underline cursor-pointer"
                >
                  Forgot Password?
                </button>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={loading}
                className="btn-primary w-full h-10 text-sm font-semibold rounded-lg cursor-pointer mt-2 shadow-xs flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Signing in...</span>
                  </>
                ) : (
                  <span>Sign In as {role === 'logistics' ? 'Logistics Partner' : 'Exporter'}</span>
                )}
              </button>

            </form>

            {/* Register Footer */}
            <div className="mt-6 text-center border-t border-border pt-4">
              <p className="text-xs text-muted-foreground">
                Don't have an account?{' '}
                <button
                  onClick={() => onNavigate('/register')}
                  className="font-semibold text-primary hover:underline cursor-pointer"
                >
                  Create an account
                </button>
              </p>
            </div>

          </div>
        </div>

      </div>

      {/* Forgot Password Modal */}
      {showForgotModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-xs p-4 animate-fade-in">
          <div className="card-claude rounded-2xl p-6 sm:p-7 max-w-md w-full shadow-2xl animate-scale-in relative">
            <button
              type="button"
              onClick={() => setShowForgotModal(false)}
              className="absolute top-4 right-4 p-1 rounded-md text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>

            <h3 className="text-base font-bold text-foreground">Reset Password</h3>
            <p className="text-xs text-muted-foreground mt-1 leading-relaxed">
              Enter your registered email address to receive password reset instructions.
            </p>

            {forgotSubmitted ? (
              <div className="mt-4 p-4 rounded-xl bg-accent text-foreground border border-primary/20 text-xs space-y-2">
                <div className="flex items-center gap-2 text-primary font-semibold">
                  <Check className="w-4 h-4" />
                  <span>Instructions Dispatched!</span>
                </div>
                <p className="text-muted-foreground">
                  We've sent a password reset token to <strong>{forgotEmail}</strong>. Please check your inbox.
                </p>
                <button
                  onClick={() => setShowForgotModal(false)}
                  className="btn-primary mt-3 w-full h-9 text-xs"
                >
                  Back to Sign In
                </button>
              </div>
            ) : (
              <form onSubmit={handleForgotSubmit} className="mt-4 space-y-3.5">
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-foreground block">
                    Registered Email
                  </label>
                  <div className="relative flex items-center">
                    <Mail className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
                    <input
                      type="email"
                      value={forgotEmail}
                      onChange={(e) => setForgotEmail(e.target.value)}
                      placeholder="account@company.com"
                      required
                      className="input-claude h-9 pl-10 pr-3.5 text-xs rounded-lg"
                    />
                  </div>
                </div>

                <div className="flex justify-end gap-2 pt-3 border-t border-border">
                  <button
                    type="button"
                    onClick={() => setShowForgotModal(false)}
                    className="btn-ghost text-xs py-1.5 px-3 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={forgotLoading}
                    className="btn-primary text-xs py-1.5 px-4 cursor-pointer flex items-center gap-1.5"
                  >
                    {forgotLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    <span>Send Reset Link</span>
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Shaking animation inline rules */}
      <style>{`
        @keyframes shake {
          0%, 100% { transform: translateX(0); }
          20%, 60% { transform: translateX(-6px); }
          40%, 80% { transform: translateX(6px); }
        }
      `}</style>
    </div>
  );
}