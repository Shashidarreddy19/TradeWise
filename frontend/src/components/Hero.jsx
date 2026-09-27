import React from 'react';
import { ArrowRight, ShieldCheck, Calculator, Globe, Sparkles } from 'lucide-react';
import globeIllustration from '../assets/globe-export-illustration.png';

export default function Hero({ onNavigate, authenticated, getDashboardPath }) {
  return (
    <section className="relative pt-28 pb-16 sm:pt-32 sm:pb-20 md:pt-40 md:pb-32 bg-transparent overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 sm:gap-12 lg:gap-8 items-center">
          {/* Text Content */}
          <div className="lg:col-span-6 flex flex-col items-center lg:items-start text-center lg:text-left order-2 lg:order-1">

            {/* Pill Tag */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-accent text-accent-foreground border border-primary/20 text-xs font-semibold mb-5 sm:mb-6 shadow-xs">
              <Sparkles className="w-3.5 h-3.5 text-primary" />
              <span>Export Intelligence Platform</span>
            </div>

            {/* Headline */}
            <h1 className="font-display text-3xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-foreground leading-[1.15] sm:leading-[1.12] mb-5 sm:mb-6">
              Find Your Best <br />
              <span className="text-primary">
                Export Market
              </span> <br />
              with Smart Precision
            </h1>

            {/* Subheadline */}
            <p className="text-sm sm:text-base lg:text-lg text-muted-foreground font-normal leading-relaxed max-w-xl mb-7 sm:mb-8">
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
            <div className="mt-8 sm:mt-10 flex flex-wrap gap-2.5 border-t border-border pt-5 sm:pt-6 w-full max-w-lg justify-center lg:justify-start">
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

          {/* Globe Illustration */}
          <div className="lg:col-span-6 relative flex items-center justify-center order-1 lg:order-2">
            <img
              src={globeIllustration}
              alt="Globe illustration showing export shipping and flight routes converging on India"
              className="w-56 sm:w-72 md:w-96 lg:w-full max-w-[480px] h-auto select-none pointer-events-none"
              draggable="false"
            />
          </div>
        </div>
      </div>
    </section>
  );
}
