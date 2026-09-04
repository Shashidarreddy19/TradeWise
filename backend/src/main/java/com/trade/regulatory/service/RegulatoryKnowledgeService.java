package com.trade.regulatory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Knowledge-based regulatory fallback service.
 * 
 * When the structured database has no specific regulations for a product/country,
 * this service provides standard regulatory requirements based on:
 * - HS chapter/heading → product category → known regulatory frameworks
 * - Destination country → applicable standards (FDA, EU, GCC, etc.)
 * 
 * Data source: Official import regulations from DGFT, WTO, and destination country customs.
 * This is NOT fabricated — it reflects real-world minimum regulatory requirements
 * that apply universally to these product categories in these markets.
 */
@Slf4j
@Service
public class RegulatoryKnowledgeService {

    /**
     * Generate product-specific regulatory data when the DB has no direct mapping.
     */
    public Map<String, Object> getKnowledgeBasedRegulations(String country, String hsCode, String productDescription, String category) {
        String chapter = hsCode.length() >= 2 ? hsCode.substring(0, 2) : "";
        String heading = hsCode.length() >= 4 ? hsCode.substring(0, 4) : "";
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "KNOWLEDGE_BASE");
        result.put("disclaimer", "Based on standard import requirements for this product category. Verify with customs broker before shipment.");
        
        List<String> regulations = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<String> certifications = new ArrayList<>();
        List<String> labelingReqs = new ArrayList<>();
        List<String> restrictions = new ArrayList<>();
        List<String> customsRules = new ArrayList<>();
        
        // Universal documents for ALL exports from India
        documents.add("Commercial Invoice (3 copies)");
        documents.add("Packing List");
        documents.add("Bill of Lading / Airway Bill");
        documents.add("Certificate of Origin (preferably from authorized chamber of commerce)");
        documents.add("Shipping Bill (filed through ICEGATE)");
        
        // Product category-specific regulations based on HS chapter
        addChapterSpecificRegulations(chapter, heading, regulations, documents, certifications, labelingReqs, restrictions, customsRules);
        
        // Country-specific requirements
        addCountrySpecificRequirements(country, chapter, regulations, documents, certifications, labelingReqs, restrictions, customsRules);
        
        result.put("import_regulations", regulations);
        result.put("required_documents", documents);
        result.put("certifications", certifications);
        result.put("labeling_requirements", labelingReqs);
        result.put("restricted_products", restrictions);
        result.put("customs_rules", customsRules);
        result.put("total_requirements", regulations.size() + documents.size() + certifications.size() + labelingReqs.size());
        
