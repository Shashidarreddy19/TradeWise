import React from 'react';
import { UploadCloud, BarChart4, Compass, GitCommit, Ship } from 'lucide-react';

export default function HowItWorks() {
  const steps = [
    {
      num: "01",
      title: "Upload Product",
      subtitle: "HS code & attributes",
      description: "Enter your product name, material, or HS code. The deterministic classification engine resolves the exact tariff lines instantly.",
      icon: UploadCloud,
    },
    {
      num: "02",
      title: "Smart Analysis",
      subtitle: "Regulatory & cost index",
      description: "Calculates landed costs, MFN duties, VAT, compliance certifications, and required documentation for target destinations.",
      icon: BarChart4,
    },
    {
      num: "03",
      title: "Get Rankings",
      subtitle: "Ranked opportunities",
      description: "Receive high-precision destination rankings evaluated by machine-learning ranking models and tariff burden metrics.",
      icon: Compass,
    },
    {
      num: "04",
      title: "Explore Scenarios",
      subtitle: "Counterfactual simulator",
      description: "Model tariff impacts, shipping volume adjustments, and certification acquisitions in real-time.",
      icon: GitCommit,
    },
    {
      num: "05",
      title: "Connect & Export",
      subtitle: "Logistics matched",
      description: "Directly create export orders, request freight quotes from vetted logistics partners, and track shipment milestones.",
      icon: Ship,
    }
  ];

  return (
    <section id="how-it-works" className="py-20 bg-muted/40 relative overflow-hidden border-t border-border">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <h2 className="font-display text-3xl sm:text-4xl font-bold text-foreground tracking-tight">
            How TradeBridge Empowers Exporters
          </h2>
          <p className="text-muted-foreground mt-3 text-sm sm:text-base leading-relaxed">
            A structured workflow designed to simplify international trade decisions from catalog creation to final port delivery.
          </p>
        </div>

        {/* Steps Flow Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6 relative z-10">
          {steps.map((step, index) => {
            const IconComponent = step.icon;
            return (
              <div 
                key={index} 
                className="flex flex-col items-center text-center p-5 rounded-xl card-claude group cursor-default"
              >
                {/* Step Number Badge */}
                <span className="text-[11px] font-mono font-semibold text-primary bg-primary/10 px-2.5 py-0.5 rounded-full mb-4 shadow-xs">
                  Step {step.num}
                </span>

                {/* Step Icon */}
                <div className="w-12 h-12 rounded-xl flex items-center justify-center bg-accent text-primary border border-primary/20 mb-4 transition-transform group-hover:scale-105 shadow-xs">
                  <IconComponent className="w-6 h-6" />
                </div>

                {/* Title & Subtitle */}
                <h3 className="text-sm font-semibold text-foreground group-hover:text-primary transition-colors">
                  {step.title}
                </h3>
                <p className="text-[11px] font-medium text-muted-foreground mt-0.5">
                  {step.subtitle}
                </p>

                {/* Description */}
                <p className="text-xs text-muted-foreground mt-2 leading-relaxed">
                  {step.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
