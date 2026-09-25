import React from 'react';
import { ShieldCheck, DollarSign, Truck } from 'lucide-react';

export default function CoreFeatures() {
  const benefits = [
    {
      title: "Compliance Intelligence",
      description: "Instantly audit required import-export checklists, phytosanitary/FDA certificates, and customs documentation across target destination markets.",
      icon: ShieldCheck,
    },
    {
      title: "Landed Cost Engine",
      description: "Model CIF/FOB export pricing, ocean/air freight estimates, destination import tariffs, and local VAT/GST in a single workflow.",
      icon: DollarSign,
    },
    {
      title: "Logistics Coordination",
      description: "Manage catalog products, create confirmed export orders, and connect directly with verified freight operators.",
      icon: Truck,
    }
  ];

  return (
    <section id="features" className="py-20 bg-background relative overflow-hidden border-t border-border">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto mb-14">
          <h2 className="font-display text-3xl sm:text-4xl font-bold text-foreground tracking-tight">
            Designed for Modern Exporters
          </h2>
          <p className="text-muted-foreground mt-3 text-sm sm:text-base leading-relaxed">
            Eliminate traditional trade bottlenecks with purpose-built tools that verify compliance, estimate landed costs, and coordinate global shipments.
          </p>
        </div>

        {/* Benefits Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {benefits.map((benefit, index) => {
            const Icon = benefit.icon;
            return (
              <div 
                key={index}
                className="card-claude rounded-xl p-6 flex flex-col items-center text-center group cursor-default"
              >
                {/* Icon Wrapper */}
                <div className="w-12 h-12 rounded-xl bg-accent text-primary border border-primary/20 flex items-center justify-center mb-5 group-hover:scale-105 transition-transform shadow-xs">
                  <Icon className="w-6 h-6" />
                </div>

                {/* Title & Description */}
                <h3 className="text-base font-semibold text-foreground mb-2 group-hover:text-primary transition-colors">{benefit.title}</h3>
                <p className="text-xs text-muted-foreground leading-relaxed">
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
