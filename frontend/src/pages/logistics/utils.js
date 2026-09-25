/**
 * logistics/utils.js — Shared helpers for all logistics views.
 */

// ── Country flag emoji ───────────────────────────────────────────────────────
export const countryFlag = (name = '') => {
  const n = name.toLowerCase();
  if (n.includes('united states') || n.includes('usa') || n.includes('us'))   return '🇺🇸';
  if (n.includes('germany') || n.includes('deutschland'))                      return '🇩🇪';
  if (n.includes('united arab emirates') || n.includes('uae'))                 return '🇦🇪';
  if (n.includes('singapore'))                                                  return '🇸🇬';
  if (n.includes('united kingdom') || n.includes('uk'))                        return '🇬🇧';
  if (n.includes('hong kong'))                                                  return '🇭🇰';
  if (n.includes('bangladesh'))                                                 return '🇧🇩';
  if (n.includes('china'))                                                      return '🇨🇳';
  if (n.includes('saudi'))                                                      return '🇸🇦';
  if (n.includes('netherlands') || n.includes('holland'))                      return '🇳🇱';
  if (n.includes('australia'))                                                  return '🇦🇺';
  if (n.includes('japan'))                                                      return '🇯🇵';
  if (n.includes('india'))                                                      return '🇮🇳';
  if (n.includes('canada'))                                                     return '🇨🇦';
  if (n.includes('france'))                                                     return '🇫🇷';
  return '🌐';
};

// ── Shipment status display ──────────────────────────────────────────────────
export const SHIPMENT_STEPS = [
  { key: 'ASSIGNED',           label: 'Shipment Accepted',   short: 'Accepted' },
  { key: 'PICKED_UP',          label: 'Picked Up',           short: 'Picked Up' },
  { key: 'AT_EXPORT_CUSTOMS',  label: 'At Export Customs',   short: 'Export Customs' },
  { key: 'IN_TRANSIT',         label: 'In Transit',          short: 'In Transit' },
  { key: 'AT_IMPORT_CUSTOMS',  label: 'At Import Customs',   short: 'Import Customs' },
  { key: 'OUT_FOR_DELIVERY',   label: 'Out for Delivery',    short: 'Out for Del.' },
  { key: 'DELIVERED',          label: 'Delivered',           short: 'Delivered' },
];

export const formatShipmentStatus = (raw) => {
  const found = SHIPMENT_STEPS.find(s => s.key === raw);
  return found ? found.label : (raw || 'Unknown');
};

export const statusToBadgeClass = (label) => {
  if (label === 'Delivered') return 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20';
  if (label === 'Shipment Accepted') return 'bg-primary/10 text-primary border border-primary/20';
  return 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20';
};


// ── User initials ────────────────────────────────────────────────────────────
export const userInitials = (name = '') => {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase() || 'LP';
};

// ── Shipping mode → estimated transit days ───────────────────────────────────
export const shippingTransitDays = (mode = '', destination = '') => {
  const m = mode.toLowerCase();
  const d = destination.toLowerCase();
  if (m.includes('air')) {
    if (d.includes('usa') || d.includes('united states') || d.includes('germany') || d.includes('uk')) return '3–5 days';
    if (d.includes('uae') || d.includes('singapore') || d.includes('saudi')) return '2–4 days';
    return '4–7 days';
  }
  if (m.includes('sea')) {
    if (d.includes('usa') || d.includes('united states')) return '18–25 days';
    if (d.includes('germany') || d.includes('netherlands') || d.includes('uk')) return '22–30 days';
    if (d.includes('uae') || d.includes('saudi')) return '7–12 days';
    if (d.includes('singapore') || d.includes('hong kong')) return '10–15 days';
    if (d.includes('bangladesh') || d.includes('china')) return '8–14 days';
    return '14–21 days';
  }
  return '14–21 days';
};