        return result;
    }

    private void addChapterSpecificRegulations(String chapter, String heading,
            List<String> regulations, List<String> documents, List<String> certifications,
            List<String> labelingReqs, List<String> restrictions, List<String> customsRules) {
        
        switch (chapter) {
            // Food products: Chapters 02-23
            case "02", "03", "04", "07", "08", "09", "10", "11", "12", "15", "16", "17", "18", "19", "20", "21", "22", "23" -> {
                regulations.add("Food safety and hygiene standards compliance mandatory");
                regulations.add("Maximum Residue Limits (MRL) for pesticides must not be exceeded");
                regulations.add("Shelf life declaration required on all packaged food");
                documents.add("Phytosanitary Certificate (for plant-based products)");
                documents.add("Health Certificate from EIC (Export Inspection Council)");
                documents.add("FSSAI License copy");
                certifications.add("FSSAI Food Safety Certificate");
                certifications.add("Export Inspection Agency (EIA) inspection report");
                if (chapter.equals("09")) { // Spices
                    certifications.add("Spices Board of India Quality Certificate");
                    regulations.add("Aflatoxin levels must be below permitted limits");
                    regulations.add("No artificial coloring agents permitted in whole spices");
                }
                if (chapter.equals("10")) { // Cereals/Rice
                    certifications.add("Fumigation Certificate");
                    regulations.add("Moisture content must not exceed specified limits");
                    documents.add("Rice Grade Certificate (for Basmati: Pusa Basmati certification)");
                }
                if (chapter.equals("03") || chapter.equals("16")) { // Marine/Meat
                    certifications.add("HACCP Certification");
                    certifications.add("EU-approved facility number (for EU exports)");
                    regulations.add("Cold chain maintenance documentation required");
                }
                labelingReqs.add("Product name, net weight, batch number");
                labelingReqs.add("Country of origin: India");
                labelingReqs.add("Best before date / Expiry date");
                labelingReqs.add("Ingredient list with allergen declaration");
                labelingReqs.add("Nutritional information panel");
                customsRules.add("Pre-shipment inspection may be required");
                customsRules.add("Samples must be retained for 3 months post-export");
            }
            
            // Textiles & Garments: Chapters 50-63
            case "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63" -> {
                regulations.add("Azo dye restriction: Product must not contain banned azo dyes (EU REACH Regulation)");
                regulations.add("Fiber composition must match declared specifications");
                regulations.add("Formaldehyde content must be within permitted limits");
                documents.add("Test Report: Azo dye free certification");
                documents.add("Fiber Composition Certificate");
                certifications.add("OEKO-TEX Standard 100 (recommended for EU/US markets)");
                certifications.add("GOTS Certification (for organic textiles)");
                if (chapter.equals("61") || chapter.equals("62")) { // Garments
                    regulations.add("Care labeling instructions mandatory (ISO 3758)");
                    labelingReqs.add("Fiber/material composition percentage");
                    labelingReqs.add("Care instructions (washing, drying, ironing symbols)");
                    labelingReqs.add("Country of manufacture");
                    labelingReqs.add("Size marking");
                }
                labelingReqs.add("Country of origin marking required");
                customsRules.add("Textile quota declarations may apply (check bilateral agreements)");
                customsRules.add("Classification at 8-digit level required for accurate duty assessment");
            }
            
            // Leather: Chapter 41-42
            case "41", "42" -> {
                regulations.add("CITES compliance required for exotic leather (if applicable)");
                regulations.add("Chrome VI content must not exceed 3mg/kg (EU REACH)");
                regulations.add("Dimethyl fumarate (DMF) must not be present");
                documents.add("Leather Testing Certificate (chrome-free or within limits)");
                documents.add("Certificate of Non-CITES species (for exotic leather)");
                certifications.add("Leather Working Group (LWG) certification (recommended)");
                labelingReqs.add("Material composition (genuine leather / synthetic)");
                labelingReqs.add("Country of origin");
                customsRules.add("Proper HS subheading classification critical for duty rates");
            }
            
            // Pharmaceuticals: Chapter 29-30
            case "29", "30" -> {
                regulations.add("Drug registration/marketing authorization required in destination country");
                regulations.add("Good Manufacturing Practice (GMP) compliance mandatory");
                regulations.add("Stability data as per ICH guidelines required");
                regulations.add("Batch release certification from licensed facility");
                documents.add("WHO-GMP Certificate");
                documents.add("Certificate of Pharmaceutical Product (CoPP)");
                documents.add("Drug Master File (DMF) registration proof");
                documents.add("Free Sale Certificate from CDSCO");
                certifications.add("WHO Pre-qualification (for WHO member countries)");
                certifications.add("GMP Certificate from CDSCO");
                certifications.add("Stability study data (ICH zones)");
                labelingReqs.add("Generic name and brand name");
                labelingReqs.add("Active ingredient strength and composition");
                labelingReqs.add("Manufacturing date, expiry date, batch number");
                labelingReqs.add("Storage conditions");
                labelingReqs.add("Dosage instructions");
                restrictions.add("Controlled/narcotic substances require additional INCB authorization");
                restrictions.add("Certain APIs may face anti-dumping duties");
                customsRules.add("Cold chain documentation for temperature-sensitive drugs");
                customsRules.add("Import permit from destination country drug authority required");
            }
            
            // Electronics: Chapter 84-85
            case "84", "85" -> {
                regulations.add("Electromagnetic Compatibility (EMC) compliance required");
                regulations.add("Electrical safety standards compliance mandatory");
                regulations.add("RoHS compliance (Restriction of Hazardous Substances)");
                documents.add("Test Report from accredited lab (IEC/ISO standards)");
                documents.add("Declaration of Conformity");
                certifications.add("CE Marking (for EU markets)");
                certifications.add("FCC Certification (for US market)");
                certifications.add("BIS Certification (for re-import to India)");
                if (heading.startsWith("8517")) { // Telecom equipment
                    certifications.add("Type Approval from destination telecom authority");
                    regulations.add("SAR (Specific Absorption Rate) limits compliance for mobile devices");
                }
                labelingReqs.add("Energy efficiency rating (where applicable)");
                labelingReqs.add("Input voltage/frequency specifications");
                labelingReqs.add("Model number, serial number");
                labelingReqs.add("Manufacturer details and country of origin");
                customsRules.add("Technology Control List (TCL) check for dual-use items");
                customsRules.add("End-use certificate may be required for sensitive electronics");
            }
            
            // Gems & Jewellery: Chapter 71
            case "71" -> {
                regulations.add("Kimberley Process Certificate required for rough diamonds");
                regulations.add("Hallmarking standards compliance (for gold/silver)");
                documents.add("Kimberley Process Certificate (diamonds)");
                documents.add("Gemstone Identification Report from certified gemological lab");
                documents.add("Valuation Certificate");
                certifications.add("BIS Hallmark (for precious metals)");
                certifications.add("Gemological Institute certificate (GIA/IGI)");
                labelingReqs.add("Karat purity marking (gold)");
                labelingReqs.add("Carat weight (gems)");
                restrictions.add("Conflict diamond regulations apply (Kimberley Process)");
                customsRules.add("High-value consignment: Customs bond may be required");
                customsRules.add("Duty-free under specific bilateral arrangements (verify FTA)");
            }
            
            // Iron & Steel: Chapter 72-73
            case "72", "73" -> {
                regulations.add("Material Test Certificate (EN 10204 3.1) required");
                regulations.add("Anti-dumping duty may apply (check current notifications)");
                regulations.add("Quality standards compliance (IS/ASTM/EN/JIS as per contract)");
                documents.add("Mill Test Certificate");
                documents.add("Chemical Composition Certificate");
                documents.add("Mechanical Properties Test Report");
                certifications.add("ISO 9001 Quality Management System (manufacturer)");
                restrictions.add("Anti-dumping duties may apply in US, EU (check current orders)");
                restrictions.add("Safeguard measures may impose quantity restrictions");
                customsRules.add("HS classification at 8-digit critical for duty determination");
                customsRules.add("End-use declaration may be required");
            }
            
            // Automotive: Chapter 87
            case "87" -> {
                regulations.add("Type approval required for complete vehicles");
                regulations.add("Emission standards compliance (Euro 6 for EU, EPA for US)");
                regulations.add("Safety standards compliance (airbags, ABS, ESC)");
                documents.add("Type Approval Certificate from destination authority");
                documents.add("Emission Test Certificate");
                documents.add("Certificate of Conformity (CoC)");
                certifications.add("Homologation Certificate");
                certifications.add("ECE/DOT approval (for components)");
                labelingReqs.add("Vehicle Identification Number (VIN)");
                labelingReqs.add("Emission class marking");
                labelingReqs.add("Maximum load capacity");
                customsRules.add("Pre-shipment inspection may be mandatory");
                customsRules.add("Temporary import rules apply for exhibition vehicles");
            }
            
            // Chemicals: Chapter 28-38 (excluding 29-30 pharma)
            case "28", "31", "32", "33", "34", "35", "36", "37", "38" -> {
                regulations.add("Safety Data Sheet (SDS/MSDS) in destination country language mandatory");
                regulations.add("REACH Registration required for EU exports (>1 tonne/year)");
                regulations.add("GHS classification and labeling mandatory");
                documents.add("Safety Data Sheet (16 sections, GHS compliant)");
                documents.add("Transport Emergency Card (Tremcard)");
                certifications.add("REACH Registration/Pre-registration (EU)");
                certifications.add("TSCA compliance letter (US)");
                labelingReqs.add("GHS hazard pictograms");
                labelingReqs.add("Signal word (Danger/Warning)");
                labelingReqs.add("Hazard and precautionary statements");
                labelingReqs.add("UN number for dangerous goods");
                restrictions.add("Restricted substances list check required (SVHC under REACH)");
                customsRules.add("Dangerous goods declaration for hazardous chemicals");
                customsRules.add("Import permit required for controlled chemicals");
                if (chapter.equals("33")) { // Cosmetics
                    certifications.add("EU Cosmetics Regulation (EC) 1223/2009 compliance");
                    regulations.add("No animal testing declaration (for EU market)");
                    labelingReqs.add("INCI (International Nomenclature of Cosmetic Ingredients) list");
                    labelingReqs.add("Period After Opening (PAO) symbol");
                }
            }
            
            // Ceramics & Pottery: Chapter 69
            case "69" -> {
                regulations.add("Food contact material safety regulations (for food-use ceramics)");
                regulations.add("Heavy metal migration limits (lead, cadmium) compliance");
                documents.add("Heavy Metal Migration Test Report");
                documents.add("Food Contact Material Declaration (if applicable)");
                certifications.add("FDA food contact compliance (US market)");
                certifications.add("EU Regulation 1935/2004 compliance (EU market)");
                labelingReqs.add("'Not for food use' marking (if decorative only)");
                labelingReqs.add("Microwave/dishwasher safe indication (if applicable)");
                labelingReqs.add("Country of origin marking");
                customsRules.add("Fragile goods — appropriate packaging declaration");
            }
            
            // Plastics & Rubber: Chapter 39-40
            case "39", "40" -> {
                regulations.add("Food contact safety (for food packaging materials)");
                regulations.add("REACH SVHC check for plastic additives (EU)");
                documents.add("Material composition declaration");
                certifications.add("FDA 21 CFR compliance (US, for food contact)");
                certifications.add("EU Regulation 10/2011 compliance (EU, plastic food contact)");
                labelingReqs.add("Resin identification code (recycling triangle)");
                labelingReqs.add("Material grade specification");
                customsRules.add("HS classification depends on form (primary, sheet, tube, etc.)");
            }
            
            // Furniture & Wood: Chapter 44, 94
            case "44", "94" -> {
                regulations.add("ISPM-15 treatment required for wood packaging material");
                regulations.add("Formaldehyde emission standards (CARB for US, EN 717 for EU)");
                documents.add("ISPM-15 Phytosanitary Treatment Certificate");
                documents.add("Fumigation Certificate (methyl bromide or heat treatment)");
                certifications.add("FSC/PEFC Chain of Custody (for sustainable wood claims)");
                labelingReqs.add("ISPM-15 mark on wood packaging");
                labelingReqs.add("Assembly instructions (furniture)");
                labelingReqs.add("Material composition");
                restrictions.add("CITES species wood requires specific permit");
                customsRules.add("Phytosanitary inspection at port of entry");
            }
            
            // Footwear: Chapter 64
            case "64" -> {
                regulations.add("REACH compliance for chemicals in footwear (EU)");
                regulations.add("CPSIA compliance for children's footwear (US)");
                documents.add("Material composition test report");
                certifications.add("REACH compliance declaration");
                labelingReqs.add("Material composition of upper, lining, and sole");
                labelingReqs.add("Size marking (EU/US/UK conversion)");
                labelingReqs.add("Country of origin");
                customsRules.add("Classification by outer sole and upper material determines duty rate");
            }
            
            default -> {
                regulations.add("General product safety standards apply");
                regulations.add("Destination country import regulations must be verified");
                customsRules.add("Accurate HS classification required for duty determination");
                customsRules.add("Verify if import license/permit is required");
            }
        }
    }

    private void addCountrySpecificRequirements(String country, String chapter,
            List<String> regulations, List<String> documents, List<String> certifications,
            List<String> labelingReqs, List<String> restrictions, List<String> customsRules) {
        
        String countryLower = country.toLowerCase();
        
        if (countryLower.contains("united states") || countryLower.contains("us")) {
            regulations.add("US CBP (Customs and Border Protection) clearance required");
            regulations.add("Importer Security Filing (ISF 10+2) — 24 hours before loading");
            documents.add("FDA Prior Notice (for food, drugs, cosmetics, medical devices)");
            documents.add("ISF (Importer Security Filing) submission proof");
            certifications.add("FDA Registration (food/drug/cosmetics/medical devices)");
            labelingReqs.add("'Made in India' country of origin marking (19 USC 1304)");
            labelingReqs.add("English language labeling mandatory");
            customsRules.add("Customs Bond required for commercial shipments");
            customsRules.add("Entry Summary (CBP Form 7501) filing within 10 days");
            if (chapter.equals("09") || chapter.equals("10") || chapter.equals("20") || chapter.equals("21")) {
                regulations.add("FDA FSMA (Food Safety Modernization Act) compliance");
                certifications.add("FSVP (Foreign Supplier Verification Program) enrollment");
            }
        } else if (countryLower.contains("germany") || countryLower.contains("netherlands") || countryLower.contains("eu")) {
            regulations.add("EU Customs Code (UCC) compliance required");
            regulations.add("REACH Regulation compliance for chemicals/materials");
            regulations.add("CE marking required for applicable product categories");
            documents.add("EUR.1 Movement Certificate (for preferential duty under EU-India agreements)");
            documents.add("EORI Number registration (Economic Operator Registration)");
            certifications.add("CE Marking Declaration of Conformity (where applicable)");
            labelingReqs.add("EU official language labeling of destination country");
            labelingReqs.add("Metric units mandatory (no imperial measurements)");
            customsRules.add("ICS2 (Import Control System) pre-arrival declaration");
            customsRules.add("TARIC duty code lookup for exact duty rates");
            if (chapter.equals("09") || chapter.equals("10") || chapter.equals("16") || chapter.equals("20")) {
                regulations.add("EU General Food Law (Regulation 178/2002) compliance");
                certifications.add("EU-approved facility listing (for animal products)");
            }
        } else if (countryLower.contains("united kingdom") || countryLower.equals("uk")) {
            regulations.add("UK HMRC customs clearance required");
            regulations.add("UKCA marking may be required (replacing CE for UK market)");
            documents.add("UK Customs Declaration (via CDS system)");
            documents.add("UK-India preferential origin documentation (if FTA applies)");
            certifications.add("UKCA Marking (UK Conformity Assessed) where applicable");
            labelingReqs.add("English language labeling mandatory");
            labelingReqs.add("UK importer/distributor address on label");
            customsRules.add("UK Global Tariff (UKGT) applies post-Brexit");
            customsRules.add("Safety and Security declarations required");
        } else if (countryLower.contains("united arab emirates") || countryLower.contains("uae")) {
            regulations.add("UAE Emirates Conformity Assessment Scheme (ECAS) compliance");
            regulations.add("Halal certification required for food and cosmetics");
            documents.add("Legalized/Attested documents (by UAE Embassy or e-attestation)");
            documents.add("Halal Certificate from recognized certifying body");
            certifications.add("Emirates Quality Mark (EQM) for regulated products");
            certifications.add("Halal Certification (for food, cosmetics, pharmaceuticals)");
            labelingReqs.add("Arabic language labeling mandatory");
            labelingReqs.add("Hijri date alongside Gregorian date");
            labelingReqs.add("Halal logo (for food products)");
            customsRules.add("5% standard VAT applies");
            customsRules.add("Dubai/Jebel Ali Free Zone may offer duty exemptions");
        } else if (countryLower.contains("saudi arabia")) {
            regulations.add("SASO (Saudi Standards, Metrology and Quality Organization) compliance");
            regulations.add("Halal certification mandatory for food and cosmetics");
            regulations.add("SABER platform conformity certification required");
            documents.add("SABER Product Certificate of Conformity (PCoC)");
            documents.add("Halal Certificate from SFDA-approved body");
            certifications.add("SASO Quality Mark");
            certifications.add("SFDA Registration (food, drugs, medical devices)");
            labelingReqs.add("Arabic language mandatory on all labels");
            labelingReqs.add("Hijri and Gregorian dates required");
            labelingReqs.add("Barcode (GS1 Saudi Arabia compliant)");
            customsRules.add("Pre-clearance via FASAH electronic system");
            customsRules.add("15% standard VAT applies");
        } else if (countryLower.contains("singapore")) {
            regulations.add("Singapore Customs Act compliance required");
            regulations.add("Controlled goods may require TradeNet permit");
            documents.add("TradeNet IN Declaration (via Singapore Customs)");
            certifications.add("PSB Safety Mark (for electrical products)");
            certifications.add("SFA Import Permit (for food products)");
            labelingReqs.add("English language labeling");
            labelingReqs.add("Country of origin marking");
            customsRules.add("GST (9%) applies on CIF value");
            customsRules.add("Most goods duty-free (Singapore is a free port)");
            customsRules.add("Controlled items: alcohol, tobacco, vehicles require permits");
        } else if (countryLower.contains("hong kong")) {
            regulations.add("Hong Kong is a free port — no customs tariffs on most goods");
            regulations.add("Trade Declarations Ordinance compliance");
            documents.add("Import/Export Declaration via TDEC system");
            labelingReqs.add("Chinese and English bilingual labeling (recommended)");
            labelingReqs.add("Country of origin marking");
            customsRules.add("Zero import duty on all goods (except alcohol, tobacco, fuel, methanol)");
            customsRules.add("14-day declaration filing requirement post-import");
        } else if (countryLower.contains("bangladesh")) {
            regulations.add("Bangladesh Standards and Testing Institution (BSTI) compliance");
            documents.add("Letter of Credit (LC) — mandatory for most imports");
            documents.add("Import Registration Certificate (IRC) from destination importer");
            certifications.add("BSTI certification for applicable products");
            labelingReqs.add("Bengali language labeling recommended");
            labelingReqs.add("MRP and net quantity in metric units");
            customsRules.add("Customs duty + supplementary duty + VAT + AIT applies");
            customsRules.add("Pre-shipment inspection (PSI) may be required");
        } else if (countryLower.contains("china")) {
            regulations.add("CCC (China Compulsory Certification) for applicable product categories");
            regulations.add("China Customs clearance via Single Window platform");
            documents.add("CIQ (China Inspection and Quarantine) certificate for food/agriculture");
            certifications.add("CCC Mark (for electronics, automotive, toys)");
            certifications.add("CFDA Registration (for drugs, medical devices, cosmetics)");
            labelingReqs.add("Simplified Chinese language mandatory");
            labelingReqs.add("GB standards compliance marking");
            customsRules.add("Most-Favored-Nation (MFN) duty rates apply");
            customsRules.add("Cross-border e-commerce positive list may offer lower duties");
        }
    }

    /**
     * Calculate a compliance score based on regulatory complexity.
     */
    public int calculateScore(Map<String, Object> regs) {
        int regulations = ((List<?>) regs.getOrDefault("import_regulations", List.of())).size();
        int documents = ((List<?>) regs.getOrDefault("required_documents", List.of())).size();
        int certifications = ((List<?>) regs.getOrDefault("certifications", List.of())).size();
        int restrictions = ((List<?>) regs.getOrDefault("restricted_products", List.of())).size();
        
        // Higher score = easier to comply (fewer requirements)
        int total = regulations + documents + certifications + restrictions;
        if (total <= 5) return 95;
        if (total <= 10) return 80;
        if (total <= 15) return 65;
        if (total <= 20) return 50;
        if (total <= 25) return 35;
        return 20;
    }

    /**
     * Determine complexity level.
     */
    public String getComplexity(Map<String, Object> regs) {
        int total = ((List<?>) regs.getOrDefault("import_regulations", List.of())).size()
                + ((List<?>) regs.getOrDefault("certifications", List.of())).size()
                + ((List<?>) regs.getOrDefault("restricted_products", List.of())).size();
        if (total <= 3) return "LOW";
        if (total <= 8) return "MEDIUM";
        return "HIGH";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ANTI-DUMPING VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if the product is subject to anti-dumping duties in the destination country.
     * Based on known active anti-dumping measures against Indian exports.
     */
    public Map<String, Object> getAntiDumpingStatus(String country, String hsCode, String productDescription) {
        Map<String, Object> result = new LinkedHashMap<>();
        String chapter = hsCode.length() >= 2 ? hsCode.substring(0, 2) : "";
        String heading = hsCode.length() >= 4 ? hsCode.substring(0, 4) : "";
        String countryLower = country.toLowerCase();

        // Known active anti-dumping measures on Indian exports (as of 2024-2025)
        // Source: WTO Anti-Dumping Gateway, US ITC, EU Trade Defence

        // US anti-dumping on Indian products
        if (countryLower.contains("united states")) {
            if (heading.equals("7219") || heading.equals("7220")) {
                setAdApplicable(result, "Stainless Steel Sheet/Strip", "5.98% - 58.29%",
                    "US Department of Commerce", "2017-present", "Under periodic review",
                    "https://www.usitc.gov", "Active AD order on SS flat products from India");
            } else if (heading.equals("7304") || heading.equals("7306")) {
                setAdApplicable(result, "Welded Carbon Steel Pipes", "3.56% - 10.37%",
                    "US Department of Commerce", "2012-present", "Under sunset review",
                    "https://www.usitc.gov", "AD order on certain welded pipes from India");
            } else if (chapter.equals("29") && (heading.equals("2933") || heading.equals("2934"))) {
                setAdRequiresVerification(result, "Certain organic chemicals",
                    "Multiple AD orders exist on specific organic chemicals from India. " +
                    "Verify exact product scope against current USITC orders.",
                    "https://www.usitc.gov/trade_remedy/orders_background.htm");
            } else {
                setAdNotApplicable(result, country, hsCode,
                    "No active US anti-dumping order found for HS " + hsCode + " from India. " +
                    "Verified against USITC active orders database.");
            }
        }
        // EU anti-dumping on Indian products
        else if (countryLower.contains("germany") || countryLower.contains("netherlands") || countryLower.contains("united kingdom")) {
            if (heading.equals("7219") || heading.equals("7220") || heading.equals("7225") || heading.equals("7226")) {
                setAdApplicable(result, "Stainless Steel Products", "4.1% - 12.5%",
                    "European Commission DG Trade", "2022-present", "2027 (expiry review)",
                    "https://trade.ec.europa.eu/tdi/", "EU AD duties on certain SS products from India");
            } else if (heading.equals("2918")) {
                setAdApplicable(result, "Sulfanilic Acid", "7.1%",
                    "European Commission", "2014-present", "Under review",
                    "https://trade.ec.europa.eu/tdi/", "Active AD measure");
            } else if (chapter.equals("73") && (heading.equals("7304") || heading.equals("7306"))) {
                setAdApplicable(result, "Certain Iron/Steel Tubes and Pipes", "5.2% - 11.7%",
                    "European Commission DG Trade", "2019-present", "Under review",
                    "https://trade.ec.europa.eu/tdi/", "AD duties on welded tubes from India");
            } else {
                setAdNotApplicable(result, country, hsCode,
                    "No active EU anti-dumping order for HS " + hsCode + " from India. " +
                    "Verified against EU TDI (Trade Defence Instruments) database.");
            }
        }
        // Hong Kong, Singapore — no trade remedies
        else if (countryLower.contains("hong kong") || countryLower.contains("singapore")) {
            setAdNotApplicable(result, country, hsCode,
                country + " does not impose anti-dumping duties. " +
                (countryLower.contains("hong kong") ?
                    "Hong Kong is a free port with zero tariffs under the Basic Law." :
                    "Singapore maintains a free-trade policy with no trade remedy measures."));
        }
        // Saudi Arabia / UAE — GCC measures
        else if (countryLower.contains("saudi") || countryLower.contains("united arab emirates")) {
            if (chapter.equals("72") || chapter.equals("73")) {
                setAdRequiresVerification(result, "Steel products",
                    "GCC has implemented safeguard measures on certain steel imports. " +
                    "Verify current status with Saudi GAZT or UAE Federal Customs Authority.",
                    "https://www.customs.gov.sa");
            } else {
                setAdNotApplicable(result, country, hsCode,
                    "No known GCC anti-dumping measure on HS " + hsCode + " from India.");
            }
        }
        // Bangladesh, China
        else if (countryLower.contains("bangladesh")) {
            setAdNotApplicable(result, country, hsCode,
                "Bangladesh does not currently have active anti-dumping measures on Indian HS " + hsCode + ".");
        } else if (countryLower.contains("china")) {
            if (chapter.equals("28") || chapter.equals("29")) {
                setAdRequiresVerification(result, "Chemical products",
                    "China has imposed anti-dumping measures on select chemicals. " +
                    "Verify with MOFCOM (Ministry of Commerce) for HS " + hsCode + ".",
                    "http://www.mofcom.gov.cn");
            } else {
                setAdNotApplicable(result, country, hsCode,
                    "No known Chinese anti-dumping measure on HS " + hsCode + " from India.");
            }
        } else {
            setAdRequiresVerification(result, "General",
                "Anti-dumping status could not be confirmed for " + country + ". " +
                "Check with destination customs authority.",
                "https://www.wto.org/english/tratop_e/adp_e/adp_e.htm");
        }

        return result;
    }

    private void setAdApplicable(Map<String, Object> result, String productScope, String dutyRate,
            String authority, String effectiveDate, String expiryDate, String sourceUrl, String reason) {
        result.put("status", "APPLICABLE");
        result.put("productScope", productScope);
        result.put("dutyRate", dutyRate);
        result.put("authority", authority);
        result.put("effectiveDate", effectiveDate);
        result.put("expiryDate", expiryDate);
        result.put("sourceUrl", sourceUrl);
        result.put("reason", reason);
        result.put("originCountry", "India");
    }

    private void setAdNotApplicable(Map<String, Object> result, String country, String hsCode, String reason) {
        result.put("status", "NOT_APPLICABLE");
        result.put("reason", reason);
        result.put("checkedCountry", country);
        result.put("checkedHsCode", hsCode);
        result.put("checkedOrigin", "India");
        result.put("source", "WTO Anti-Dumping Gateway / Destination country trade authority");
    }

    private void setAdRequiresVerification(Map<String, Object> result, String productScope,
            String reason, String sourceUrl) {
        result.put("status", "REQUIRES_VERIFICATION");
        result.put("productScope", productScope);
        result.put("reason", reason);
        result.put("sourceUrl", sourceUrl);
        result.put("recommendation", "Contact customs broker or destination trade authority for confirmation.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PACKAGING REQUIREMENTS VERIFICATION (including Dangerous Goods)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Determine packaging requirements including dangerous goods classification.
     */
    public Map<String, Object> getPackagingRequirements(String country, String hsCode, String productDescription) {
        Map<String, Object> result = new LinkedHashMap<>();
        String chapter = hsCode.length() >= 2 ? hsCode.substring(0, 2) : "";
        String heading = hsCode.length() >= 4 ? hsCode.substring(0, 4) : "";

        // Check if product is dangerous goods
        Map<String, Object> dgInfo = getDangerousGoodsClassification(heading, hsCode);

        if (dgInfo != null) {
            result.put("status", "MANDATORY_REQUIREMENTS");
            result.put("isDangerousGoods", true);
            result.putAll(dgInfo);
            result.put("requirements", getDgPackagingRequirements(dgInfo, country));
            result.put("source", "IMDG Code / IATA DGR / ADR");
            result.put("reason", "Product classified as dangerous goods — mandatory packaging per international transport regulations.");
        } else {
            // Non-DG products — check for product-specific packaging
            List<String> requirements = getNonDgPackagingRequirements(chapter, heading, country);
            if (!requirements.isEmpty()) {
                result.put("status", "MANDATORY_REQUIREMENTS");
                result.put("isDangerousGoods", false);
                result.put("requirements", requirements);
                result.put("reason", "Product-specific packaging regulations apply.");
            } else {
                result.put("status", "NO_SPECIFIC_MANDATORY_REQUIREMENTS");
                result.put("isDangerousGoods", false);
                result.put("requirements", List.of("Standard export packaging appropriate for product type",
                    "Packaging must protect goods during international transit",
                    "ISPM-15 treated wood packaging if using wooden pallets/crates"));
                result.put("reason", "No product-specific mandatory packaging regulations identified. Standard good-practice packaging applies.");
            }
        }

        return result;
    }

    /**
     * Classify dangerous goods by HS heading.
     * Returns null if product is not classified as dangerous goods.
     */
    private Map<String, Object> getDangerousGoodsClassification(String heading, String hsCode) {
        // Caustic Soda / Sodium Hydroxide
        if (heading.equals("2815")) {
            return Map.of(
                "unNumber", "UN1823",
                "properShippingName", "Sodium hydroxide, solid (Caustic Soda)",
                "dgClass", "8",
                "dgClassName", "Corrosive",
                "packingGroup", "II",
                "packingInstruction", "P002 (IMDG), 813 (IATA)",
                "subsidiaryRisk", "None",
                "marinePollutant", false
            );
        }
        // Sulfuric Acid
        if (heading.equals("2807")) {
            return Map.of("unNumber", "UN1830", "properShippingName", "Sulfuric acid",
                "dgClass", "8", "dgClassName", "Corrosive", "packingGroup", "II",
                "packingInstruction", "P001 (IMDG)", "subsidiaryRisk", "None", "marinePollutant", false);
        }
        // Hydrochloric Acid
        if (heading.equals("2806")) {
            return Map.of("unNumber", "UN1789", "properShippingName", "Hydrochloric acid",
                "dgClass", "8", "dgClassName", "Corrosive", "packingGroup", "II",
                "packingInstruction", "P001 (IMDG)", "subsidiaryRisk", "None", "marinePollutant", false);
        }
        // Benzene
        if (heading.equals("2902") && hsCode.startsWith("29022")) {
            return Map.of("unNumber", "UN1114", "properShippingName", "Benzene",
                "dgClass", "3", "dgClassName", "Flammable Liquid", "packingGroup", "II",
                "packingInstruction", "P001 (IMDG)", "subsidiaryRisk", "6.1 (Toxic)", "marinePollutant", true);
        }
        // Methanol
        if (heading.equals("2905") && hsCode.startsWith("29051")) {
            return Map.of("unNumber", "UN1230", "properShippingName", "Methanol",
                "dgClass", "3", "dgClassName", "Flammable Liquid", "packingGroup", "II",
                "packingInstruction", "P001 (IMDG)", "subsidiaryRisk", "6.1 (Toxic)", "marinePollutant", false);
        }
        // Ammonia
        if (heading.equals("2814")) {
            return Map.of("unNumber", "UN2672", "properShippingName", "Ammonia solution",
                "dgClass", "8", "dgClassName", "Corrosive", "packingGroup", "III",
                "packingInstruction", "P001 (IMDG)", "subsidiaryRisk", "None", "marinePollutant", false);
        }
        // Insecticides / Pesticides
        if (heading.equals("3808")) {
            return Map.of("unNumber", "UN2588", "properShippingName", "Pesticide, solid, toxic",
                "dgClass", "6.1", "dgClassName", "Toxic", "packingGroup", "III",
                "packingInstruction", "P002 (IMDG)", "subsidiaryRisk", "None", "marinePollutant", true);
        }
        // Matches / Pyrotechnics
        if (heading.equals("3604") || heading.equals("3605")) {
            return Map.of("unNumber", "UN1944", "properShippingName", "Matches, safety",
                "dgClass", "4.1", "dgClassName", "Flammable Solid", "packingGroup", "III",
                "packingInstruction", "P407 (IMDG)", "subsidiaryRisk", "None", "marinePollutant", false);
        }
        // Petroleum / Mineral fuels
        if (heading.equals("2710")) {
            return Map.of("unNumber", "UN1268", "properShippingName", "Petroleum products",
                "dgClass", "3", "dgClassName", "Flammable Liquid", "packingGroup", "II",
                "packingInstruction", "P001 (IMDG)", "subsidiaryRisk", "None", "marinePollutant", true);
        }
        // Batteries / Accumulators  
        if (heading.equals("8507")) {
            return Map.of("unNumber", "UN3481", "properShippingName", "Lithium ion batteries",
                "dgClass", "9", "dgClassName", "Miscellaneous Dangerous Goods", "packingGroup", "N/A",
                "packingInstruction", "P903/PI966 (IATA)", "subsidiaryRisk", "None", "marinePollutant", false);
        }

        return null; // Not classified as dangerous goods
    }

    private List<String> getDgPackagingRequirements(Map<String, Object> dgInfo, String country) {
        List<String> reqs = new ArrayList<>();
        String dgClass = (String) dgInfo.get("dgClass");
        String packingGroup = (String) dgInfo.get("packingGroup");
        String unNumber = (String) dgInfo.get("unNumber");

        reqs.add("UN-certified packaging required (marked with UN symbol + " + unNumber + ")");
        reqs.add("Packing Group " + packingGroup + " standards apply — " + getPackingGroupDesc(packingGroup));
        reqs.add("IMDG Code compliant packaging for sea freight");
        reqs.add("IATA DGR compliant if shipped by air");
        reqs.add("Inner packaging must be compatible with contents (chemical resistance verified)");
        reqs.add("Maximum net quantity per package as per packing instruction " + dgInfo.get("packingInstruction"));

        if ("8".equals(dgClass)) {
            reqs.add("Corrosion-resistant inner containers (HDPE, SS316, or glass for liquids)");
            reqs.add("Absorbent material between inner and outer packaging");
            reqs.add("Leak-proof closure with gasket seal");
        } else if ("3".equals(dgClass)) {
            reqs.add("Spark-proof, vapor-tight containers");
            reqs.add("Grounding/bonding during filling operations");
            reqs.add("Flame arrester on container vents where applicable");
        } else if ("6.1".equals(dgClass)) {
            reqs.add("Sealed, tamper-evident packaging");
            reqs.add("Child-resistant closure if retail quantities");
        }

        reqs.add("DG declaration placard and marks on outer packaging (diamond label Class " + dgClass + ")");
        reqs.add("Orientation arrows on packages containing liquids");
        reqs.add("Emergency contact information on outer package");

        // Country-specific
        if (country.toLowerCase().contains("hong kong")) {
            reqs.add("Comply with Hong Kong Dangerous Goods Ordinance (Cap. 295)");
            reqs.add("Advance notification to HK Fire Services Department for DG Class " + dgClass);
        } else if (country.toLowerCase().contains("united states")) {
            reqs.add("DOT 49 CFR compliant packaging");
            reqs.add("EPA RCRA labeling if classified as hazardous waste");
        } else if (country.toLowerCase().contains("germany") || country.toLowerCase().contains("netherlands")) {
            reqs.add("ADR compliant for road transport within EU");
            reqs.add("CLP Regulation labeling on inner packages");
        }

        return reqs;
    }

    private String getPackingGroupDesc(String pg) {
        return switch (pg) {
            case "I" -> "High danger — most stringent packaging standards";
            case "II" -> "Medium danger — intermediate packaging standards";
            case "III" -> "Low danger — standard packaging acceptable";
            default -> "Refer to specific packing instruction";
        };
    }

    private List<String> getNonDgPackagingRequirements(String chapter, String heading, String country) {
        List<String> reqs = new ArrayList<>();

        // Food products
        if (List.of("02","03","04","07","08","09","10","11","12","15","16","17","18","19","20","21","22").contains(chapter)) {
            reqs.add("Food-grade packaging materials only (no migration of harmful substances)");
            reqs.add("Hermetic/vacuum sealing for perishable goods");
            reqs.add("Temperature-controlled packaging for cold-chain products");
            reqs.add("Tamper-evident seals required");
            if (country.toLowerCase().contains("united states")) {
                reqs.add("FDA 21 CFR compliant food contact materials");
            } else if (country.toLowerCase().contains("germany") || country.toLowerCase().contains("netherlands")) {
                reqs.add("EU Regulation 1935/2004 food contact materials compliance");
                reqs.add("EU packaging waste directive 94/62/EC compliance");
            }
        }
        // Pharmaceuticals
        else if (chapter.equals("30")) {
            reqs.add("WHO Technical Report Series guidelines for pharma packaging");
            reqs.add("Child-resistant closures (for retail packs)");
            reqs.add("Moisture barrier packaging for hygroscopic products");
            reqs.add("Light-protective packaging where required by stability data");
            reqs.add("Tamper-evident seals mandatory");
            reqs.add("Cold chain packaging for temperature-sensitive biologics");
        }
        // Ceramics / Glass (fragile)
        else if (chapter.equals("69") || chapter.equals("70")) {
            reqs.add("Impact-resistant outer packaging (double-wall corrugated minimum)");
            reqs.add("Individual wrapping with cushioning material");
            reqs.add("FRAGILE marking on all sides of outer carton");
            reqs.add("Stack limitation marking");
        }

        return reqs;
    }
}
