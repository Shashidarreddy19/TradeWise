import React from 'react';
import { ShieldCheck, DollarSign, Truck, Sparkles } from 'lucide-react';

export default function CoreFeatures() {
  const benefits = [
    {
      title: "Compliance Audit",
      description: "Instantly audit required import-export checklists, certificates, and customs regulations for 150+ target countries.",
      icon: ShieldCheck,
      color: "from-sky-50 to-sky-100/50 text-sky-600 border-sky-200"
    },
    {
      title: "Landed Cost Engine",
      description: "Estimate target margins, ocean freight rates, destination tariffs, and local port clearance fees in a single workflow.",
      icon: DollarSign,
      color: "from-indigo-50 to-indigo-100/50 text-indigo-600 border-indigo-200"
    },
    {
      title: "Logistics Coordination",
      description: "Broadcast ready orders directly to verified freight operators and cargo forwarders to secure shipment assignments.",
      icon: Truck,
      color: "from-emerald-50 to-emerald-100/50 text-emerald-600 border-emerald-200"
    }
  ];

  return (
    <section id="features" className="py-24 bg-slate-50 relative overflow-hidden border-t border-slate-200/80">
      {/* Decorative ambient background glows */}
      <div className="absolute top-10 left-10 w-80 h-80 bg-sky-500/5 rounded-full glow-blur animate-float"></div>
      <div className="absolute bottom-10 right-10 w-96 h-96 bg-indigo-500/5 rounded-full glow-blur"></div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
            Designed to Simplify Indian Export
          </h2>
          <p className="text-slate-600 mt-4 leading-relaxed">
            Eliminate traditional trade bottlenecks with tools built explicitly to manage your compliance checks, estimate real margins, and coordinate freight.
          </p>
        </div>

        {/* Benefits Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {benefits.map((benefit, index) => {
            const Icon = benefit.icon;
            return (
              <div 
                key={index}
                className="bg-white border border-slate-200 rounded-3xl p-8 hover:shadow-md transition-all duration-300 flex flex-col items-center text-center group"
              >
                {/* Icon Wrapper */}
                <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${benefit.color} border flex items-center justify-center mb-6 group-hover:scale-105 transition-transform duration-300`}>
                  <Icon className="w-8 h-8" />
                </div>

                {/* Title & Description */}
                <h3 className="text-lg font-bold text-slate-900 mb-3">{benefit.title}</h3>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
                  {benefit.description}
                </p>
              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
}
