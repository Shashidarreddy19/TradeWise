import React from 'react';
import { HelpCircle, AlertCircle, TrendingDown, ShieldAlert } from 'lucide-react';

export default function ProblemStatement() {
  const painPoints = [
    {
      title: "Confused by complex regulations?",
      description: "Navigating hundreds of regulatory documents across multiple countries in different languages is a compliance nightmare.",
      icon: HelpCircle,
      color: "from-blue-50 to-indigo-50 text-blue-600 border-blue-200"
    },
    {
      title: "Unsure which market is profitable?",
      description: "Without granular localized demand and pricing indicators, picking target export markets is mostly guesswork.",
      icon: TrendingDown,
      color: "from-purple-50 to-pink-50 text-purple-600 border-purple-200"
    },
    {
      title: "Surprised by hidden costs?",
      description: "Unforeseen customs duties, local taxes, high freight margins, and unexpected storage fees wipe out profit margins.",
      icon: AlertCircle,
      color: "from-emerald-50 to-teal-50 text-emerald-600 border-emerald-200"
    },
    {
      title: "Worried about compliance risks?",
      description: "A single missing certificate or incorrect packaging label can result in customs rejection and massive financial losses.",
      icon: ShieldAlert,
      color: "from-rose-50 to-red-50 text-rose-600 border-rose-200"
    }
  ];

  return (
    <section id="problem" className="py-24 bg-white relative overflow-hidden">
      {/* Background glow */}
      <div className="absolute top-1/2 left-0 w-96 h-96 bg-red-500/5 rounded-full glow-blur -translate-y-1/2"></div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Header (Tagline capsule removed) */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 mt-4 tracking-tight">
            Global Trade is Filled with{' '}
            <span className="text-rose-600">Hidden Friction</span>
          </h2>
          <p className="text-slate-600 mt-4">
            For SMEs, expanding globally is not just about producing great goods—it is about surviving an obstacle course of compliance, logistics, and opacity.
          </p>
        </div>

        {/* 2x2 Grid Layout for Pain Points (Left side illustration completely removed) */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-5xl mx-auto">
          {painPoints.map((point, index) => {
            const IconComp = point.icon;
            return (
              <div
                key={index}
                className="flex gap-4 p-6 rounded-2xl border border-slate-100 bg-slate-50/50 hover:bg-white hover:border-slate-200 hover:shadow-sm transition-all duration-300 group"
              >
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center border bg-gradient-to-tr shrink-0 ${point.color} group-hover:scale-110 transition-transform`}>
                  <IconComp className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900 group-hover:text-rose-600 transition-colors">
                    {point.title}
                  </h3>
                  <p className="text-sm text-slate-600 mt-1.5 leading-relaxed">
                    {point.description}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
