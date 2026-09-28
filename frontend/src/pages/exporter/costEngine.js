/**
 * costEngine.js — Enterprise-Grade Trade Economics & Export Profitability Engine
 *
 * Mathematically and commercially accurate export economics engine following international trade & WTO valuation standards:
 *
 * CRITICAL SEPARATION OF CONCEPTS:
 * 1. PRODUCT COST: Internal cost to prepare product (Manufacturing + Packaging + Labeling + Quality/Compliance).
 * 2. SELLER EXPORT COST: Actual costs incurred by the exporter to fulfill the Incoterm (e.g. CIF: Product + Origin Logistics + Freight + Insurance).
 * 3. SELLING / INVOICE VALUE: Commercial invoice billed to the buyer (Quantity × Selling Price).
 * 4. CUSTOMS VALUE: Legally applicable customs valuation basis at destination (CIF Transaction / Invoice Value).
 * 5. IMPORT TAX BASE: Customs Value + Applicable Duty (+ legally includable additions).
 * 6. BUYER LANDED COST: Invoice Value + Import Duty + Import VAT + Destination Handling & Clearance.
 * 7. EXPORTER PROFIT: Total Revenue in Calc Currency - Total Seller-Borne Cost (NOT Landed Cost).
 * 8. EXPORTER BREAK-EVEN: Seller Export Cost / Quantity.
 */

// ── Incoterms Responsibility Matrix ───────────────────────────────────────────

