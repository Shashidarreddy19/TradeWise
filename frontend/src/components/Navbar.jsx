import React, { useState, useEffect, useRef } from 'react';
import { Menu, X, ArrowRight, Bell, ChevronDown, LogOut, User, Settings, LayoutDashboard } from 'lucide-react';

export default function Navbar({ onNavigate, currentPath, authenticated, user, onLogout, getDashboardPath }) {
  const [isOpen, setIsOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClick = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // Guest nav items (landing page sections)
  const guestNavItems = [
    { name: 'Home', href: '#' },
    { name: "Exporter's Dilemma", href: '#problem' },
    { name: 'How It Works', href: '#how-it-works' },
    { name: 'Features', href: '#features' },
  ];

  const handleNavClick = (e, href) => {
    e.preventDefault();
    if (href === '#') {
      if (currentPath !== '/') {
        onNavigate('/');
      } else {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
      return;
    }
    if (currentPath !== '/') {
      onNavigate('/');
      setTimeout(() => {
        const el = document.querySelector(href);
        if (el) el.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    } else {
      const el = document.querySelector(href);
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // Logo click: dashboard if logged in, landing if guest
  const handleLogoClick = () => {
    if (authenticated) {
      onNavigate(getDashboardPath());
    } else {
      onNavigate('/');
    }
  };

  // User initials for avatar
  const initials = user?.name
    ? user.name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)
    : 'U';

  return (
    <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${scrolled
      ? 'py-4 glass-panel border-b border-white/10 shadow-lg'
      : 'py-6 bg-transparent border-b border-transparent'
      }`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">

          {/* Logo */}
          <div
            onClick={handleLogoClick}
            className="flex items-center gap-2 cursor-pointer select-none"
          >
            <span className="font-display text-xl font-bold tracking-tight bg-gradient-to-r from-slate-900 via-slate-850 to-slate-600 bg-clip-text text-transparent">
              Trade
            </span>
          </div>

          {/* ═══════════ DESKTOP NAV ═══════════ */}
          {!authenticated ? (
            <>
              {/* Guest: section links */}
              <div className="hidden lg:flex items-center gap-8">
                {guestNavItems.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    onClick={(e) => handleNavClick(e, item.href)}
                    className="text-sm font-semibold text-slate-600 hover:text-sky-600 transition-colors"
                  >
                    {item.name}
                  </a>
                ))}
              </div>

              {/* Guest: Sign In + Get Started */}
              <div className="hidden lg:flex items-center gap-4">
                <button
                  onClick={() => onNavigate('/login')}
                  className="text-sm font-bold text-slate-600 hover:text-sky-600 transition-colors cursor-pointer"
                >
                  Sign In
                </button>
                <button
                  onClick={() => onNavigate('/register')}
                  className="inline-flex items-center justify-center gap-1 px-4 py-2 text-sm font-semibold rounded-lg text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 hover:scale-105 active:scale-95 transition-all shadow-md shadow-sky-500/10 cursor-pointer"
                >
                  Get Started
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </>
          ) : (
            <>
              {/* Authenticated: dashboard-oriented nav */}
              <div className="hidden lg:flex items-center gap-8">
                <button onClick={() => onNavigate(getDashboardPath())} className="text-sm font-semibold text-slate-600 hover:text-sky-600 transition-colors cursor-pointer">Dashboard</button>
              </div>

              {/* Authenticated: notifications + profile */}
              <div className="hidden lg:flex items-center gap-4">
                {/* Notifications bell */}
                <button
                  onClick={() => onNavigate(getDashboardPath())}
                  className="relative p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100/50 transition-colors cursor-pointer"
                >
                  <Bell className="w-5 h-5" />
                </button>

                {/* Profile dropdown */}
                <div className="relative" ref={dropdownRef}>
                  <button
                    onClick={() => setProfileOpen(!profileOpen)}
                    className="flex items-center gap-2 px-2 py-1.5 rounded-xl hover:bg-slate-100/50 transition-colors cursor-pointer"
                  >
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold shadow-sm">
                      {initials}
                    </div>
                    <span className="text-sm font-semibold text-slate-700 max-w-[120px] truncate hidden xl:block">
                      {user?.name || 'User'}
                    </span>
                    <ChevronDown className={`w-3.5 h-3.5 text-slate-400 transition-transform ${profileOpen ? 'rotate-180' : ''}`} />
                  </button>

                  {/* Dropdown menu */}
                  {profileOpen && (
                    <div className="absolute right-0 top-full mt-2 w-56 bg-white border border-slate-200/80 rounded-xl shadow-2xl shadow-slate-200/50 py-2 animate-in fade-in slide-in-from-top-2 duration-200 z-50">
                      <div className="px-4 py-3 border-b border-slate-100">
                        <p className="text-sm font-bold text-slate-800 truncate">{user?.name}</p>
                        <p className="text-[11px] text-slate-500 truncate">{user?.email}</p>
                        <span className="inline-block mt-1 text-[9px] font-bold text-sky-600 bg-sky-50 px-2 py-0.5 rounded-full uppercase tracking-wider">
                          {user?.role}
                        </span>
                      </div>
                      <div className="py-1">
                        <button
                          onClick={() => { setProfileOpen(false); onNavigate(getDashboardPath()); }}
                          className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors cursor-pointer"
                        >
                          <LayoutDashboard className="w-4 h-4" />
                          Dashboard
                        </button>
                        <button
                          onClick={() => { setProfileOpen(false); onNavigate(getDashboardPath()); }}
                          className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors cursor-pointer"
                        >
                          <User className="w-4 h-4" />
                          My Profile
                        </button>
                        <button
                          onClick={() => { setProfileOpen(false); onNavigate(getDashboardPath()); }}
                          className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors cursor-pointer"
                        >
                          <Settings className="w-4 h-4" />
                          Settings
                        </button>
                      </div>
                      <div className="border-t border-slate-100 pt-1">
                        <button
                          onClick={() => { setProfileOpen(false); onLogout(); }}
                          className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 transition-colors cursor-pointer"
                        >
                          <LogOut className="w-4 h-4" />
                          Logout
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </>
          )}

          {/* Mobile Menu Button */}
          <div className="lg:hidden">
            <button
              onClick={() => setIsOpen(!isOpen)}
              className="p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100/50 transition-colors"
            >
              {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* ═══════════ MOBILE DRAWER ═══════════ */}
      {isOpen && (
        <div className="lg:hidden absolute top-full left-0 right-0 glass-panel border-b border-white/10 shadow-2xl py-6 px-4 animate-in fade-in slide-in-from-top-5 duration-200">
          <div className="flex flex-col gap-4">
            {!authenticated ? (
              <>
                {guestNavItems.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    onClick={(e) => { setIsOpen(false); handleNavClick(e, item.href); }}
                    className="text-sm font-semibold text-slate-600 hover:text-sky-600 transition-colors"
                  >
                    {item.name}
                  </a>
                ))}
                <div className="border-t border-slate-100/80 pt-4 flex flex-col gap-3">
                  <button
                    onClick={() => { setIsOpen(false); onNavigate('/login'); }}
                    className="text-sm font-bold text-slate-600 hover:text-sky-600 transition-colors w-full py-2.5 border border-slate-200 rounded-xl cursor-pointer"
                  >
                    Sign In
                  </button>
                  <button
                    onClick={() => { setIsOpen(false); onNavigate('/register'); }}
                    className="inline-flex items-center justify-center gap-2 w-full py-3 font-semibold rounded-xl text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 transition-all text-center cursor-pointer"
                  >
                    Get Started
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </>
            ) : (
              <>
                <button onClick={() => { setIsOpen(false); onNavigate(getDashboardPath()); }} className="text-sm font-semibold text-slate-600 hover:text-sky-600 transition-colors text-left">Dashboard</button>
                <div className="border-t border-slate-100/80 pt-4 flex flex-col gap-3">
                  <div className="flex items-center gap-3 px-2 py-2">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-sky-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold">{initials}</div>
                    <div>
                      <p className="text-sm font-bold text-slate-800">{user?.name}</p>
                      <p className="text-[10px] text-slate-500">{user?.role}</p>
                    </div>
                  </div>
                  <button
                    onClick={() => { setIsOpen(false); onLogout(); }}
                    className="text-sm font-bold text-red-600 hover:text-red-500 transition-colors w-full py-2.5 border border-red-200 rounded-xl cursor-pointer"
                  >
                    Logout
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
