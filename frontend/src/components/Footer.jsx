import React from 'react';

export default function Footer({ onNavigate }) {
  const handleNavClick = (e, href) => {
    e.preventDefault();
    if (href === '#') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    const el = document.querySelector(href);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <footer className="bg-card border-t border-border pt-12 pb-8 relative overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Columns Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-8 pb-10 border-b border-border mb-8">
          
          {/* Column 1: Brand & Statement */}
          <div className="lg:col-span-3 flex flex-col gap-3">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 rounded-md bg-primary flex items-center justify-center text-primary-foreground font-bold text-xs">
                T
              </div>
              <span className="font-display text-lg font-bold tracking-tight text-foreground">
                Trade<span className="text-primary">Wise</span>
              </span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed max-w-sm">
              Global compliance intelligence, landed cost calculation, and market ranking decision platform built for Indian SME exporters.
            </p>
            <div className="mt-1">
              <span className="inline-flex items-center gap-1.5 text-[10px] font-semibold text-primary bg-primary/10 px-2.5 py-1 rounded-full uppercase tracking-wider">
                🇮🇳 Built for Indian SMEs
              </span>
            </div>
          </div>

          {/* Column 2: Quick Links */}
          <div className="flex flex-col gap-2.5">
            <h4 className="text-xs font-semibold text-foreground uppercase tracking-wider">Navigation</h4>
            <ul className="flex flex-col gap-2 text-xs text-muted-foreground">
              <li>
                <a href="#" onClick={(e) => handleNavClick(e, '#')} className="hover:text-foreground transition-colors">
                  Home
                </a>
              </li>
              <li>
                <a href="#problem" onClick={(e) => handleNavClick(e, '#problem')} className="hover:text-foreground transition-colors">
                  Exporter's Dilemma
                </a>
              </li>
              <li>
                <a href="#how-it-works" onClick={(e) => handleNavClick(e, '#how-it-works')} className="hover:text-foreground transition-colors">
                  How It Works
                </a>
              </li>
              <li>
                <a href="#features" onClick={(e) => handleNavClick(e, '#features')} className="hover:text-foreground transition-colors">
                  Features
                </a>
              </li>
            </ul>
          </div>

          {/* Column 3: Platform Portals */}
          <div className="flex flex-col gap-2.5">
            <h4 className="text-xs font-semibold text-foreground uppercase tracking-wider">Portals</h4>
            <ul className="flex flex-col gap-2 text-xs text-muted-foreground">
              <li>
                <button 
                  onClick={() => onNavigate && onNavigate('/exporter')} 
                  className="hover:text-foreground transition-colors cursor-pointer text-left"
                >
                  Exporter Dashboard
                </button>
              </li>
              <li>
                <button 
                  onClick={() => onNavigate && onNavigate('/logistics')} 
                  className="hover:text-foreground transition-colors cursor-pointer text-left"
                >
                  Logistics Portal
                </button>
              </li>
              <li>
                <button 
                  onClick={() => onNavigate && onNavigate('/login')} 
                  className="hover:text-foreground transition-colors cursor-pointer text-left"
                >
                  Sign In
                </button>
              </li>
              <li>
                <button 
                  onClick={() => onNavigate && onNavigate('/register')} 
                  className="hover:text-foreground transition-colors cursor-pointer text-left"
                >
                  Create Account
                </button>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-muted-foreground">
          <p>© {new Date().getFullYear()} TradeWise. All rights reserved.</p>
          <div className="flex items-center gap-6">
            <span className="text-[11px]">ITC-HS 2022 Verified</span>
            <span className="text-[11px]">TiDB Cloud Multi-Region</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
