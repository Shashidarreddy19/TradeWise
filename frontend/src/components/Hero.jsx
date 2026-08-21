import React from 'react';
import { ArrowRight } from 'lucide-react';

export default function Hero({ onNavigate, authenticated, getDashboardPath }) {
  return (
    <section className="relative pt-32 pb-24 md:pt-40 md:pb-36 bg-transparent overflow-hidden">
      {/* Sweeping Light Rays */}
      <div className="absolute inset-0 pointer-events-none z-0 overflow-hidden">
        <div className="absolute top-[-20%] left-[-50%] w-[200%] h-[140%] bg-gradient-to-tr from-transparent via-sky-500/[0.04] to-transparent skew-y-12 translate-x-[-100%] animate-[lightSweep_12s_ease-in-out_infinite]"></div>
        <div className="absolute top-[-20%] left-[-50%] w-[200%] h-[140%] bg-gradient-to-tr from-transparent via-indigo-500/[0.03] to-transparent skew-y-12 translate-x-[-100%] animate-[lightSweep_16s_ease-in-out_infinite]" style={{ animationDelay: '3s' }}></div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 lg:gap-8 items-center">
          {/* Text Content */}
          <div className="lg:col-span-6 flex flex-col items-center lg:items-start text-center lg:text-left">

            {/* Headline */}
            <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-905 leading-[1.1] mb-6">
              Find Your Best <br />
              <span className="bg-gradient-to-r from-sky-600 via-indigo-600 to-indigo-700 bg-clip-text text-transparent">
                Export Market
              </span> <br />
              with Smart Precision
            </h1>

            {/* Subheadline */}
            <p className="text-lg text-slate-600 font-normal leading-relaxed max-w-xl mb-10">
              Compliance intelligence, landed cost estimation, and context-aware country rankings—all in one professional platform built for Indian SMEs.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-col sm:flex-row items-center gap-4 w-full sm:w-auto">
              <button
                onClick={() => onNavigate(authenticated ? getDashboardPath() : '/register')}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-8 py-4 font-semibold text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 hover:scale-105 active:scale-98 shadow-lg shadow-sky-500/15 rounded-xl transition-all cursor-pointer"
              >
                {authenticated ? 'Go to Dashboard' : 'Get Started'}
                <ArrowRight className="w-5 h-5" />
              </button>
            </div>

            {/* Qualitative Value Badges (No numbers, no country names) */}
            <div className="mt-12 flex flex-wrap gap-x-4 gap-y-3 border-t border-slate-200 pt-8 w-full max-w-lg justify-center lg:justify-start">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-650 bg-slate-100/50 border border-slate-200/80 px-3.5 py-1.5 rounded-full shadow-xxs">
                <span className="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
                Grounded Compliance
              </div>
              <div className="flex items-center gap-2 text-xs font-bold text-slate-650 bg-slate-100/50 border border-slate-200/80 px-3.5 py-1.5 rounded-full shadow-xxs">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                Landed Cost Engine
              </div>
              <div className="flex items-center gap-2 text-xs font-bold text-slate-650 bg-slate-100/50 border border-slate-200/80 px-3.5 py-1.5 rounded-full shadow-xxs">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                Context-Aware Rankings
              </div>
            </div>
          </div>

          {/* Interactive Compliance Globe & Hand-held Magnifying Glass Visual */}
          <div className="lg:col-span-6 relative flex items-center justify-center min-h-[480px]">
            {/* Ambient Background Glow */}
            <div className="absolute w-80 h-80 rounded-full bg-sky-500/5 blur-[80px] pointer-events-none z-0"></div>

            {/* Main Visual Container */}
            <div className="w-full aspect-square max-w-[480px] bg-white/60 border border-slate-200 rounded-full p-4 flex items-center justify-center shadow-sm relative overflow-hidden z-10">
              
              {/* Spinning background rings */}
              <div className="absolute inset-4 border border-dashed border-slate-200 rounded-full animate-spin-slow"></div>
              <div className="absolute inset-16 border border-slate-200/50 rounded-full animate-pulse-slow"></div>

              {/* Central Vector Composition SVG */}
              <svg viewBox="0 0 400 400" className="w-full h-full relative z-10">
                {/* Definitions */}
                <defs>
                  {/* Lens Clip Path */}
                  <clipPath id="lensClip">
                    <circle cx="240" cy="160" r="48" />
                  </clipPath>

                  {/* Magnifying Glass Metallic Gradient */}
                  <linearGradient id="metalGradient" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0%" stopColor="#f8fafc" />
                    <stop offset="30%" stopColor="#cbd5e1" />
                    <stop offset="70%" stopColor="#94a3b8" />
                    <stop offset="100%" stopColor="#475569" />
                  </linearGradient>

                  {/* Handle wood/grip gradient */}
                  <linearGradient id="handleGradient" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0%" stopColor="#475569" />
                    <stop offset="100%" stopColor="#0f172a" />
                  </linearGradient>

                  {/* Globe Grid Radial Gradient */}
                  <radialGradient id="globeBg" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stopColor="#ffffff" />
                    <stop offset="100%" stopColor="#f1f5f9" />
                  </radialGradient>

                  {/* Lens Scan Sweep Glow */}
                  <linearGradient id="scanGlow" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#38bdf8" stopOpacity="0" />
                    <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.4" />
                    <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
                  </linearGradient>
                </defs>

                {/* ==================================================== */}
                {/* LAYER 1: BASE STYLIZED GLOBE (Normal outer view) */}
                {/* ==================================================== */}
                <g>
                  {/* Globe base */}
                  <circle cx="200" cy="200" r="110" fill="url(#globeBg)" stroke="#cbd5e1" strokeWidth="1.5" />
                  
                  {/* Slow majestic rotating continents and grid curves */}
                  <g className="animate-[spin_40s_linear_infinite] origin-center" style={{ transformOrigin: '200px 200px' }}>
                    {/* Latitudes */}
                    <path d="M 140 120 Q 200 130 260 120" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 110 150 Q 200 165 290 150" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 95 180 Q 200 198 305 180" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 90 200 L 310 200" fill="none" stroke="#e2e8f0" strokeWidth="1.2" />
                    <path d="M 95 220 Q 200 202 305 220" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 110 250 Q 200 235 290 250" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 140 280 Q 200 270 260 280" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    
                    {/* Longitudes */}
                    <path d="M 200 90 L 200 310" fill="none" stroke="#e2e8f0" strokeWidth="1.2" />
                    <path d="M 200 90 Q 150 200 200 310" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 200 90 Q 110 200 200 310" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 200 90 Q 250 200 200 310" fill="none" stroke="#e2e8f0" strokeWidth="1" />
                    <path d="M 200 90 Q 290 200 200 310" fill="none" stroke="#e2e8f0" strokeWidth="1" />

                    {/* Stylized minimal grey continents */}
                    <path d="M 120 130 Q 140 120 160 140 T 180 170 T 150 190 T 120 170 Z" fill="#e2e8f0" className="opacity-70" />
                    <path d="M 220 220 Q 250 210 280 230 T 290 270 T 250 280 T 220 250 Z" fill="#e2e8f0" className="opacity-70" />
                    <path d="M 240 120 Q 270 100 290 130 T 270 170 T 230 150 Z" fill="#e2e8f0" className="opacity-70" />
                    <path d="M 110 230 Q 130 220 150 250 T 120 280 Z" fill="#e2e8f0" className="opacity-70" />
                  </g>
                </g>

                {/* ==================================================== */}
                {/* LAYER 2: REVEALED STRUCTURED LENS REGION (Clipped) */}
                {/* ==================================================== */}
                <g clipPath="url(#lensClip)">
                  {/* Deeper grid scanned background */}
                  <circle cx="200" cy="200" r="110" fill="#eff6ff" />

                  {/* Scanned rotating continents and wireframes matching speed */}
                  <g className="animate-[spin_40s_linear_infinite] origin-center" style={{ transformOrigin: '200px 200px' }}>
                    {/* Blue Latitudes */}
                    <path d="M 140 120 Q 200 130 260 120" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 110 150 Q 200 165 290 150" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 95 180 Q 200 198 305 180" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 90 200 L 310 200" fill="none" stroke="#3b82f6" strokeWidth="1.2" />
                    <path d="M 95 220 Q 200 202 305 220" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 110 250 Q 200 235 290 250" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 140 280 Q 200 270 260 280" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    
                    {/* Blue Longitudes */}
                    <path d="M 200 90 L 200 310" fill="none" stroke="#3b82f6" strokeWidth="1.2" />
                    <path d="M 200 90 Q 150 200 200 310" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 200 90 Q 110 200 200 310" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 200 90 Q 250 200 200 310" fill="none" stroke="#60a5fa" strokeWidth="1" />
                    <path d="M 200 90 Q 290 200 200 310" fill="none" stroke="#60a5fa" strokeWidth="1" />

                    {/* Continents converted into dense vector meshes */}
                    <path d="M 120 130 Q 140 120 160 140 T 180 170 T 150 190 T 120 170 Z" fill="#dbeafe" stroke="#3b82f6" strokeWidth="1" strokeDasharray="2,2" />
                    <path d="M 220 220 Q 250 210 280 230 T 290 270 T 250 280 T 220 250 Z" fill="#dbeafe" stroke="#3b82f6" strokeWidth="1" strokeDasharray="2,2" />
                    <path d="M 240 120 Q 270 100 290 130 T 270 170 T 230 150 Z" fill="#dbeafe" stroke="#3b82f6" strokeWidth="1" strokeDasharray="2,2" />
                    <path d="M 110 230 Q 130 220 150 250 T 120 280 Z" fill="#dbeafe" stroke="#3b82f6" strokeWidth="1" strokeDasharray="2,2" />
                  </g>

                  {/* Dense technical scanning grids */}
                  <g opacity="0.3">
                    <line x1="180" y1="130" x2="300" y2="130" stroke="#2563eb" strokeWidth="0.8" />
                    <line x1="180" y1="150" x2="300" y2="150" stroke="#2563eb" strokeWidth="0.8" />
                    <line x1="180" y1="170" x2="300" y2="170" stroke="#2563eb" strokeWidth="0.8" />
                    <line x1="180" y1="190" x2="300" y2="190" stroke="#2563eb" strokeWidth="0.8" />
                    <line x1="210" y1="100" x2="210" y2="220" stroke="#2563eb" strokeWidth="0.8" />
                    <line x1="230" y1="100" x2="230" y2="220" stroke="#2563eb" strokeWidth="0.8" />
                    <line x1="250" y1="100" x2="250" y2="220" stroke="#2563eb" strokeWidth="0.8" />
                    <line x1="270" y1="100" x2="270" y2="220" stroke="#2563eb" strokeWidth="0.8" />
                  </g>

                  {/* Green Compliance Verification Checks */}
                  <g>
                    {/* Check 1 */}
                    <circle cx="225" cy="140" r="7" fill="#10b981" />
                    <path d="M222 140 l2 2 l4 -4" fill="none" stroke="#ffffff" strokeWidth="1.5" strokeLinecap="round" />
                    
                    {/* Check 2 */}
                    <circle cx="262" cy="170" r="7" fill="#10b981" />
                    <path d="M259 170 l2 2 l4 -4" fill="none" stroke="#ffffff" strokeWidth="1.5" strokeLinecap="round" />

                    {/* Check 3 */}
                    <circle cx="218" cy="182" r="7" fill="#10b981" />
                    <path d="M215 182 l2 2 l4 -4" fill="none" stroke="#ffffff" strokeWidth="1.5" strokeLinecap="round" />
                  </g>

                  {/* Sweeping scan light effect */}
                  <rect x="180" y="100" width="120" height="120" fill="url(#scanGlow)" className="animate-[scanSweep_3s_ease-in-out_infinite]" />
                </g>

                {/* ==================================================== */}
                {/* LAYER 3: THE MAGNIFYING GLASS & HUMAN HAND */}
                {/* ==================================================== */}
                {/* Hovering group */}
                <g className="animate-[floatGlass_6s_ease-in-out_infinite] origin-center" style={{ transformOrigin: '240px 160px' }}>
                  {/* Glass handle */}
                  <line x1="206" y1="194" x2="135" y2="265" stroke="url(#handleGradient)" strokeWidth="9.5" strokeLinecap="round" className="shadow" />
                  
                  {/* Lens Metallic Ring Frame */}
                  <circle cx="240" cy="160" r="48" stroke="url(#metalGradient)" strokeWidth="4.5" fill="none" />
                  
                  {/* Glass glare highlight */}
                  <path d="M 202 138 A 48 48 0 0 1 270 128" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round" opacity="0.85" />

                  {/* Sleeve */}
                  <path d="M 0 380 Q 55 330 105 292" fill="none" stroke="#475569" strokeWidth="20" strokeLinecap="round" />
                  <line x1="95" y1="302" x2="110" y2="287" stroke="#cbd5e1" strokeWidth="22" strokeLinecap="round" />

                  {/* Hand base */}
                  <circle cx="128" cy="270" r="13" fill="#f8fafc" stroke="#cbd5e1" strokeWidth="1.5" />
                  
                  {/* Fingers wrapping handle */}
                  <rect x="135" y="244" width="14" height="6.5" rx="3" transform="rotate(-45 135 244)" fill="#f1f5f9" stroke="#cbd5e1" strokeWidth="1" />
                  <rect x="143" y="252" width="14" height="6.5" rx="3" transform="rotate(-45 143 252)" fill="#f1f5f9" stroke="#cbd5e1" strokeWidth="1" />
                  <rect x="151" y="260" width="14" height="6.5" rx="3" transform="rotate(-45 151 260)" fill="#f1f5f9" stroke="#cbd5e1" strokeWidth="1" />
                  
                  {/* Thumb overlay */}
                  <path d="M 118 276 Q 126 262 121 257" fill="none" stroke="#f8fafc" strokeWidth="6" strokeLinecap="round" />
                </g>
              </svg>
            </div>
          </div>
        </div>
      </div>
      
      {/* Light Rays and Scanner Animations */}
      <style>{`
        @keyframes lightSweep {
          0% {
            transform: translate3d(-50%, 0, 0) skewY(-12deg);
          }
          50% {
            transform: translate3d(50%, 0, 0) skewY(-12deg);
          }
          100% {
            transform: translate3d(-50%, 0, 0) skewY(-12deg);
          }
        }
        @keyframes floatGlass {
          0% { transform: translate3d(0, 0, 0) rotate(0deg); }
          50% { transform: translate3d(-4px, -12px, 0) rotate(1.5deg); }
          100% { transform: translate3d(0, 0, 0) rotate(0deg); }
        }
        @keyframes scanSweep {
          0% { transform: translate3d(0, -60px, 0); }
          50% { transform: translate3d(0, 60px, 0); }
          100% { transform: translate3d(0, -60px, 0); }
        }
      `}</style>
    </section>
  );
}
