import React, { useState, useEffect, useRef } from 'react';
import { Menu, X, ArrowRight, Bell, ChevronDown, LogOut, User, Settings, LayoutDashboard } from 'lucide-react';
import ThemeToggle from './ThemeToggle';

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
    <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
      scrolled
        ? 'py-3.5 bg-card/90 backdrop-blur-md border-b border-border shadow-xs'
        : 'py-5 bg-transparent border-b border-transparent'
    }`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">

          {/* Logo */}
          <div
            onClick={handleLogoClick}
            className="flex items-center gap-2.5 cursor-pointer select-none group"
          >
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-primary-foreground font-bold text-sm shadow-xs transition-transform group-hover:scale-105">
              T
            </div>
            <span className="font-display text-xl font-bold tracking-tight text-foreground">
              Trade<span className="text-primary">Wise</span>
            </span>
          </div>

          {/* ═══════════ DESKTOP NAV ═══════════ */}
          {!authenticated ? (
            <>
              {/* Guest: section links */}
              <div className="hidden lg:flex items-center gap-7">
                {guestNavItems.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    onClick={(e) => handleNavClick(e, item.href)}
                    className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                  >
                    {item.name}
                  </a>
                ))}
              </div>

              {/* Guest: Theme Toggle + Sign In + Get Started */}
              <div className="hidden lg:flex items-center gap-3">
                <ThemeToggle variant="simple" />

                <button
                  onClick={() => onNavigate('/login')}
                  className="text-sm font-medium text-muted-foreground hover:text-foreground px-3 py-2 rounded-lg transition-colors cursor-pointer"
                >
                  Sign In
                </button>
                <button
                  onClick={() => onNavigate('/register')}
                  className="btn-primary inline-flex items-center gap-1.5 cursor-pointer"
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
                <button onClick={() => onNavigate(getDashboardPath())} className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors cursor-pointer">Dashboard</button>
              </div>

              {/* Authenticated: theme toggle + notifications + profile */}
              <div className="hidden lg:flex items-center gap-3">
                <ThemeToggle variant="simple" />

                {/* Notifications bell */}
                <button
                  onClick={() => onNavigate(getDashboardPath())}
                  className="relative p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors cursor-pointer border border-border/60"
                  title="Notifications"
                >
                  <Bell className="w-4 h-4" />
                </button>

                {/* Profile dropdown */}
                <div className="relative" ref={dropdownRef}>
                  <button
                    onClick={() => setProfileOpen(!profileOpen)}
                    className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg border border-border bg-card hover:bg-muted transition-colors cursor-pointer"
                  >
                    <div className="w-7 h-7 rounded-full bg-primary/15 text-primary flex items-center justify-center text-xs font-bold">
                      {initials}
                    </div>
                    <span className="text-xs font-semibold text-foreground max-w-[120px] truncate hidden xl:block">
                      {user?.name || 'User'}
                    </span>
                    <ChevronDown className={`w-3.5 h-3.5 text-muted-foreground transition-transform ${profileOpen ? 'rotate-180' : ''}`} />
                  </button>

                  {/* Dropdown menu */}
                  {profileOpen && (
                    <div className="absolute right-0 top-full mt-2 w-56 bg-popover border border-border rounded-lg shadow-lg py-2 animate-scale-in z-50">
                      <div className="px-4 py-2.5 border-b border-border">
                        <p className="text-sm font-semibold text-foreground truncate">{user?.name}</p>
                        <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
                        <span className="inline-block mt-1 text-[10px] font-bold text-primary bg-primary/10 px-2 py-0.5 rounded-full uppercase tracking-wider">
                          {user?.role}
                        </span>
                      </div>
                      <div className="py-1">
                        <button
                          onClick={() => { setProfileOpen(false); onNavigate(getDashboardPath()); }}
                          className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-foreground hover:bg-muted transition-colors cursor-pointer"
                        >
                          <LayoutDashboard className="w-3.5 h-3.5 text-muted-foreground" />
                          Dashboard
                        </button>
                        <button
                          onClick={() => { setProfileOpen(false); onNavigate(getDashboardPath()); }}
                          className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-foreground hover:bg-muted transition-colors cursor-pointer"
                        >
                          <User className="w-3.5 h-3.5 text-muted-foreground" />
                          My Profile
                        </button>
                        <button
                          onClick={() => { setProfileOpen(false); onNavigate(getDashboardPath()); }}
                          className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-foreground hover:bg-muted transition-colors cursor-pointer"
                        >
                          <Settings className="w-3.5 h-3.5 text-muted-foreground" />
                          Settings
                        </button>
                      </div>
                      <div className="border-t border-border pt-1">
                        <button
                          onClick={() => { setProfileOpen(false); onLogout(); }}
                          className="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-destructive hover:bg-destructive/10 transition-colors cursor-pointer"
                        >
                          <LogOut className="w-3.5 h-3.5" />
                          Logout
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </>
          )}

          {/* Mobile Menu Button + Mobile Theme Toggle */}
          <div className="lg:hidden flex items-center gap-2">
            <ThemeToggle variant="simple" />
            <button
              onClick={() => setIsOpen(!isOpen)}
              className="p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors border border-border"
              aria-label="Toggle menu"
            >
              {isOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* ═══════════ MOBILE DRAWER ═══════════ */}
      {isOpen && (
        <div className="lg:hidden absolute top-full left-0 right-0 bg-card border-b border-border shadow-xl py-5 px-4 animate-scale-in">
          <div className="flex flex-col gap-3">
            {!authenticated ? (
              <>
                {guestNavItems.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    onClick={(e) => { setIsOpen(false); handleNavClick(e, item.href); }}
                    className="text-sm font-medium text-muted-foreground hover:text-foreground py-1.5 transition-colors"
                  >
                    {item.name}
                  </a>
                ))}
                <div className="border-t border-border pt-4 flex flex-col gap-2.5">
                  <button
                    onClick={() => { setIsOpen(false); onNavigate('/login'); }}
                    className="btn-secondary w-full text-center cursor-pointer"
                  >
                    Sign In
                  </button>
                  <button
                    onClick={() => { setIsOpen(false); onNavigate('/register'); }}
                    className="btn-primary w-full text-center cursor-pointer"
                  >
                    Get Started
                    <ArrowRight className="w-4 h-4 ml-1" />
                  </button>
                </div>
              </>
            ) : (
              <>
                <button onClick={() => { setIsOpen(false); onNavigate(getDashboardPath()); }} className="text-sm font-medium text-foreground hover:text-primary py-1.5 transition-colors text-left">Dashboard</button>
                <div className="border-t border-border pt-3 flex flex-col gap-2.5">
                  <div className="flex items-center gap-2.5 px-2 py-1.5">
                    <div className="w-8 h-8 rounded-full bg-primary/15 text-primary flex items-center justify-center text-xs font-bold">{initials}</div>
                    <div>
                      <p className="text-xs font-semibold text-foreground">{user?.name}</p>
                      <p className="text-[10px] text-muted-foreground uppercase">{user?.role}</p>
                    </div>
                  </div>
                  <button
                    onClick={() => { setIsOpen(false); onLogout(); }}
                    className="w-full py-2 text-xs font-semibold text-destructive border border-destructive/20 rounded-lg hover:bg-destructive/10 transition-colors cursor-pointer text-center"
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