// ── HS-code to customs document checklist ────────────────────────────────────
export const getCustomsDocuments = (hsCode = '', destination = '') => {
  const chapter = hsCode.replace(/\D/g, '').slice(0, 2);
  const docs = [
    { name: 'Commercial Invoice', mandatory: true, note: '3 originals, CIF value in USD' },
    { name: 'Packing List', mandatory: true, note: 'Net/gross weight per carton' },
    { name: 'Bill of Lading / Airway Bill', mandatory: true, note: 'Issued by carrier' },
    { name: 'Certificate of Origin', mandatory: true, note: 'Authorized chamber of commerce' },
    { name: 'Shipping Bill', mandatory: true, note: 'Filed on ICEGATE before departure' },
  ];

  // Food / Spices / Agri
  if (['02','03','07','08','09','10','11','12','15','16','17','18','19','20','21'].includes(chapter)) {
    docs.push({ name: 'Phytosanitary Certificate', mandatory: true, note: 'From National Plant Protection Organization' });
    docs.push({ name: 'Health / EIC Certificate', mandatory: true, note: 'Export Inspection Council' });
    if (chapter === '09') docs.push({ name: 'Spices Board Quality Certificate', mandatory: true, note: 'Spices Board of India' });
  }
  // Pharma
  if (chapter === '30' || chapter === '29') {
    docs.push({ name: 'WHO-GMP Certificate', mandatory: true, note: 'Manufacturing facility certification' });
    docs.push({ name: 'Free Sale Certificate', mandatory: true, note: 'From CDSCO' });
    docs.push({ name: 'Drug Master File (DMF)', mandatory: false, note: 'If required by importer' });
  }
  // Textiles / Garments
  if (['50','51','52','53','54','55','56','57','58','59','60','61','62','63'].includes(chapter)) {
    docs.push({ name: 'Textile Test Report', mandatory: true, note: 'Azo dye and fiber composition test' });
  }
  // Electronics
  if (chapter === '84' || chapter === '85') {
    docs.push({ name: 'Test/Conformity Report', mandatory: true, note: 'CE/FCC/IEC depending on market' });
  }
  // Chemicals (DG goods)
  if (['27','28','29','31','38'].includes(chapter)) {
    docs.push({ name: 'Safety Data Sheet (SDS)', mandatory: true, note: 'GHS-compliant, 16 sections' });
    docs.push({ name: 'Dangerous Goods Declaration', mandatory: true, note: 'IMDG/IATA DGD as applicable' });
  }

  // Country-specific
  const dest = destination.toLowerCase();
  if (dest.includes('uae') || dest.includes('saudi')) {
    docs.push({ name: 'Certificate of Origin (Arab League)', mandatory: true, note: 'Embassy/consulate attested' });
    if (['02','03','07','08','09','16','17','18','19','20','21'].includes(chapter)) {
      docs.push({ name: 'Halal Certificate', mandatory: true, note: 'From UAE/Saudi recognized halal body' });
    }
  }
  if (dest.includes('china')) {
    docs.push({ name: 'GACC Registration', mandatory: true, note: 'Exporter must register with China Customs' });
  }
  if (dest.includes('united states') || dest.includes('usa')) {
    docs.push({ name: 'Importer Security Filing (ISF 10+2)', mandatory: true, note: 'Filed 24hrs before vessel loading' });
    if (['02','03','07','08','09','16','17','19','20','21'].includes(chapter)) {
      docs.push({ name: 'FDA Prior Notice', mandatory: true, note: 'Via FDA Prior Notice System Interface' });
    }
  }

  return docs;
};

// ── Freight cost estimate ─────────────────────────────────────────────────────
export const estimateFreightCost = (mode = '', destination = '', weightKg = 1000) => {
  const m = mode.toLowerCase();
  const d = destination.toLowerCase();
  let ratePerKg;

  if (m.includes('air')) {
    ratePerKg = d.includes('usa') || d.includes('germany') ? 4.5
      : d.includes('uae') || d.includes('singapore') ? 3.2
      : 3.8;
    return {
      mode: 'Air Freight',
      ratePerKg: `USD ${ratePerKg.toFixed(2)}/kg`,
      estimated: `USD ${(ratePerKg * weightKg).toLocaleString()}`,
      transitDays: shippingTransitDays(mode, destination),
    };
  }
  // Sea
  const containerRates = d.includes('usa') ? 1800
    : d.includes('germany') || d.includes('netherlands') ? 2200
    : d.includes('uk') ? 2000
    : d.includes('uae') || d.includes('saudi') ? 700
    : d.includes('singapore') || d.includes('hong kong') ? 900
    : 1100;
  return {
    mode: 'Sea Freight (20 ft container)',
    ratePerKg: 'N/A (container rate)',
    estimated: `USD ${containerRates.toLocaleString()} (full container)`,
    transitDays: shippingTransitDays(mode, destination),
  };
};