export const INCOTERMS = {
  EXW: {
    code: 'EXW',
    label: 'EXW – Ex Works',
    description: 'Buyer assumes all costs & risks from factory floor.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: false,
    sellerPaysExportClearance: false,
    sellerPaysFreight: false,
    sellerPaysInsurance: false,
    sellerPaysDestination: false,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  FCA: {
    code: 'FCA',
    label: 'FCA – Free Carrier',
    description: 'Seller delivers to named carrier cleared for export.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: false,
    sellerPaysInsurance: false,
    sellerPaysDestination: false,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  FOB: {
    code: 'FOB',
    label: 'FOB – Free On Board',
    description: 'Seller loads goods on board vessel at origin port cleared for export.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: false,
    sellerPaysInsurance: false,
    sellerPaysDestination: false,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  CFR: {
    code: 'CFR',
    label: 'CFR – Cost & Freight',
    description: 'Seller pays origin logistics, export clearance & ocean/air freight.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: true,
    sellerPaysInsurance: false,
    sellerPaysDestination: false,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  CIF: {
    code: 'CIF',
    label: 'CIF – Cost, Insurance, Freight',
    description: 'Seller pays origin logistics, export clearance, ocean freight & marine insurance.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: true,
    sellerPaysInsurance: true,
    sellerPaysDestination: false,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  CPT: {
    code: 'CPT',
    label: 'CPT – Carriage Paid To',
    description: 'Multimodal equivalent of CFR.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: true,
    sellerPaysInsurance: false,
    sellerPaysDestination: false,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  CIP: {
    code: 'CIP',
    label: 'CIP – Carriage & Insurance Paid To',
    description: 'Multimodal equivalent of CIF with comprehensive cargo insurance.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: true,
    sellerPaysInsurance: true,
    sellerPaysDestination: false,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  DAP: {
    code: 'DAP',
    label: 'DAP – Delivered At Place',
    description: 'Seller delivers to destination place; buyer clears customs & pays import taxes.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: true,
    sellerPaysInsurance: true,
    sellerPaysDestination: true,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  DPU: {
    code: 'DPU',
    label: 'DPU – Delivered at Place Unloaded',
    description: 'Seller delivers and unloads at destination terminal; buyer pays import taxes.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: true,
    sellerPaysInsurance: true,
    sellerPaysDestination: true,
    sellerPaysDuty: false,
    sellerPaysTax: false,
  },
  DDP: {
    code: 'DDP',
    label: 'DDP – Delivered Duty Paid',
    description: 'Seller bears all costs including destination logistics, import customs duty & VAT.',
    sellerPaysPackaging: true,
    sellerPaysOriginLogistics: true,
    sellerPaysExportClearance: true,
    sellerPaysFreight: true,
    sellerPaysInsurance: true,
    sellerPaysDestination: true,
    sellerPaysDuty: true,
    sellerPaysTax: true,
  },
};

export const COST_TYPES = {
  FIXED_PER_SHIPMENT:    'Fixed per shipment',
  VARIABLE_PER_UNIT:     'Variable per unit',
  VARIABLE_BY_WEIGHT:    'Variable by weight',
  VARIABLE_BY_VOLUME:    'Variable by CBM',
  VARIABLE_BY_CONTAINER: 'Variable by container',
  PERCENTAGE_OF_VALUE:   'Percentage of value',
};

export const CURRENCIES = ['INR', 'USD', 'EUR', 'GBP', 'SAR', 'AED', 'SGD', 'AUD', 'JPY', 'CNY'];

export const MISSING = null;

// ── Currency Conversion Helpers ───────────────────────────────────────────────

export function convertCurrency(amount, sourceCurrency, targetCurrency, fxRates = {}) {
  if (amount == null || isNaN(amount)) return { amount: MISSING, rate: MISSING, source: 'Invalid amount', verified: false };
  if (sourceCurrency === targetCurrency) {
    return { amount: Number(amount), rate: 1, source: 'Same currency', verified: true, timestamp: new Date().toISOString().split('T')[0] };
  }

  const directKey = `${sourceCurrency}_${targetCurrency}`;
  const inverseKey = `${targetCurrency}_${sourceCurrency}`;

  let rate = null;
  let source = 'User-provided FX';
  let verified = false;
  let ts = null;

  if (fxRates[directKey] != null && !isNaN(fxRates[directKey])) {
    rate = Number(fxRates[directKey]);
    source = fxRates[`${directKey}_source`] || 'Official FX Source';
    verified = fxRates[`${directKey}_verified`] !== false;
    ts = fxRates[`${directKey}_ts`] || null;
  } else if (fxRates[inverseKey] != null && !isNaN(fxRates[inverseKey]) && Number(fxRates[inverseKey]) > 0) {
    rate = 1 / Number(fxRates[inverseKey]);
    source = `Inverted ${fxRates[`${inverseKey}_source`] || 'FX Source'}`;
    verified = fxRates[`${inverseKey}_verified`] !== false;
    ts = fxRates[`${inverseKey}_ts`] || null;
  }

  if (rate == null) {
    return { amount: MISSING, rate: MISSING, source: `Exchange rate ${sourceCurrency} → ${targetCurrency} not found`, verified: false, timestamp: null };
  }

  return {
    amount: Number(amount) * rate,
    rate,
    source,
    verified,
    timestamp: ts,
  };
}

// ── Jurisdiction Validation ────────────────────────────────────────────────────
//
// Generic, destination-agnostic check: a duty/tax source string may only be
// treated as "Verified" if it genuinely names an authority applicable to the
// selected destination country. This prevents the engine from ever labeling a
// foreign-jurisdiction source (e.g. "EU TARIC Database" for a Singapore
// shipment) as Verified — the exact defect described in the audit. The map
// below associates known authority name fragments with the jurisdiction(s)
// they legitimately cover; it is data about regulatory bodies, not a
// per-country calculation branch, and any destination not covered simply
// falls through to "no match => unverified" rather than being silently
// assumed correct.
const AUTHORITY_JURISDICTIONS = {
  'eu taric': ['germany', 'netherlands', 'france', 'italy', 'eu', 'european union'],
  'eu vat directive': ['germany', 'netherlands', 'france', 'italy', 'eu', 'european union'],
  'gcc unified': ['saudi arabia', 'united arab emirates', 'uae', 'qatar', 'kuwait', 'bahrain', 'oman'],
  'zatca': ['saudi arabia'],
  'uae federal tax authority': ['united arab emirates', 'uae'],
  'usitc': ['united states', 'usa', 'us'],
  'us state level sales tax': ['united states', 'usa', 'us'],
  'uk global tariff': ['united kingdom', 'uk'],
  'hmrc': ['united kingdom', 'uk'],
  'singapore customs': ['singapore'],
};

function isSourceJurisdictionMatched(sourceLabel, destinationCountry) {
  if (!sourceLabel || !destinationCountry) return false;
  const src = String(sourceLabel).toLowerCase();
  const dest = String(destinationCountry).toLowerCase();
  if (src.includes(dest)) return true;
  for (const [authority, jurisdictions] of Object.entries(AUTHORITY_JURISDICTIONS)) {
    if (src.includes(authority)) return jurisdictions.includes(dest);
  }
  return false;
}

/**
 * validateDutySource / validateTaxSource — engine-level guard so a mismatched
 * source can never be recorded as "Verified" regardless of what the caller
 * passed in. Returns the (possibly downgraded) verified flag plus a reason.
 */
export function validateDutySource(source, verifiedFlag, destinationCountry) {
  const matched = isSourceJurisdictionMatched(source, destinationCountry);
  return {
    verified: Boolean(verifiedFlag) && matched,
    reason: matched ? null : `Source "${source || 'unknown'}" is not confirmed applicable to ${destinationCountry || 'the selected destination'}.`,
  };
}

export function validateTaxSource(source, verifiedFlag, destinationCountry) {
  return validateDutySource(source, verifiedFlag, destinationCountry);
}

export function validateInputs(inputs) {
  const errors = [];
  if (!inputs.origin?.trim())      errors.push('Origin country is required.');
  if (!inputs.destination?.trim()) errors.push('Destination country is required.');
  if (!inputs.hsCode?.trim())      errors.push('HS Code is required.');
  if (!inputs.freightMode)         errors.push('Freight mode is required.');

  const qty = parseFloat(inputs.quantity);
  if (isNaN(qty) || qty <= 0) errors.push('Quantity must be greater than 0.');

  const unitMfgCost = parseFloat(inputs.unitMfgCost);
  if (isNaN(unitMfgCost) || unitMfgCost < 0) errors.push('Unit manufacturing cost must be 0 or greater.');

  const unitWeight = parseFloat(inputs.unitWeight);
  if (isNaN(unitWeight) || unitWeight <= 0) errors.push('Unit weight (kg) must be greater than 0.');

  return { valid: errors.length === 0, errors };
}

// ── Centralized Calculation Engine: calculateTradeEconomics ────────────────────

/**
 * calculateTradeEconomics / calculateExportCost
 * Master centralized calculation engine for export trade economics.
 */
export function calculateTradeEconomics(transaction, fxRates = {}, dutyData = {}, taxData = {}, _options = {}) {
  // `_options.skipDerivedAnalysis` is an internal recursion guard. Scenario analysis
  // (section 17) re-invokes this SAME engine with a cloned transaction whose only change
  // is the freight assumption — that is the single source of truth. When it does so it
  // sets this flag so the nested call skips building its own scenarios/sensitivity/quantity
  // tiers (which would recurse infinitely and is unnecessary for a scenario sub-result).
  const skipDerivedAnalysis = _options.skipDerivedAnalysis === true;
  const CC = transaction.calculationCurrency || 'INR'; // Base calculation currency (Internal Ledger)
  const qty = parseFloat(transaction.quantity) || 1;
  const unitWeight = parseFloat(transaction.unitWeight) || 1.0;
  const totalWeightKg = qty * unitWeight;
  const incotermKey = (transaction.incoterm || 'CIF').toUpperCase();
  const incotermRules = INCOTERMS[incotermKey] || INCOTERMS['CIF'];

  const assumptions = [];
  const confidenceReasons = [];
  const costLedger = {}; // unique ID -> cost item
  const traceability = [];
  const warnings = [];

  let isAllVerified = true;
  let hasCriticalMissing = false;

  // Helper to record an entry in the cost ledger
  const record = (id, label, group, amountCC, origAmount, origCurrency, type, basis, source, verified = false, sellerBears = true) => {
    if (amountCC === MISSING || isNaN(amountCC)) return;
    costLedger[id] = {
      id,
      label,
      group,
      amountCC: Number(amountCC),
      amountPerUnitCC: Number(amountCC) / qty,
      originalAmount: Number(origAmount ?? amountCC),
      originalCurrency: origCurrency || CC,
      type,
      basis,
      source: source || (verified ? 'Official database' : 'User input'),
      verified: Boolean(verified),
      sellerBears: Boolean(sellerBears),
    };

    if (!verified) isAllVerified = false;
  };

  // Helper to convert to CC
  const toCC = (amount, fromCurr) => {
    if (amount == null || isNaN(amount) || amount === '') return { ok: false, amount: MISSING };
    const num = Number(amount);
    if (!fromCurr || fromCurr === CC) return { ok: true, amount: num, rate: 1, source: 'Base currency' };
    const conv = convertCurrency(num, fromCurr, CC, fxRates);
    if (conv.amount === MISSING) {
      warnings.push(`Exchange rate ${fromCurr} → ${CC} missing.`);
      hasCriticalMissing = true;
      return { ok: false, amount: MISSING, error: conv.source };
    }
    return { ok: true, amount: conv.amount, rate: conv.rate, source: conv.source, verified: conv.verified, timestamp: conv.timestamp };
  };

  // ── 1. FX Engine for Transaction Currencies ──────────────────────────────────
  const sellingCurrency = transaction.sellingCurrency || 'SAR';
  const sellingPriceRaw = parseFloat(transaction.sellingPricePerUnit);

  let sellingPricePerUnitOriginal = !isNaN(sellingPriceRaw) && sellingPriceRaw > 0 ? sellingPriceRaw : null;
  let fxRateSellingToCC = 1;
  let fxSource = 'Same currency';
  let fxVerified = true;

  if (sellingCurrency !== CC) {
    const directKey = `${sellingCurrency}_${CC}`;
    const inverseKey = `${CC}_${sellingCurrency}`;
    if (fxRates[directKey] != null) {
      fxRateSellingToCC = Number(fxRates[directKey]);
      fxSource = fxRates[`${directKey}_source`] || 'Official FX Rate';
      fxVerified = fxRates[`${directKey}_verified`] !== false;
    } else if (fxRates[inverseKey] != null && Number(fxRates[inverseKey]) > 0) {
      fxRateSellingToCC = 1 / Number(fxRates[inverseKey]);
      fxSource = `Inverted (${fxRates[`${inverseKey}_source`] || 'FX Rate'})`;
      fxVerified = fxRates[`${inverseKey}_verified`] !== false;
    } else {
      warnings.push(`Exchange rate ${sellingCurrency} → ${CC} not provided. Cannot compare selling price.`);
      confidenceReasons.push({ item: `FX Rate (${sellingCurrency} → ${CC})`, status: 'Not verified', note: 'Missing FX rate' });
      hasCriticalMissing = true;
    }
  }

  confidenceReasons.push({
    item: `FX Rate (${sellingCurrency} → ${CC})`,
    status: fxVerified ? 'Verified' : 'User-provided',
    note: `1 ${sellingCurrency} = ${fxRateSellingToCC.toFixed(4)} ${CC} (${fxSource})`
  });

  const sellingPricePerUnitCC = sellingPricePerUnitOriginal != null ? sellingPricePerUnitOriginal * fxRateSellingToCC : null;
  const totalRevenueOriginal = sellingPricePerUnitOriginal != null ? sellingPricePerUnitOriginal * qty : null;
  const totalRevenueCC = totalRevenueOriginal != null ? totalRevenueOriginal * fxRateSellingToCC : null;

  // ── 2. Product Cost (Manufacturing & Quality) ────────────────────────────────
  const unitMfgCost = parseFloat(transaction.unitMfgCost) || 0;
  const mfgCurrency = transaction.mfgCurrency || CC;
  const mfgConv = toCC(unitMfgCost * qty, mfgCurrency);
  if (mfgConv.ok) {
    record('manufacturing', 'Base Manufacturing', 'Manufacturing',
      mfgConv.amount, unitMfgCost * qty, mfgCurrency, COST_TYPES.VARIABLE_PER_UNIT,
      `${qty.toLocaleString()} units × ${mfgCurrency} ${unitMfgCost.toFixed(2)}/unit`,
      transaction.mfgSource || 'Product master / User input', false, true);
  }

  // Packaging
  const packPerUnit = parseFloat(transaction.packagingCostPerUnit) || 0;
  if (packPerUnit > 0) {
    const packConv = toCC(packPerUnit * qty, transaction.packagingCurrency || CC);
    if (packConv.ok) {
      record('packaging', 'Export Packaging', 'Manufacturing',
        packConv.amount, packPerUnit * qty, transaction.packagingCurrency || CC, COST_TYPES.VARIABLE_PER_UNIT,
        `${qty.toLocaleString()} units × ₹${packPerUnit.toFixed(2)}/unit`,
        'User input', false, true);
    }
  }

  // Labeling
  const labelPerUnit = parseFloat(transaction.labelingCostPerUnit) || 0;
  if (labelPerUnit > 0) {
    const labelConv = toCC(labelPerUnit * qty, CC);
    if (labelConv.ok) {
      record('labeling', 'Export Labeling & Barcoding', 'Manufacturing',
        labelConv.amount, labelPerUnit * qty, CC, COST_TYPES.VARIABLE_PER_UNIT,
        `${qty.toLocaleString()} units × ₹${labelPerUnit.toFixed(2)}/unit`,
        'User input', false, true);
    }
  }

  // Quality, Testing & Certification
  const inspectionCost = parseFloat(transaction.qualityInspectionCost) || 0;
  const testingCost = parseFloat(transaction.productTestingCost) || 0;
  const certCost = parseFloat(transaction.certificationCost) || 0;
  const qualityTotal = inspectionCost + testingCost + certCost;
  if (qualityTotal > 0) {
    record('quality_compliance', 'Quality, Testing & Certifications', 'Manufacturing',
      qualityTotal, qualityTotal, CC, COST_TYPES.FIXED_PER_SHIPMENT,
      `Inspection (₹${inspectionCost}) + Testing (₹${testingCost}) + Certs (₹${certCost})`,
      'User input / Compliance estimate', false, true);
  }

  const otherProd = parseFloat(transaction.otherProductionCost) || 0;
  if (otherProd > 0) {
    record('other_production', 'Other Production Costs', 'Manufacturing',
      otherProd, otherProd, CC, COST_TYPES.FIXED_PER_SHIPMENT,
      'Other production expenses', 'User input', false, true);
  }

  const productCostCC = (costLedger['manufacturing']?.amountCC || 0)
    + (costLedger['packaging']?.amountCC || 0)
    + (costLedger['labeling']?.amountCC || 0)
    + (costLedger['quality_compliance']?.amountCC || 0)
    + (costLedger['other_production']?.amountCC || 0);

  // ── 3. Origin Logistics & Export Preparation ────────────────────────────────
  const sellerBearsOrigin = incotermRules.sellerPaysOriginLogistics;

  const exportDocs = parseFloat(transaction.exportDocumentationCost) || 0;
  const customsBroker = parseFloat(transaction.customsBrokerFee) || 0;
  const fwdFee = parseFloat(transaction.freightForwardingFee) || 0;
  const inlandTransport = parseFloat(transaction.inlandTransportCost) || 0;
  const loadingCharges = parseFloat(transaction.loadingHandlingCharges) || 0;
  const warehouseOrigin = parseFloat(transaction.warehouseStorageOrigin) || 0;
  const portHandling = parseFloat(transaction.portTerminalHandling) || 0;
  const exportClearance = parseFloat(transaction.exportCustomsClearance) || 0;

  if (exportDocs > 0)      record('export_docs', 'Export Documentation', 'Origin Logistics', exportDocs, exportDocs, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Certificates, COO, Shipping Bill', 'User input', false, sellerBearsOrigin);
  if (customsBroker > 0)   record('customs_broker', 'Customs Broker / CHA Fee', 'Origin Logistics', customsBroker, customsBroker, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Origin Customs Agent Fee', 'User input', false, sellerBearsOrigin);
  if (fwdFee > 0)          record('freight_forwarding', 'Freight Forwarding Handling', 'Origin Logistics', fwdFee, fwdFee, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Forwarder processing fee', 'User input', false, sellerBearsOrigin);
  if (inlandTransport > 0) record('inland_transport', 'Inland Transport to Port', 'Origin Logistics', inlandTransport, inlandTransport, CC, COST_TYPES.FIXED_PER_SHIPMENT, `Factory to departure port (${totalWeightKg.toLocaleString()} kg)`, 'User input / Logistics rate', false, sellerBearsOrigin);
  if (loadingCharges > 0)  record('loading_handling', 'Origin Loading & Handling', 'Origin Logistics', loadingCharges, loadingCharges, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Terminal stuffing/loading', 'User input', false, sellerBearsOrigin);
  if (warehouseOrigin > 0) record('warehouse_origin', 'Origin CFS/Warehouse Storage', 'Origin Logistics', warehouseOrigin, warehouseOrigin, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Pre-shipment warehousing', 'User input', false, sellerBearsOrigin);
  if (portHandling > 0)    record('port_handling', 'Origin Port / THC Charges', 'Origin Logistics', portHandling, portHandling, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Port terminal handling charges', 'User input', false, sellerBearsOrigin);
  if (exportClearance > 0) record('export_clearance', 'Export Customs Clearance', 'Origin Logistics', exportClearance, exportClearance, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Origin ICEGATE clearance', 'User input', false, incotermRules.sellerPaysExportClearance);

  // ── 4. International Freight ────────────────────────────────────────────────
  const freightMode = transaction.freightMode || 'Sea';
  const rawFreight = parseFloat(transaction.freightCost);
  const freightCurrency = transaction.freightCurrency || CC;
  let freightCostCC = MISSING;
  let freightSource = transaction.freightSource || 'User-provided';
  let freightVerified = Boolean(transaction.freightVerified);

  if (!isNaN(rawFreight) && rawFreight > 0) {
    const fConv = toCC(rawFreight, freightCurrency);
    if (fConv.ok) {
      freightCostCC = fConv.amount;
      record('international_freight', `International Freight (${freightMode})`, 'International Freight',
        freightCostCC, rawFreight, freightCurrency, COST_TYPES.FIXED_PER_SHIPMENT,
        `${freightMode} — ${transaction.freightBasis || 'Per shipment'}`, freightSource, freightVerified, incotermRules.sellerPaysFreight);
      confidenceReasons.push({ item: `Freight (${freightMode})`, status: freightVerified ? 'Verified' : 'User-provided', note: `${freightCurrency} ${rawFreight.toLocaleString()}` });
    }
  } else {
    confidenceReasons.push({ item: `Freight (${freightMode})`, status: 'Not provided', note: 'Required for CIF/CFR/DDP calculations' });
    if (incotermRules.sellerPaysFreight) {
      warnings.push('Freight cost is required for the selected Incoterm.');
    }
  }

  // ── 5. Marine Cargo Insurance ───────────────────────────────────────────────
  let insuranceCostCC = MISSING;
  let insuranceSource = 'User-provided rate';
  let insuranceVerified = false;
  const rawInsuranceFixed = parseFloat(transaction.insuranceFixedAmount);
  const rawInsuranceRate = parseFloat(transaction.insuranceRate);

  // Insured Value = (Product Cost + Freight Cost)
  const productAndFreightCC = (costLedger['manufacturing']?.amountCC || 0) + (freightCostCC !== MISSING ? freightCostCC : 0);

  if (!isNaN(rawInsuranceFixed) && rawInsuranceFixed > 0) {
    const iConv = toCC(rawInsuranceFixed, transaction.insuranceCurrency || CC);
    if (iConv.ok) {
      insuranceCostCC = iConv.amount;
      insuranceSource = 'User input (fixed amount)';
      record('insurance', 'Marine Cargo Insurance', 'Insurance',
        insuranceCostCC, rawInsuranceFixed, transaction.insuranceCurrency || CC, COST_TYPES.FIXED_PER_SHIPMENT,
        'Fixed insurance policy premium', insuranceSource, false, incotermRules.sellerPaysInsurance);
      confidenceReasons.push({ item: 'Insurance Premium', status: 'User-provided', note: `Fixed amount: ₹${insuranceCostCC.toLocaleString()}` });
    }
  } else if (!isNaN(rawInsuranceRate) && rawInsuranceRate >= 0) {
    insuranceCostCC = productAndFreightCC * (rawInsuranceRate / 100);
    insuranceSource = `User input rate (${rawInsuranceRate}% of insured value)`;
    record('insurance', 'Marine Cargo Insurance', 'Insurance',
      insuranceCostCC, insuranceCostCC, CC, COST_TYPES.PERCENTAGE_OF_VALUE,
      `${rawInsuranceRate}% × (Product ₹${(costLedger['manufacturing']?.amountCC || 0).toLocaleString()} + Freight ₹${(freightCostCC || 0).toLocaleString()} = ₹${productAndFreightCC.toLocaleString()})`,
      insuranceSource, false, incotermRules.sellerPaysInsurance);
    confidenceReasons.push({ item: 'Insurance Rate', status: 'User-provided', note: `${rawInsuranceRate}% on insured value of ₹${productAndFreightCC.toLocaleString()}` });
  }



  // ── 7. Commercial Invoice Value (Buyer Purchase Price) ───────────────────────
  // Under international commerce, Invoice Value is Quantity × Selling Price
  const invoiceValueOriginal = totalRevenueOriginal;
  const invoiceValueCC = totalRevenueCC;

  // ── 8. Destination Customs Valuation ────────────────────────────────────────
  // Under WTO Customs Valuation & Saudi ZATCA Rules:
  // For CIF transactions, Customs Value is the CIF Commercial Transaction Value / Invoice Value.
  const customsValuationBasis = 'CIF Transaction / Commercial Invoice Value';
  const customsValueOriginal = invoiceValueOriginal;
  const customsValueCC = invoiceValueCC;

  // ── 9. Customs Duty Calculation ──────────────────────────────────────────────
  let dutyRate = MISSING;
  let dutyCostCC = MISSING;
  let dutyCostOriginal = MISSING;
  let dutySource = 'Destination tariff line not verified';
  let dutyVerified = false;

  if (dutyData?.rate != null && dutyData.rate !== '') {
    dutyRate = parseFloat(dutyData.rate);
    dutySource = dutyData.source || 'Source not specified';
    dutyVerified = dutyData.verified !== false;
  } else if (transaction.manualDutyRate != null && transaction.manualDutyRate !== '') {
    dutyRate = parseFloat(transaction.manualDutyRate);
    dutySource = transaction.dutySource || 'User-provided rate (unverified)';
    dutyVerified = Boolean(transaction.dutyVerified);
  }

  // Engine-level jurisdiction guard: a duty source can never be reported as
  // Verified unless it is actually applicable to the destination country —
  // this is enforced here regardless of what the caller passed in, so a
  // mismatched source (e.g. an EU source for a Singapore shipment) cannot slip
  // through even if an upstream caller mistakenly marks it verified.
  const dutySourceCheck = validateDutySource(dutySource, dutyVerified, transaction.destination);
  dutyVerified = dutySourceCheck.verified;
  if (dutySourceCheck.reason) warnings.push(`Customs duty: ${dutySourceCheck.reason}`);

  if (dutyRate != null && !isNaN(dutyRate) && customsValueCC != null) {
    dutyCostCC = customsValueCC * (dutyRate / 100);
    dutyCostOriginal = customsValueOriginal != null ? customsValueOriginal * (dutyRate / 100) : null;
    record('import_duty', 'Import Customs Duty', 'Customs Duty',
      dutyCostCC, dutyCostOriginal, sellingCurrency, COST_TYPES.PERCENTAGE_OF_VALUE,
      `Customs Value ${sellingCurrency} ${customsValueOriginal?.toLocaleString() || 0} × ${dutyRate}%`,
      dutySource, dutyVerified, incotermRules.sellerPaysDuty);
    confidenceReasons.push({ item: 'Customs Duty', status: dutyVerified ? 'Verified' : 'User-provided', note: `${dutyRate}% (${dutySource})` });
  } else {
    confidenceReasons.push({ item: 'Customs Duty', status: 'Not verified', note: 'Duty rate required for destination customs' });
  }

  // Anti-dumping / safeguard duties
  const antiDumping = parseFloat(transaction.antiDumpingDuty) || 0;
  if (antiDumping > 0) {
    record('antidumping_duty', 'Anti-Dumping / Trade Remedy Duty', 'Customs Duty',
      antiDumping, antiDumping, CC, COST_TYPES.FIXED_PER_SHIPMENT,
      'Trade remedy / countervailing order', 'User input', false, incotermRules.sellerPaysDuty);
  }

  // ── 10. Import VAT / GST (Proper Legal Tax Base) ─────────────────────────────
  // Import VAT Taxable Base = Customs Value + Applicable Duty (+ other legally includable amounts)
  const totalDutyCC = (dutyCostCC !== MISSING ? dutyCostCC : 0) + antiDumping;
  const totalDutyOriginal = (dutyCostOriginal !== MISSING ? dutyCostOriginal : 0) + (fxRateSellingToCC > 0 ? antiDumping / fxRateSellingToCC : 0);

  // The import-VAT taxable base is destination-specific. Rather than assuming one universal
  // formula, the caller may supply the jurisdiction's rule via taxData.taxableBaseComponents
  // (a set of the components that jurisdiction legally includes). We only ever include
  // components that actually exist in this calculation — nothing is invented. The default,
  // when a caller supplies no explicit rule, is the customs value + applicable duties, which
  // is the standard destination-import VAT base used by Saudi Arabia (ZATCA), the GCC, the EU
  // and most jurisdictions — but it is applied because it is the default rule, not because it
  // is hardcoded per country here.
  const taxBaseComponents = (taxData && Array.isArray(taxData.taxableBaseComponents) && taxData.taxableBaseComponents.length)
    ? taxData.taxableBaseComponents
    : ['customsValue', 'duty'];
  const includesComponent = (c) => taxBaseComponents.includes(c);
  const vatTaxableBaseCC =
      (includesComponent('customsValue') && customsValueCC != null ? customsValueCC : 0)
    + (includesComponent('duty') ? totalDutyCC : 0);
  const vatTaxableBaseOriginal =
      (includesComponent('customsValue') && customsValueOriginal != null ? customsValueOriginal : 0)
    + (includesComponent('duty') ? totalDutyOriginal : 0);
  const taxableBaseRuleLabel = taxData?.taxableBaseRuleLabel
    || `${(transaction.destination || 'Destination')} import-VAT base = ${taxBaseComponents.join(' + ')}`;

  let taxRate = MISSING;
  let taxCostCC = MISSING;
  let taxCostOriginal = MISSING;
  let taxSource = 'Tax rate not verified';
  let taxVerified = false;

  if (taxData?.rate != null && taxData.rate !== '') {
    taxRate = parseFloat(taxData.rate);
    taxSource = taxData.source || 'Source not specified';
    taxVerified = taxData.verified !== false;
  } else if (transaction.manualTaxRate != null && transaction.manualTaxRate !== '') {
    taxRate = parseFloat(transaction.manualTaxRate);
    taxSource = transaction.taxSource || 'User-provided tax rate';
    taxVerified = Boolean(transaction.taxVerified);
  }

  // Same engine-level jurisdiction guard as customs duty above.
  const taxSourceCheck = validateTaxSource(taxSource, taxVerified, transaction.destination);
  taxVerified = taxSourceCheck.verified;
  if (taxSourceCheck.reason) warnings.push(`Import VAT/GST: ${taxSourceCheck.reason}`);

  if (taxRate != null && !isNaN(taxRate) && vatTaxableBaseCC > 0) {
    taxCostCC = vatTaxableBaseCC * (taxRate / 100);
    taxCostOriginal = vatTaxableBaseOriginal * (taxRate / 100);
    record('import_vat', 'Import VAT / GST', 'Import Taxes',
      taxCostCC, taxCostOriginal, sellingCurrency, COST_TYPES.PERCENTAGE_OF_VALUE,
      `Tax Base ${sellingCurrency} ${vatTaxableBaseOriginal.toLocaleString()} (Customs ${customsValueOriginal?.toLocaleString() || 0} + Duty ${totalDutyOriginal.toFixed(2)}) × ${taxRate}%`,
      taxSource, taxVerified, incotermRules.sellerPaysTax);
    confidenceReasons.push({ item: 'Import VAT/GST', status: taxVerified ? 'Verified' : 'User-provided', note: `${taxRate}% (${taxSource})` });
  } else {
    confidenceReasons.push({ item: 'Import VAT/GST', status: 'Not verified', note: 'VAT rate required' });
  }

  // ── 11. Destination Handling & Local Logistics ──────────────────────────────
  const sellerBearsDest = incotermRules.sellerPaysDestination;
  const destPort = parseFloat(transaction.destinationPortHandling) || 0;
  const destCustoms = parseFloat(transaction.destinationCustomsClearance) || 0;
  const destDocs = parseFloat(transaction.destinationImportDocs) || 0;
  const destTransport = parseFloat(transaction.destinationLocalTransport) || 0;
  const destWh = parseFloat(transaction.destinationWarehousing) || 0;
  const destDelivery = parseFloat(transaction.destinationDelivery) || 0;
  const destOther = parseFloat(transaction.destinationOther) || 0;

  if (destPort > 0)      record('dest_port', 'Destination Port / THC', 'Destination Charges', destPort, destPort, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Destination port handling', 'User input / Logistics rate', false, sellerBearsDest);
  if (destCustoms > 0)   record('dest_customs', 'Destination Customs Clearance', 'Destination Charges', destCustoms, destCustoms, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Import clearance agent fee', 'User input', false, sellerBearsDest);
  if (destDocs > 0)      record('dest_docs', 'Destination Import Documentation', 'Destination Charges', destDocs, destDocs, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Import permit, DO delivery order', 'User input', false, sellerBearsDest);
  if (destTransport > 0) record('dest_transport', 'Destination Inland Transport', 'Destination Charges', destTransport, destTransport, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Port to destination warehouse', 'User input', false, sellerBearsDest);
  if (destWh > 0)        record('dest_warehouse', 'Destination Warehousing', 'Destination Charges', destWh, destWh, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Destination storage', 'User input', false, sellerBearsDest);
  if (destDelivery > 0)  record('dest_delivery', 'Final Buyer Delivery', 'Destination Charges', destDelivery, destDelivery, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Last-mile delivery to buyer facility', 'User input', false, sellerBearsDest);
  if (destOther > 0)     record('dest_other', 'Other Destination Charges', 'Destination Charges', destOther, destOther, CC, COST_TYPES.FIXED_PER_SHIPMENT, 'Sundry import expenses', 'User input', false, sellerBearsDest);

  const destinationChargesCC = destPort + destCustoms + destDocs + destTransport + destWh + destDelivery + destOther;
  const destinationChargesOriginal = fxRateSellingToCC > 0 ? destinationChargesCC / fxRateSellingToCC : 0;

  // ── 12. Seller Incoterm Export Cost (Total Exporter Expense) ─────────────────
  let totalSellerCostCC = 0;
  Object.values(costLedger).forEach(c => {
    if (c.sellerBears) totalSellerCostCC += c.amountCC;
  });
  const sellerCostPerUnitCC = qty > 0 ? totalSellerCostCC / qty : 0;
  const exporterBreakEvenPerUnitCC = sellerCostPerUnitCC;
  const exporterBreakEvenPerUnitOriginal = fxRateSellingToCC > 0 ? exporterBreakEvenPerUnitCC / fxRateSellingToCC : null;

  // ── 12. Buyer Landed Cost (Starts from Buyer Invoice Value) ─────────────────
  // Under international trade standards:
  // Buyer Landed Cost = CIF Invoice Value + Import Duty + Import VAT + Destination Charges + Other Buyer Costs
  let buyerLandedCostCC = null;
  let buyerLandedCostOriginal = null;
  let landedCostPerUnitCC = null;
  let landedCostPerUnitOriginal = null;

  if (invoiceValueCC != null) {
    buyerLandedCostCC = invoiceValueCC
      + (dutyCostCC !== MISSING ? dutyCostCC : 0)
      + antiDumping
      + (taxCostCC !== MISSING ? taxCostCC : 0)
      + (incotermRules.sellerPaysDestination ? 0 : destinationChargesCC);

    buyerLandedCostOriginal = invoiceValueOriginal
      + (dutyCostOriginal !== MISSING ? dutyCostOriginal : 0)
      + (fxRateSellingToCC > 0 ? antiDumping / fxRateSellingToCC : 0)
      + (taxCostOriginal !== MISSING ? taxCostOriginal : 0)
      + (incotermRules.sellerPaysDestination ? 0 : destinationChargesOriginal);

    landedCostPerUnitCC = qty > 0 ? buyerLandedCostCC / qty : 0;
    landedCostPerUnitOriginal = qty > 0 ? buyerLandedCostOriginal / qty : 0;
  }

  // ── 13. Exporter Profitability & Margin Math ─────────────────────────────────
  let exporterProfitCC = null;
  let exporterProfitOriginal = null;
  let exporterProfitPerUnitCC = null;
  let exporterProfitPerUnitOriginal = null;
  let exporterMarginPct = null;
  let exporterMarkupPct = null;

  if (totalRevenueCC != null && totalSellerCostCC > 0) {
    exporterProfitCC = totalRevenueCC - totalSellerCostCC;
    exporterProfitOriginal = fxRateSellingToCC > 0 ? exporterProfitCC / fxRateSellingToCC : null;
    exporterProfitPerUnitCC = exporterProfitCC / qty;
    exporterProfitPerUnitOriginal = fxRateSellingToCC > 0 ? exporterProfitPerUnitCC / fxRateSellingToCC : null;
    exporterMarginPct = totalRevenueCC > 0 ? (exporterProfitCC / totalRevenueCC) * 100 : null;
    exporterMarkupPct = totalSellerCostCC > 0 ? (exporterProfitCC / totalSellerCostCC) * 100 : null;
  }

  const isAboveExporterBreakEven = sellingPricePerUnitCC != null && sellingPricePerUnitCC >= exporterBreakEvenPerUnitCC;
  const isAboveLandedCost = sellingPricePerUnitCC != null && landedCostPerUnitCC != null && sellingPricePerUnitCC >= landedCostPerUnitCC;

  // ── 14. Target Selling Price (Proper Margin Formula) ─────────────────────────
  const targetMarginInput = parseFloat(transaction.targetProfitMarginPct);
  const targetMarginPct = !isNaN(targetMarginInput) && targetMarginInput >= 0 && targetMarginInput < 100 ? targetMarginInput : 20;

  const targetPricePerUnitCC = sellerCostPerUnitCC / (1 - targetMarginPct / 100);
  const targetPricePerUnitOriginal = fxRateSellingToCC > 0 ? targetPricePerUnitCC / fxRateSellingToCC : null;
  const targetRevenueCC = targetPricePerUnitCC * qty;

  // ── 15. Auditable Traceability & Formula Explanations ────────────────────────
  traceability.push({
    title: 'Selling Price Currency Conversion',
    formula: `${sellingPricePerUnitOriginal?.toLocaleString() || 0} ${sellingCurrency}/unit × ${fxRateSellingToCC.toFixed(4)} ${CC}/${sellingCurrency} = ₹${(sellingPricePerUnitCC || 0).toFixed(2)}/unit`,
    category: 'Currency & Revenue',
  });
  traceability.push({
    title: 'Total Export Revenue (CIF Invoice Value)',
    formula: `${qty.toLocaleString()} units × ${sellingPricePerUnitOriginal?.toLocaleString() || 0} ${sellingCurrency} = ${invoiceValueOriginal?.toLocaleString() || 0} ${sellingCurrency} (≈ ₹${(invoiceValueCC || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })})`,
    category: 'Currency & Revenue',
  });
  traceability.push({
    title: 'Insured Value for Cargo Insurance',
    formula: `Product Cost (₹${(costLedger['manufacturing']?.amountCC || 0).toLocaleString()}) + Freight (₹${(freightCostCC || 0).toLocaleString()}) = ₹${productAndFreightCC.toLocaleString()}`,
    category: 'Logistics & Insurance',
  });
  traceability.push({
    title: 'Seller Incoterm Cost Total',
    formula: `All seller-borne costs under ${incotermKey}: Manufacturing (₹${productCostCC.toLocaleString()}) + Origin Logistics (₹${((costLedger['export_docs']?.amountCC || 0) + (costLedger['customs_broker']?.amountCC || 0) + (costLedger['freight_forwarding']?.amountCC || 0) + (costLedger['inland_transport']?.amountCC || 0) + (costLedger['loading_handling']?.amountCC || 0) + (costLedger['warehouse_origin']?.amountCC || 0) + (costLedger['port_handling']?.amountCC || 0) + (costLedger['export_clearance']?.amountCC || 0)).toLocaleString()}) + Freight (₹${(freightCostCC || 0).toLocaleString()}) + Insurance (₹${(insuranceCostCC || 0).toLocaleString()}) = ₹${totalSellerCostCC.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    category: 'Cost Accounting',
  });
  traceability.push({
    title: 'Exporter Break-Even / Unit',
    formula: `Total Seller Cost (₹${totalSellerCostCC.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}) / ${qty.toLocaleString()} units = ₹${exporterBreakEvenPerUnitCC.toFixed(2)}/unit (≈ ${exporterBreakEvenPerUnitOriginal?.toFixed(2)} ${sellingCurrency}/unit)`,
    category: 'Profitability',
  });
  traceability.push({
    title: 'Exporter Profit (Seller-borne basis)',
    formula: `Total Revenue (₹${(totalRevenueCC || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}) - Total Seller Cost (₹${totalSellerCostCC.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}) = ₹${(exporterProfitCC || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (≈ ${(exporterProfitOriginal || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${sellingCurrency})`,
    category: 'Profitability',
  });
  traceability.push({
    title: 'Exporter Profit Margin %',
    formula: `(Exporter Profit ₹${(exporterProfitCC || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} / Total Revenue ₹${(totalRevenueCC || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}) × 100 = ${(exporterMarginPct || 0).toFixed(2)}%`,
    category: 'Profitability',
  });
  traceability.push({
    title: 'Exporter Profit Markup %',
    formula: `(Exporter Profit ₹${(exporterProfitCC || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} / Total Seller Cost ₹${totalSellerCostCC.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}) × 100 = ${(exporterMarkupPct || 0).toFixed(2)}%`,
    category: 'Profitability',
  });
  traceability.push({
    title: `Target Selling Price (${targetMarginPct}% Target Margin)`,
    formula: `Seller Cost/Unit (₹${sellerCostPerUnitCC.toFixed(2)}) / (1 - ${targetMarginPct}/100) = ₹${targetPricePerUnitCC.toFixed(2)}/unit (≈ ${targetPricePerUnitOriginal?.toFixed(2)} ${sellingCurrency}/unit)`,
    category: 'Pricing Strategy',
  });
  traceability.push({
    title: 'Customs Valuation Basis',
    formula: `Destination CIF Transaction Value: ${invoiceValueOriginal?.toLocaleString() || 0} ${sellingCurrency} (≈ ₹${(invoiceValueCC || 0).toLocaleString()})`,
    category: 'Customs & Valuation',
  });
  traceability.push({
    title: 'Import VAT Taxable Base',
    formula: `Customs Value (${customsValueOriginal?.toLocaleString() || 0} ${sellingCurrency}) + Customs Duty (${dutyCostOriginal || 0} ${sellingCurrency}) = ${vatTaxableBaseOriginal.toLocaleString()} ${sellingCurrency} (≈ ₹${vatTaxableBaseCC.toLocaleString()})`,
    category: 'Duties & Taxes',
  });
  traceability.push({
    title: 'Import VAT Amount',
    formula: `Taxable Base (${vatTaxableBaseOriginal.toLocaleString()} ${sellingCurrency}) × ${taxRate ?? 0}% = ${(taxCostOriginal || 0).toLocaleString()} ${sellingCurrency} (≈ ₹${(taxCostCC || 0).toLocaleString()})`,
    category: 'Duties & Taxes',
  });
  traceability.push({
    title: 'Total Buyer Landed Cost',
    formula: `CIF Invoice (${invoiceValueOriginal?.toLocaleString() || 0} ${sellingCurrency}) + Customs Duty (${dutyCostOriginal || 0} ${sellingCurrency}) + Import VAT (${(taxCostOriginal || 0).toLocaleString()} ${sellingCurrency}) + Destination Handling (${destinationChargesOriginal.toFixed(2)} ${sellingCurrency}) = ${buyerLandedCostOriginal?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || 0} ${sellingCurrency} (≈ ₹${buyerLandedCostCC?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || 0} · ₹${landedCostPerUnitCC?.toFixed(2)}/unit)`,
    category: 'Buyer Landed Cost',
  });

  // ── 16. Full Sensitivity Analysis (Distinguishing Exporter vs Buyer) ───────────
  const sensitivityItems = [];
  if (!skipDerivedAnalysis && totalRevenueCC != null && totalSellerCostCC > 0) {
    const baseProfit = exporterProfitCC;

    // A. Freight ±10%
    if (freightCostCC !== MISSING && freightCostCC > 0) {
      const fUp = freightCostCC * 1.10;
      const sCostUp = totalSellerCostCC + (incotermRules.sellerPaysFreight ? (fUp - freightCostCC) : 0);
      const profitFUp = totalRevenueCC - sCostUp;
      sensitivityItems.push({
        variable: 'Freight Cost',
        change: '+10%',
        newValue: `₹${fUp.toLocaleString()}`,
        exporterProfitImpactCC: profitFUp - baseProfit,
        buyerLandedCostImpactCC: incotermRules.sellerPaysFreight ? 0 : (fUp - freightCostCC),
        newExporterProfitCC: profitFUp,
        // Aliases matching the UI table's expected field names (Variable / Shift /
        // New Profit / Impact) — the UI previously read `newProfitCC`/`impactCC`,
        // which never existed on this object, so every row rendered "—".
        newProfitCC: profitFUp,
        impactCC: profitFUp - baseProfit,
        newMarginPct: totalRevenueCC > 0 ? (profitFUp / totalRevenueCC) * 100 : 0,
        type: 'freight',
      });

      const fDown = freightCostCC * 0.90;
      const sCostDown = totalSellerCostCC + (incotermRules.sellerPaysFreight ? (fDown - freightCostCC) : 0);
      const profitFDown = totalRevenueCC - sCostDown;
      sensitivityItems.push({
        variable: 'Freight Cost',
        change: '-10%',
        newValue: `₹${fDown.toLocaleString()}`,
        exporterProfitImpactCC: profitFDown - baseProfit,
        buyerLandedCostImpactCC: incotermRules.sellerPaysFreight ? 0 : (fDown - freightCostCC),
        newExporterProfitCC: profitFDown,
        newProfitCC: profitFDown,
        impactCC: profitFDown - baseProfit,
        newMarginPct: totalRevenueCC > 0 ? (profitFDown / totalRevenueCC) * 100 : 0,
        type: 'freight',
      });
    }

    // B. Selling Price ±5%
    if (sellingPricePerUnitOriginal != null) {
      const spUpOrig = sellingPricePerUnitOriginal * 1.05;
      const revUpCC = spUpOrig * qty * fxRateSellingToCC;
      const profitSpUp = revUpCC - totalSellerCostCC;
      sensitivityItems.push({
        variable: 'Selling Price',
        change: '+5%',
        newValue: `${spUpOrig.toFixed(2)} ${sellingCurrency} (≈ ₹${(spUpOrig * fxRateSellingToCC).toFixed(2)})`,
        exporterProfitImpactCC: profitSpUp - baseProfit,
        buyerLandedCostImpactCC: (revUpCC - totalRevenueCC) * (1 + (taxRate || 0) / 100),
        newExporterProfitCC: profitSpUp,
        newProfitCC: profitSpUp,
        impactCC: profitSpUp - baseProfit,
        newMarginPct: revUpCC > 0 ? (profitSpUp / revUpCC) * 100 : 0,
        type: 'price',
      });

      const spDownOrig = sellingPricePerUnitOriginal * 0.95;
      const revDownCC = spDownOrig * qty * fxRateSellingToCC;
      const profitSpDown = revDownCC - totalSellerCostCC;
      sensitivityItems.push({
        variable: 'Selling Price',
        change: '-5%',
        newValue: `${spDownOrig.toFixed(2)} ${sellingCurrency} (≈ ₹${(spDownOrig * fxRateSellingToCC).toFixed(2)})`,
        exporterProfitImpactCC: profitSpDown - baseProfit,
        buyerLandedCostImpactCC: (revDownCC - totalRevenueCC) * (1 + (taxRate || 0) / 100),
        newExporterProfitCC: profitSpDown,
        newProfitCC: profitSpDown,
        impactCC: profitSpDown - baseProfit,
        newMarginPct: revDownCC > 0 ? (profitSpDown / revDownCC) * 100 : 0,
        type: 'price',
      });
    }

    // C. Customs Duty Shift (0%, 5%, 10%, 20% Absolute percentage points)
    [0, 5, 10, 20].forEach(dShiftRate => {
      const dShiftDutyOriginal = (customsValueOriginal || 0) * (dShiftRate / 100);
      const dShiftDutyCC = dShiftDutyOriginal * fxRateSellingToCC;
      const dShiftVatCC = ((customsValueCC || 0) + dShiftDutyCC) * ((taxRate || 0) / 100);
      const newLandedCostCC = (invoiceValueCC || 0) + dShiftDutyCC + dShiftVatCC + (incotermRules.sellerPaysDestination ? 0 : destinationChargesCC);

      const dShiftSellerCost = totalSellerCostCC + (incotermRules.sellerPaysDuty ? (dShiftDutyCC - (dutyCostCC || 0)) : 0);
      const profitDutyShift = totalRevenueCC - dShiftSellerCost;

      const dutyImpactCC = incotermRules.sellerPaysDuty ? (profitDutyShift - baseProfit) : 0;
      sensitivityItems.push({
        variable: `Customs Duty @ ${dShiftRate}%`,
        change: `${dShiftRate}% abs rate`,
        newValue: `${dShiftRate}% rate (${dShiftDutyOriginal.toLocaleString()} ${sellingCurrency})`,
        exporterProfitImpactCC: dutyImpactCC,
        buyerLandedCostImpactCC: incotermRules.sellerPaysDuty ? 0 : (newLandedCostCC - (buyerLandedCostCC || 0)),
        newExporterProfitCC: profitDutyShift,
        newProfitCC: profitDutyShift,
        impactCC: dutyImpactCC,
        newMarginPct: totalRevenueCC > 0 ? (profitDutyShift / totalRevenueCC) * 100 : 0,
        type: 'duty',
      });
    });
  }

  // ── 17. Complete Scenario Analysis ───────────────────────────────────────────
  //
  // SINGLE SOURCE OF TRUTH: each scenario is produced by cloning the base transaction,
  // changing ONLY the freight assumption, and running it back through THIS SAME engine
  // (calculateTradeEconomics). There is no second profit formula anywhere — every
  // scenario figure comes from the identical calculation path as the main report, so
  // margin/break-even/profit are always internally consistent with the scenario's own
  // revenue and seller cost. The `skipDerivedAnalysis` flag prevents infinite recursion.
  //
  // The rawFreight above is expressed in `freightCurrency`; scaling that raw input by a
  // multiplier and re-running the engine reproduces the exact freight→insurance→seller-cost
  // chain, so freight-driven insurance changes are captured correctly (not approximated).
  const scenarios = [];
  if (!skipDerivedAnalysis && totalRevenueCC != null && totalSellerCostCC > 0) {
    const runFreightScenario = (freightMultiplier) => {
      const clonedTxn = {
        ...transaction,
        // Only the freight assumption changes. If the base had no numeric freight we
        // leave it untouched so the scenario degrades gracefully to the base result.
        freightCost: (!isNaN(rawFreight) && rawFreight > 0)
          ? rawFreight * freightMultiplier
          : transaction.freightCost,
      };
      return calculateTradeEconomics(clonedTxn, fxRates, dutyData, taxData, { skipDerivedAnalysis: true });
    };

    const pushScenario = (name, multiplier, assumptions) => {
      const res = runFreightScenario(multiplier);
      scenarios.push({
        name,
        assumptions,
        freightMultiplier: multiplier,
        // All values read straight from the sub-result of the same engine.
        sellerCostCC: res.sellerCost.amountCC,
        revenueCC: res.revenue.totalRevenueCC,
        profitCC: res.exporterProfit.amountCC,
        marginPct: res.exporterProfit.marginPct,
        breakEvenPerUnitCC: res.exporterBreakEven.exporterBreakEvenPerUnitCC,
        breakEvenOriginal: res.exporterBreakEven.exporterBreakEvenPerUnitOriginal,
      });
    };

    // 1. Conservative — freight rises 15% (a cost headwind), all else equal.
    pushScenario('Conservative', 1.15, ['Freight +15% (cost headwind)', 'All other inputs unchanged']);

    // 2. Expected (Current) — the base result, unchanged. Uses the already-computed
    //    figures directly (multiplier 1.0 would reproduce them identically).
    scenarios.push({
      name: 'Expected (Current)',
      assumptions: ['Current inputs & verified rates'],
      freightMultiplier: 1.0,
      sellerCostCC: totalSellerCostCC,
      revenueCC: totalRevenueCC,
      profitCC: exporterProfitCC,
      marginPct: exporterMarginPct,
      breakEvenPerUnitCC: exporterBreakEvenPerUnitCC,
      breakEvenOriginal: exporterBreakEvenPerUnitOriginal,
    });

    // 3. Optimistic — freight falls 10% (a cost saving), all else equal.
    pushScenario('Optimistic', 0.90, ['Freight -10% (cost saving)', 'All other inputs unchanged']);
  }

  // ── 18. Quantity Economics (Shipment Logistics Rules) ────────────────────────
  //
  // SINGLE SOURCE OF TRUTH RULE: this table must reconcile exactly with the main
  // calculation above at q === qty. Freight for other tiers scales by the SAME
  // container-rounding rule the freight module itself would use (a container /
  // consignment cost step-function is a legitimate real-world behaviour — an extra
  // 20ft container is only booked once a shipment crosses ~20,000kg), but the
  // scaling factor is applied against the ACTUAL verified freightCostCC/unitWeight
  // that produced the main result, not an independent re-derivation. At q === qty
  // the scaling factor is always exactly 1, so the two figures are identical by
  // construction — this directly fixes the "Seller Cost ₹301,175 vs ₹25,39,250"
  // divergence, which was caused by this table quietly recomputing freight (and
  // therefore insurance and seller cost) using a different rule than the figure
  // shown above it.
  const qtyTiers = [100, 500, 1000, 5000, 10000];
  if (!qtyTiers.includes(qty)) qtyTiers.push(qty);
  qtyTiers.sort((a, b) => a - b);

  // Container step-count for the BASE (current) shipment — this is the reference
  // point every other tier scales against, so the base tier's own multiple is
  // always 1 by definition and never re-derives a different freight figure.
  const baseContainerSteps = (freightMode === 'Sea' && totalWeightKg > 0)
    ? Math.max(1, Math.ceil(totalWeightKg / 20000))
    : 1;
  const freightPerContainerStepCC = (freightCostCC && freightCostCC > 0)
    ? freightCostCC / baseContainerSteps
    : (freightCostCC || 0);
  // Air freight scales linearly by weight from the verified per-kg rate implied
  // by the actual entered freight cost (never an assumed constant).
  const verifiedAirRatePerKgCC = (freightMode === 'Air' && freightCostCC && totalWeightKg > 0)
    ? freightCostCC / totalWeightKg
    : null;

  const quantityEconomics = qtyTiers.map(q => {
    const isCurrentTier = q === qty;
    const qWeightKg = q * unitWeight;
    const qMfg = unitMfgCost * q;
    const qPack = packPerUnit * q;
    const qLabel = labelPerUnit * q;
    const qQuality = qualityTotal; // fixed per shipment, not per unit
    const qOriginDocs = exportDocs + customsBroker + fwdFee + inlandTransport + loadingCharges + warehouseOrigin + portHandling + exportClearance;

    // Freight: at the current/base quantity this MUST equal freightCostCC exactly
    // (no recomputation at all), so the two reports can never disagree for the
    // scenario the user actually entered. Other tiers scale from the same
    // verified base using a consistent step-function / linear rule.
    let qFreight;
    if (isCurrentTier) {
      qFreight = freightCostCC || 0;
    } else if (freightMode === 'Sea') {
      const steps = qWeightKg > 0 ? Math.max(1, Math.ceil(qWeightKg / 20000)) : baseContainerSteps;
      qFreight = freightPerContainerStepCC * steps;
    } else if (freightMode === 'Air' && verifiedAirRatePerKgCC != null) {
      qFreight = verifiedAirRatePerKgCC * qWeightKg;
    } else {
      qFreight = freightCostCC || 0;
    }

    const qInsuredVal = qMfg + qFreight;
    const qInsurance = isCurrentTier
      ? (insuranceCostCC || 0)
      : (rawInsuranceRate > 0 ? qInsuredVal * (rawInsuranceRate / 100) : (insuranceCostCC || 0));

    let qSellerCost = qMfg + qPack + qLabel + qQuality;
    if (sellerBearsOrigin) qSellerCost += qOriginDocs;
    if (incotermRules.sellerPaysFreight) qSellerCost += qFreight;
    if (incotermRules.sellerPaysInsurance) qSellerCost += qInsurance;

    // At the current quantity, use the main calculation's totals verbatim so the
    // two sections can never diverge, even by floating-point rounding.
    if (isCurrentTier) {
      qSellerCost = totalSellerCostCC;
    }

    const qRevenue = isCurrentTier ? (totalRevenueCC ?? (sellingPricePerUnitCC || 0) * q) : (sellingPricePerUnitCC || 0) * q;
    const qProfit = qRevenue - qSellerCost;
    const qMargin = qRevenue > 0 ? (qProfit / qRevenue) * 100 : 0;
    const qBreakEvenCC = qSellerCost / q;
    const qBreakEvenOrig = fxRateSellingToCC > 0 ? qBreakEvenCC / fxRateSellingToCC : 0;

    const qInvoiceOrig = (sellingPricePerUnitOriginal || 0) * q;
    const qDutyOrig = qInvoiceOrig * ((dutyRate || 0) / 100);
    const qVatOrig = (qInvoiceOrig + qDutyOrig) * ((taxRate || 0) / 100);
    const qLandedOrig = qInvoiceOrig + qDutyOrig + qVatOrig + destinationChargesOriginal;

    return {
      quantity: q,
      totalWeightKg: qWeightKg,
      sellerCostCC: qSellerCost,
      costPerUnitCC: qBreakEvenCC,
      costPerUnitOriginal: qBreakEvenOrig,
      revenueCC: qRevenue,
      profitCC: qProfit,
      marginPct: qMargin,
      buyerLandedCostOriginal: qLandedOrig,
      buyerLandedCostCC: qLandedOrig * fxRateSellingToCC,
      isCurrent: isCurrentTier,
    };
  });

  // ── 19. Cost Breakdown Groups ────────────────────────────────────────────────
  const groupColors = {
    'Manufacturing': 'bg-primary',
    'Origin Logistics': 'bg-amber-600',
    'International Freight': 'bg-amber-500',
    'Insurance': 'bg-emerald-500',
    'Customs Duty': 'bg-rose-500',
    'Import Taxes': 'bg-orange-500',
    'Destination Charges': 'bg-emerald-600',
  };

  const grouped = {};
  Object.values(costLedger).forEach(c => {
    grouped[c.group] = (grouped[c.group] || 0) + c.amountCC;
  });

  const costBreakdown = Object.entries(grouped).map(([group, amt]) => ({
    group,
    amountCC: amt,
    amountPerUnitCC: qty > 0 ? amt / qty : 0,
    shareOfSellerCostPct: totalSellerCostCC > 0 ? (amt / totalSellerCostCC) * 100 : 0,
    shareOfLandedCostPct: (buyerLandedCostCC || totalSellerCostCC) > 0 ? (amt / (buyerLandedCostCC || totalSellerCostCC)) * 100 : 0,
    color: groupColors[group] || 'bg-slate-400',
  }));

  // ── 20. Confidence Scoring Engine ────────────────────────────────────────────
  //
  // Confidence is computed additively from the ACTUAL evidence behind each input, not a
  // hardcoded tier. Each factor contributes a weighted number of points only when the
  // underlying value is genuinely verified against jurisdiction-appropriate source data.
  // User-provided or estimated inputs contribute nothing (they aren't "wrong", but they
  // aren't independently verified either), and a critical missing input caps the score.
  // Every factor — and why it did or didn't earn its points — is recorded in
  // confidenceFactors[] so the UI can explain the number.
  const taxableBaseRuleVerified = taxData?.taxableBaseVerified === true
    || (taxVerified && dutyVerified); // base rule is trustworthy when both its inputs are verified
  const hsClassificationVerified = transaction.hsVerified === true;

  const confidenceFactors = [
    { key: 'fx',            label: 'FX rate verified',            weight: 20, met: fxVerified === true,   evidence: fxVerified ? fxSource : `${sellingCurrency}→${CC} user/inverted rate` },
    { key: 'duty',          label: 'Customs duty verified',       weight: 22, met: dutyVerified === true,  evidence: dutyVerified ? dutySource : 'Duty rate not jurisdiction-verified' },
    { key: 'vat',           label: 'Import VAT/GST verified',     weight: 22, met: taxVerified === true,   evidence: taxVerified ? taxSource : 'VAT rate not jurisdiction-verified' },
    { key: 'hs',            label: 'HS classification verified',  weight: 12, met: hsClassificationVerified, evidence: hsClassificationVerified ? `HS ${transaction.hsCode} confirmed` : 'HS code not independently confirmed' },
    { key: 'taxableBase',   label: 'Taxable-base rule verified',  weight: 12, met: taxableBaseRuleVerified, evidence: taxableBaseRuleVerified ? 'Destination taxable-base rule applied' : 'Taxable-base rule not fully verified' },
    { key: 'freight',       label: 'Freight verified (else user-provided)', weight: 6, met: freightVerified === true, evidence: freightVerified ? freightSource : 'Freight is user-provided' },
    { key: 'insurance',     label: 'Insurance verified (else user-provided)', weight: 6, met: insuranceVerified === true, evidence: insuranceVerified ? insuranceSource : 'Insurance is user-provided' },
  ];

  let confidenceScore = confidenceFactors.reduce((sum, f) => sum + (f.met ? f.weight : 0), 0);
  // A genuinely missing critical input (e.g. no FX rate at all) hard-caps confidence so a
  // few verified secondary inputs can't inflate a fundamentally incomplete calculation.
  if (hasCriticalMissing) confidenceScore = Math.min(confidenceScore, 45);
  confidenceScore = Math.max(0, Math.min(100, Math.round(confidenceScore)));

  const confidenceLevel = confidenceScore >= 85 ? 'High' : confidenceScore >= 60 ? 'Medium' : 'Low';

  const verifiedFactorLabels = confidenceFactors.filter(f => f.met).map(f => f.label.replace(/ verified.*/i, '').trim());
  const unverifiedFactorLabels = confidenceFactors.filter(f => !f.met).map(f => f.label.replace(/ verified.*/i, '').trim());
  const confidenceSummary = confidenceLevel === 'High'
    ? `High Confidence (${confidenceScore}/100) — verified: ${verifiedFactorLabels.join(', ') || 'core rates'}.`
    : confidenceLevel === 'Medium'
    ? `Medium Confidence (${confidenceScore}/100) — verified: ${verifiedFactorLabels.join(', ') || 'some rates'}; not independently verified: ${unverifiedFactorLabels.join(', ') || 'none'}.`
    : `Low Confidence (${confidenceScore}/100) — ${hasCriticalMissing ? 'a critical input is missing. ' : ''}Not verified: ${unverifiedFactorLabels.join(', ') || 'most inputs'}. Review assumptions.`;

  // Merge the factor evidence into the existing confidenceReasons list (used by the
  // Verification & Input Evidence panel) so both share one source of truth.
  confidenceFactors.forEach(f => {
    confidenceReasons.push({
      item: f.label,
      status: f.met ? 'Verified' : (f.key === 'freight' || f.key === 'insurance') ? 'User-provided' : 'Unverified',
      note: f.evidence,
      points: f.met ? f.weight : 0,
      maxPoints: f.weight,
    });
  });

  // ── 20b. Automatic Consistency Validation ────────────────────────────────────
  // Cross-checks every headline figure against the formula that should have
  // produced it, so a future edit that accidentally introduces a second,
  // divergent calculation (the exact class of bug this audit fixed) fails loud
  // in development instead of silently shipping mismatched numbers.
  const EPS = 0.01; // absolute tolerance for floating-point comparisons
  const consistencyIssues = [];
  const approxEqual = (a, b, tol = EPS) => a != null && b != null && !isNaN(a) && !isNaN(b) && Math.abs(a - b) <= tol;

  const checkWeight = () => {
    const expected = qty * unitWeight;
    if (!approxEqual(totalWeightKg, expected, 0.001)) {
      consistencyIssues.push(`checkWeight: totalWeightKg (${totalWeightKg}) !== quantity × unitWeightKg (${expected})`);
    }
  };
  const checkSellerCost = () => {
    const expected = Object.values(costLedger).filter(c => c.sellerBears).reduce((s, c) => s + c.amountCC, 0);
    if (!approxEqual(totalSellerCostCC, expected, 1)) {
      consistencyIssues.push(`checkSellerCost: totalSellerCostCC (${totalSellerCostCC}) !== sum of seller-borne ledger entries (${expected})`);
    }
  };
  const checkRevenue = () => {
    if (totalRevenueCC != null) {
      const expected = (sellingPricePerUnitCC || 0) * qty;
      if (!approxEqual(totalRevenueCC, expected, 1)) {
        consistencyIssues.push(`checkRevenue: totalRevenueCC (${totalRevenueCC}) !== sellingPricePerUnitCC × qty (${expected})`);
      }
    }
  };
  const checkProfit = () => {
    if (exporterProfitCC != null && totalRevenueCC != null) {
      const expected = totalRevenueCC - totalSellerCostCC;
      if (!approxEqual(exporterProfitCC, expected, 1)) {
        consistencyIssues.push(`checkProfit: exporterProfitCC (${exporterProfitCC}) !== revenue - sellerCost (${expected})`);
      }
    }
  };
  const checkBreakEven = () => {
    const expected = qty > 0 ? totalSellerCostCC / qty : 0;
    if (!approxEqual(exporterBreakEvenPerUnitCC, expected, 0.01)) {
      consistencyIssues.push(`checkBreakEven: exporterBreakEvenPerUnitCC (${exporterBreakEvenPerUnitCC}) !== sellerCost / qty (${expected})`);
    }
  };
  const checkTax = () => {
    if (taxCostCC !== MISSING && taxRate != null) {
      const expected = vatTaxableBaseCC * (taxRate / 100);
      if (!approxEqual(taxCostCC, expected, 1)) {
        consistencyIssues.push(`checkTax: taxCostCC (${taxCostCC}) !== taxableBase × rate (${expected})`);
      }
    }
  };
  const checkLandedCost = () => {
    if (buyerLandedCostCC != null) {
      const expected = (invoiceValueCC || 0)
        + (dutyCostCC !== MISSING ? dutyCostCC : 0)
        + antiDumping
        + (taxCostCC !== MISSING ? taxCostCC : 0)
        + (incotermRules.sellerPaysDestination ? 0 : destinationChargesCC);
      if (!approxEqual(buyerLandedCostCC, expected, 1)) {
        consistencyIssues.push(`checkLandedCost: buyerLandedCostCC (${buyerLandedCostCC}) !== customsValue + duty + tax + destinationCharges (${expected})`);
      }
    }
  };
  const checkScenarioConsistency = () => {
    const currentTierScenario = quantityEconomics.find(q => q.isCurrent);
    if (currentTierScenario && !approxEqual(currentTierScenario.sellerCostCC, totalSellerCostCC, 1)) {
      consistencyIssues.push(`checkScenarioConsistency: Quantity Economics seller cost at current quantity (${currentTierScenario.sellerCostCC}) !== main sellerCost (${totalSellerCostCC})`);
    }
    const expectedCurrentScenario = scenarios.find(s => s.name === 'Expected (Current)');
    if (expectedCurrentScenario && !approxEqual(expectedCurrentScenario.sellerCostCC, totalSellerCostCC, 1)) {
      consistencyIssues.push(`checkScenarioConsistency: 'Expected (Current)' scenario sellerCost (${expectedCurrentScenario.sellerCostCC}) !== main sellerCost (${totalSellerCostCC})`);
    }
  };
  const checkSensitivityConsistency = () => {
    sensitivityItems.forEach(s => {
      const newProfit = s.newProfitCC ?? s.newExporterProfitCC;
      const impact = s.impactCC ?? s.exporterProfitImpactCC;
      if (newProfit != null && exporterProfitCC != null && !approxEqual(newProfit - exporterProfitCC, impact, 1)) {
        consistencyIssues.push(`checkSensitivityConsistency: "${s.variable} ${s.change}" impact (${impact}) !== newProfit - baseProfit (${newProfit - exporterProfitCC})`);
      }
    });
  };
  const checkSourceJurisdiction = () => {
    if (dutyVerified && !isSourceJurisdictionMatched(dutySource, transaction.destination)) {
      consistencyIssues.push(`checkSourceJurisdiction: dutySource "${dutySource}" marked Verified but not matched to destination "${transaction.destination}"`);
    }
    if (taxVerified && !isSourceJurisdictionMatched(taxSource, transaction.destination)) {
      consistencyIssues.push(`checkSourceJurisdiction: taxSource "${taxSource}" marked Verified but not matched to destination "${transaction.destination}"`);
    }
  };

  checkWeight();
  checkSellerCost();
  checkRevenue();
  checkProfit();
  checkBreakEven();
  checkTax();
  checkLandedCost();
  checkScenarioConsistency();
  checkSensitivityConsistency();
  checkSourceJurisdiction();

  if (consistencyIssues.length > 0 && typeof import.meta !== 'undefined' && import.meta.env?.DEV) {
    // eslint-disable-next-line no-console
    console.warn('[costEngine] Consistency validation found mismatches:', consistencyIssues);
  }

  // ── 21. Master Structured Result ─────────────────────────────────────────────
  return {
    transaction: {
      product: transaction.productName || 'Premium Basmati Rice',
      category: transaction.productCategory || 'Food Products',
      origin: transaction.origin || 'India',
      destination: transaction.destination || 'Saudi Arabia',
      hsCode: transaction.hsCode || '1006309059',
      hs6: transaction.hsCode ? transaction.hsCode.substring(0, 6) : '100630',
      quantity: qty,
      unitWeight,
      totalWeightKg,
      incoterm: incotermKey,
      incotermDescription: incotermRules.description,
    },
    currencies: {
      calculationCurrency: CC,
      sellingCurrency,
      exchangeRate: fxRateSellingToCC,
      exchangeRateSource: fxSource,
      exchangeRateVerified: fxVerified,
    },
    currency: {
      calculationCurrency: CC,
      sellingCurrency,
      exchangeRate: fxRateSellingToCC,
      exchangeRateSource: fxSource,
    },
    productCost: {
      amountCC: productCostCC,
      amountPerUnitCC: qty > 0 ? productCostCC / qty : 0,
      unitMfgCost,
      packagingPerUnit: packPerUnit,
      labelingPerUnit: labelPerUnit,
      qualityComplianceTotal: qualityTotal,
    },
    sellerExportCost: {
      amountCC: totalSellerCostCC,
      amountPerUnitCC: sellerCostPerUnitCC,
      amountOriginal: fxRateSellingToCC > 0 ? totalSellerCostCC / fxRateSellingToCC : 0,
      amountPerUnitOriginal: exporterBreakEvenPerUnitOriginal,
    },
    sellerCost: {
      amountCC: totalSellerCostCC,
      amountPerUnitCC: sellerCostPerUnitCC,
      amountOriginal: fxRateSellingToCC > 0 ? totalSellerCostCC / fxRateSellingToCC : 0,
      amountPerUnitOriginal: exporterBreakEvenPerUnitOriginal,
    },
    invoiceValue: {
      amountOriginal: invoiceValueOriginal,
      amountCC: invoiceValueCC,
      sellingPricePerUnitOriginal,
      sellingPricePerUnitCC,
      currency: sellingCurrency,
    },
    revenue: {
      sellingPricePerUnit: sellingPricePerUnitOriginal,
      sellingPricePerUnitCC,
      sellingCurrency,
      totalRevenueOriginal,
      totalRevenueCC,
      quantity: qty,
    },
    customsValue: {
      basis: customsValuationBasis,
      amountOriginal: customsValueOriginal,
      amountCC: customsValueCC,
      currency: sellingCurrency,
    },
    customsDuty: {
      dutyRate,
      dutyAmountCC: dutyCostCC,
      dutyAmountOriginal: dutyCostOriginal,
      dutyBasis: `${sellingCurrency} ${customsValueOriginal?.toLocaleString() || 0} × ${dutyRate ?? 0}%`,
      dutySource,
      dutySourceJurisdiction: transaction.destination || null,
      dutyVerificationStatus: dutyVerified ? 'Verified' : 'Unverified',
      dutyVerified,
      antiDumpingDuty: antiDumping,
    },
    duties: {
      hsCode: transaction.hsCode || '',
      hs6: transaction.hsCode ? transaction.hsCode.substring(0, 6) : '',
      dutyRate,
      dutyAmountCC: dutyCostCC,
      dutyAmountOriginal: dutyCostOriginal,
      dutyBasis: `${sellingCurrency} ${customsValueOriginal?.toLocaleString() || 0} × ${dutyRate ?? 0}%`,
      dutySource,
      dutySourceJurisdiction: transaction.destination || null,
      dutyVerificationStatus: dutyVerified ? 'Verified' : 'Unverified',
      dutyVerified,
      antiDumpingDuty: antiDumping,
    },
    importTax: {
      vatRate: taxRate,
      taxableBaseOriginal: vatTaxableBaseOriginal,
      taxableBaseCC: vatTaxableBaseCC,
      taxableBaseComponents: taxBaseComponents,
      taxableBaseRuleLabel,
      vatAmountOriginal: taxCostOriginal,
      vatAmountCC: taxCostCC,
      taxSource,
      taxSourceJurisdiction: transaction.destination || null,
      taxVerificationStatus: taxVerified ? 'Verified' : 'Unverified',
      taxVerified,
    },
    taxes: {
      vatRate: taxRate,
      taxableBaseOriginal: vatTaxableBaseOriginal,
      taxableBaseCC: vatTaxableBaseCC,
      taxableBaseComponents: taxBaseComponents,
      taxableBaseRuleLabel,
      vatAmountOriginal: taxCostOriginal,
      vatAmountCC: taxCostCC,
      taxSource,
      taxSourceJurisdiction: transaction.destination || null,
      taxVerificationStatus: taxVerified ? 'Verified' : 'Unverified',
      taxVerified,
    },
    destinationCosts: {
      amountCC: destinationChargesCC,
      amountOriginal: destinationChargesOriginal,
      portHandling: destPort,
      customsClearance: destCustoms,
      importDocs: destDocs,
      localTransport: destTransport,
      warehousing: destWh,
      delivery: destDelivery,
      other: destOther,
    },
    buyerLandedCost: {
      amountOriginal: buyerLandedCostOriginal,
      amountCC: buyerLandedCostCC,
      amountPerUnitOriginal: landedCostPerUnitOriginal,
      amountPerUnitCC: landedCostPerUnitCC,
    },
    landedCost: {
      amountOriginal: buyerLandedCostOriginal,
      amountCC: buyerLandedCostCC,
      amountPerUnitOriginal: landedCostPerUnitOriginal,
      amountPerUnitCC: landedCostPerUnitCC,
    },
    buyerCost: {
      amountCC: (buyerLandedCostCC != null && invoiceValueCC != null) ? (buyerLandedCostCC - invoiceValueCC) : 0,
      amountOriginal: (buyerLandedCostOriginal != null && invoiceValueOriginal != null) ? (buyerLandedCostOriginal - invoiceValueOriginal) : 0,
    },
    exporterProfit: {
      amountCC: exporterProfitCC,
      amountOriginal: exporterProfitOriginal,
      profitPerUnitCC: exporterProfitPerUnitCC,
      profitPerUnitOriginal: exporterProfitPerUnitOriginal,
      marginPct: exporterMarginPct,
      markupPct: exporterMarkupPct,
      isProfitable: exporterProfitCC != null && exporterProfitCC > 0,
    },
    exporterMargin: exporterMarginPct,
    exporterMarkup: exporterMarkupPct,
    exporterBreakEven: {
      exporterBreakEvenPerUnitCC,
      exporterBreakEvenPerUnitOriginal,
      sellingPricePerUnitCC,
      sellingPricePerUnitOriginal,
      isAboveExporterBreakEven,
      statusText: isAboveExporterBreakEven
        ? `Your selling price of ${sellingPricePerUnitOriginal?.toLocaleString()} ${sellingCurrency}/unit (≈ ₹${(sellingPricePerUnitCC || 0).toFixed(2)}/unit) is ABOVE the exporter break-even of ${exporterBreakEvenPerUnitOriginal?.toFixed(2)} ${sellingCurrency}/unit (₹${exporterBreakEvenPerUnitCC.toFixed(2)}/unit).`
        : `Your selling price of ${sellingPricePerUnitOriginal?.toLocaleString()} ${sellingCurrency}/unit is BELOW the exporter break-even of ${exporterBreakEvenPerUnitOriginal?.toFixed(2)} ${sellingCurrency}/unit.`,
    },
    breakEven: {
      exporterBreakEvenPerUnitCC,
      exporterBreakEvenPerUnitOriginal,
      landedCostPerUnitCC,
      landedCostPerUnitOriginal,
      sellingPricePerUnitCC,
      sellingPricePerUnitOriginal,
      isAboveExporterBreakEven,
      isAboveLandedCost,
      exporterBreakEvenStatusText: isAboveExporterBreakEven
        ? `Your selling price of ${sellingPricePerUnitOriginal?.toLocaleString()} ${sellingCurrency}/unit (≈ ₹${(sellingPricePerUnitCC || 0).toFixed(2)}/unit) is ABOVE the exporter break-even of ${exporterBreakEvenPerUnitOriginal?.toFixed(2)} ${sellingCurrency}/unit (₹${exporterBreakEvenPerUnitCC.toFixed(2)}/unit).`
        : `Your selling price of ${sellingPricePerUnitOriginal?.toLocaleString()} ${sellingCurrency}/unit is BELOW the exporter break-even of ${exporterBreakEvenPerUnitOriginal?.toFixed(2)} ${sellingCurrency}/unit.`,
    },
    targetSellingPrice: {
      targetMarginPct,
      targetPricePerUnitCC,
      targetPricePerUnitOriginal,
      targetRevenueCC,
      formula: 'sellerExportCostPerUnit / (1 - targetMargin / 100)',
    },
    freight: {
      mode: freightMode,
      amountCC: freightCostCC,
      currency: freightCurrency,
      basis: transaction.freightBasis || 'Per shipment',
      source: freightSource,
      verified: freightVerified,
    },
    insurance: {
      ratePct: rawInsuranceRate,
      insuredValueCC: productAndFreightCC,
      amountCC: insuranceCostCC,
      source: insuranceSource,
      verified: insuranceVerified,
    },
    costLedger,
    costBreakdown,
    scenarios,
    sensitivities: sensitivityItems,
    sensitivity: sensitivityItems,
    quantityEconomics,
    qtyEconomics: quantityEconomics,
    assumptions,
    confidence: {
      level: confidenceLevel,
      score: confidenceScore,
      reasons: confidenceReasons,
      factors: confidenceFactors,
      summary: confidenceSummary,
    },
    traceability,
    warnings,
    verification: {
      passed: consistencyIssues.length === 0,
      issues: consistencyIssues,
      dutySourceJurisdictionMatched: isSourceJurisdictionMatched(dutySource, transaction.destination),
      taxSourceJurisdictionMatched: isSourceJurisdictionMatched(taxSource, transaction.destination),
    },
    costs: {
      totalSellerCost: totalSellerCostCC,
      totalLandedCost: buyerLandedCostCC || totalSellerCostCC,
      totalExportCost: totalSellerCostCC,
      totalProductCost: productCostCC,
      customsValue: customsValueCC,
      dutyRate,
      dutyVerified,
      taxRate,
      taxVerified,
      ledger: costLedger,
      grouped,
    },
    profitability: {
      profit: exporterProfitCC,
      profitPerUnit: exporterProfitPerUnitCC,
      margin: exporterMarginPct,
      markup: exporterMarkupPct,
      breakEvenPrice: exporterBreakEvenPerUnitCC,
      costPerUnit: sellerCostPerUnitCC,
      targetSellingPrice: targetPricePerUnitCC,
      targetMarginPct,
      isProfitable: exporterProfitCC != null && exporterProfitCC > 0,
    },
    calculatedAt: new Date().toISOString(),
  };
}

// Alias for backward compatibility
export const calculateExportCost = calculateTradeEconomics;

// ── Display Formatters ─────────────────────────────────────────────────────────

export function fmt(amount, currency = 'INR', decimals = 2) {
  if (amount == null || amount === MISSING || isNaN(amount)) return '—';
  const n = Number(amount);
  const symbols = { INR: '₹', USD: '$', EUR: '€', GBP: '£', SAR: 'SAR ', AED: 'AED ', SGD: 'S$', AUD: 'A$', JPY: '¥', CNY: '¥' };
  const sym = symbols[currency] || (currency ? `${currency} ` : '');
  return `${sym}${n.toLocaleString('en-IN', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })}`;
}

export function fmtPct(val, decimals = 1) {
  if (val == null || val === MISSING || isNaN(val)) return '—';
  return `${Number(val).toFixed(decimals)}%`;
}
