import React from 'react';
import { ArrowRight, ShieldCheck, Calculator, Globe, Sparkles } from 'lucide-react';

export default function Hero({ onNavigate, authenticated, getDashboardPath }) {
  return (
    <section className="relative pt-32 pb-20 md:pt-40 md:pb-32 bg-transparent overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Text Content */}
          <div className="lg:col-span-6 flex flex-col items-center lg:items-start text-center lg:text-left">
            
            {/* Pill Tag */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-accent text-accent-foreground border border-primary/20 text-xs font-semibold mb-6 shadow-xs">
              <Sparkles className="w-3.5 h-3.5 text-primary" />
              <span>Export Intelligence Platform</span>
            </div>

            {/* Headline */}
            <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-foreground leading-[1.12] mb-6">
              Find Your Best <br />
              <span className="text-primary">
                Export Market
              </span> <br />
              with Smart Precision
            </h1>

            {/* Subheadline */}
            <p className="text-base sm:text-lg text-muted-foreground font-normal leading-relaxed max-w-xl mb-8">
              Deterministic landed cost estimation, verified regulatory compliance intelligence, and context-aware country rankings—built specifically for Indian SME exporters.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-col sm:flex-row items-center gap-3 w-full sm:w-auto">
              <button
                onClick={() => onNavigate(authenticated ? getDashboardPath() : '/register')}
                className="btn-primary w-full sm:w-auto px-7 py-3.5 text-base font-semibold cursor-pointer"
              >
                {authenticated ? 'Go to Dashboard' : 'Get Started Free'}
                <ArrowRight className="w-4 h-4 ml-1" />
              </button>
              <button
                onClick={() => {
                  const el = document.querySelector('#how-it-works');
                  if (el) el.scrollIntoView({ behavior: 'smooth' });
                }}
                className="btn-outline w-full sm:w-auto px-6 py-3.5 text-base font-medium cursor-pointer"
              >
                Explore Platform
              </button>
            </div>

            {/* Qualitative Value Badges */}
            <div className="mt-10 flex flex-wrap gap-2.5 border-t border-border pt-6 w-full max-w-lg justify-center lg:justify-start">
              <div className="badge-neutral text-xs">
                <ShieldCheck className="w-3.5 h-3.5 text-primary" />
                <span>Grounded Compliance</span>
              </div>
              <div className="badge-neutral text-xs">
                <Calculator className="w-3.5 h-3.5 text-primary" />
                <span>Landed Cost Engine</span>
              </div>
              <div className="badge-neutral text-xs">
                <Globe className="w-3.5 h-3.5 text-primary" />
                <span>Context-Aware Rankings</span>
              </div>
            </div>
          </div>

          {/* Clean Visual Illustration / Interactive Card Container */}
          <div className="lg:col-span-6 relative flex items-center justify-center min-h-[420px]">
            {/* Ambient Background Glow */}
            <div className="absolute w-72 h-72 rounded-full bg-primary/10 blur-[80px] pointer-events-none z-0"></div>

            {/* Main Visual Container */}
            <div className="w-full aspect-square max-w-[440px] card-claude rounded-2xl p-6 flex flex-col justify-between relative overflow-hidden z-10 floating-3d">
              
              {/* Card Header preview */}
              <div className="flex items-center justify-between border-b border-border pb-4">
                <div className="flex items-center gap-2.5">
                  <div className="w-3 h-3 rounded-full bg-primary/60 shadow-xs"></div>
                  <div className="w-3 h-3 rounded-full bg-border"></div>
                  <div className="w-3 h-3 rounded-full bg-border"></div>
                  <span className="text-xs font-semibold text-muted-foreground ml-2">Export Intelligence Node</span>
                </div>
                <span className="badge-success text-[10px]">Active Engine</span>
              </div>

              {/* Central stylized metric visualization */}
              <div className="my-auto py-6 space-y-4">
                <div className="p-4 rounded-xl bg-muted/60 border border-border flex items-center justify-between shadow-3d-flat">
                  <div>
                    <p className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">HS Code Classification</p>
                    <p className="text-sm font-bold text-foreground mt-0.5">09103030 — Turmeric (Curcuma)</p>
                  </div>
                  <span className="badge-primary text-xs font-semibold">96% Confident</span>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3.5 rounded-xl bg-muted/60 border border-border shadow-3d-flat">
                    <p className="text-[10px] font-semibold text-muted-foreground uppercase">Top Market</p>
                    <p className="text-sm font-bold text-foreground mt-0.5">United States</p>
                    <p className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold mt-1">0% MFN Customs Duty</p>
                  </div>
                  <div className="p-3.5 rounded-xl bg-muted/60 border border-border shadow-3d-flat">
                    <p className="text-[10px] font-semibold text-muted-foreground uppercase">Compliance Index</p>
                    <p className="text-sm font-bold text-foreground mt-0.5">92 / 100</p>
                    <p className="text-[10px] text-primary font-semibold mt-1">FDA + Phytosanitary</p>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-accent/70 border border-primary/20 flex items-center justify-between shadow-3d-flat">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full bg-primary animate-pulse"></div>
                    <span className="text-xs font-medium text-foreground">Fast Grounded Retrieval</span>
                  </div>
                  <span className="text-xs font-mono font-semibold text-primary">&lt; 30ms</span>
                </div>
              </div>

              {/* Bottom status bar */}
              <div className="flex items-center justify-between pt-3 border-t border-border text-[11px] text-muted-foreground">
                <span>ITC-HS 2022 Verified</span>
                <span>TiDB Cloud Synced</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
