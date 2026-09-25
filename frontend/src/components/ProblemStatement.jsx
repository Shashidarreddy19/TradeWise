import React from 'react';
import { HelpCircle, AlertCircle, TrendingDown, ShieldAlert } from 'lucide-react';

export default function ProblemStatement() {
  const painPoints = [
    {
      title: "Confused by complex regulations?",
      description: "Navigating hundreds of regulatory documents across multiple destination countries and jurisdictions creates friction and delays.",
      icon: HelpCircle,
    },
    {
      title: "Unsure which market is profitable?",
      description: "Without granular localized tariff analysis and demand indicators, picking target export destinations is often guesswork.",
      icon: TrendingDown,
    },
    {
      title: "Surprised by hidden costs?",
      description: "Unforeseen customs duties, local consumption taxes, and unbudgeted clearing fees erode exporter margins.",
      icon: AlertCircle,
    },
    {
      title: "Worried about compliance risks?",
      description: "A single missing certification or incorrect product label can lead to customs holds, penalties, or rejected shipments.",
      icon: ShieldAlert,
    }
  ];

  return (
    <section id="problem" className="py-20 bg-background relative overflow-hidden border-t border-border">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Header */}
        <div className="text-center max-w-2xl mx-auto mb-14">
          <h2 className="font-display text-3xl sm:text-4xl font-bold text-foreground tracking-tight">
            Global Trade is Filled with{' '}
            <span className="text-primary">Hidden Friction</span>
          </h2>
          <p className="text-muted-foreground mt-3 text-sm sm:text-base leading-relaxed">
            For SME exporters, expanding globally requires navigating an obstacle course of complex tariffs, shifting regulations, and opaque landed costs.
          </p>
        </div>

        {/* 2x2 Grid Layout for Pain Points */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-4xl mx-auto">
          {painPoints.map((point, index) => {
            const IconComp = point.icon;
            return (
              <div
                key={index}
                className="flex gap-4 p-5 rounded-xl card-claude group cursor-default"
              >
                <div className="w-10 h-10 rounded-lg flex items-center justify-center shrink-0 bg-accent text-primary border border-primary/20 transition-transform group-hover:scale-105 shadow-xs">
                  <IconComp className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-foreground group-hover:text-primary transition-colors">
                    {point.title}
                  </h3>
                  <p className="text-xs text-muted-foreground mt-1 leading-relaxed">
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
