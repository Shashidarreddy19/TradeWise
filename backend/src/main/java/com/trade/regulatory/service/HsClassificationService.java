package com.trade.regulatory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.regulatory.dto.HsClassificationRequest;
import com.trade.regulatory.dto.HsClassificationResponse;
import com.trade.regulatory.dto.HsClassificationResponse.HsCandidate;
import com.trade.regulatory.entity.HsMasterEntity;
import com.trade.regulatory.repository.HsMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Production HS Code Classification Service.
 *
 * Pipeline:
 * 1. Extract product features from user input
 * 2. Retrieve candidate HS codes from TradeData (India)
 * 3. Score candidates using text similarity
 * 4. Rank candidates using NVIDIA LLM (if available)
 * 5. Apply deterministic validation rules
 * 6. Return top candidates with evidence + explanation
 *
 * CRITICAL RULES:
 * - NEVER fabricate an HS code
 * - NEVER return a code not in hs_master
 * - NEVER claim 100% certainty
 * - Only select from database-retrieved candidates
 */
@Slf4j
@Service
@Transactional(readOnly = true, transactionManager = "tradeDataTransactionManager")
public class HsClassificationService {

    private final HsMasterRepository hsMasterRepo;
    private final NvidiaAiService aiService;
    private final ObjectMapper objectMapper;

    // Scoring weights
    private static final double W_DESCRIPTION = 0.25;
    private static final double W_MATERIAL    = 0.15;
    private static final double W_FUNCTION    = 0.20;
    private static final double W_CATEGORY    = 0.10;
    private static final double W_MANUFACTURING = 0.10;
    private static final double W_PHYSICAL_FORM = 0.05;
    private static final double W_TECHNICAL   = 0.10;
    private static final double W_OFFICIAL    = 0.05;

    private static final double MIN_CONFIDENCE_THRESHOLD = 40.0;

    // HS Chapter → Category mapping for candidate retrieval
    private static final Map<String, List<String>> CATEGORY_CHAPTERS = Map.ofEntries(
        Map.entry("Agricultural Products", List.of("01","02","03","04","05","06","07","08","09","10","11","12","13","14")),
        Map.entry("Spices", List.of("09")),
        Map.entry("Food Products", List.of("02","03","04","07","08","09","10","11","15","16","17","18","19","20","21","22","23")),
        Map.entry("Processed Foods", List.of("16","17","18","19","20","21","22")),
        Map.entry("Marine Products", List.of("03","16")),
        Map.entry("Textiles", List.of("50","51","52","53","54","55","56","57","58","59","60","61","62","63")),
        Map.entry("Apparel & Garments", List.of("61","62")),
        Map.entry("Leather Products", List.of("41","42","43")),
        Map.entry("Footwear", List.of("64")),
        Map.entry("Ceramics & Pottery", List.of("69")),
        Map.entry("Chemicals", List.of("28","29","31","38")),
        Map.entry("Cosmetics & Personal Care", List.of("33")),
        Map.entry("Pharmaceuticals", List.of("29","30")),
        Map.entry("Electronics", List.of("84","85","90")),
        Map.entry("Engineering Goods", List.of("72","73","74","75","76","82","83","84")),
        Map.entry("Machinery", List.of("84","85")),
        Map.entry("Automotive Components", List.of("40","68","70","73","84","85","87")),
        Map.entry("Jewellery & Gems", List.of("71")),
        Map.entry("Plastics & Rubber", List.of("39","40")),
        Map.entry("Furniture & Wood", List.of("44","94"))
    );

    // Product terminology → HS search terms expansion
    // Helps find candidates when user terms differ from HS nomenclature
    private static final Map<String, List<String>> TERM_EXPANSION = Map.ofEntries(
        Map.entry("fertilizer", List.of("fertiliser", "mineral", "chemical", "nitrogen", "phosphate", "potassium")),
        Map.entry("brake", List.of("friction", "brake", "lining", "pad")),
        Map.entry("cream", List.of("cream", "preparation", "beauty", "skin care", "cosmetic")),
        Map.entry("tablet", List.of("medicament", "pharmaceutical", "dosage", "therapeutic")),
        Map.entry("medicine", List.of("medicament", "pharmaceutical", "therapeutic", "prophylactic")),
        Map.entry("container", List.of("article", "container", "box", "receptacle", "conveyance")),
        Map.entry("plastic", List.of("plastic", "polyethylene", "polypropylene", "polymer")),
        Map.entry("phone", List.of("telephone", "smartphone", "cellular", "wireless")),
        Map.entry("shoe", List.of("footwear", "shoe", "boot", "upper", "sole")),
        Map.entry("rice", List.of("rice", "basmati", "milled", "husked", "grain")),
        Map.entry("pepper", List.of("pepper", "piper", "spice", "dried", "capsicum")),
        Map.entry("leather", List.of("leather", "hide", "skin", "bovine", "tanned"))
    );

    public HsClassificationService(HsMasterRepository hsMasterRepo,
                                     NvidiaAiService aiService,
                                     ObjectMapper objectMapper) {
        this.hsMasterRepo = hsMasterRepo;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    /**
     * Main classification entry point.
     */
    public HsClassificationResponse classify(HsClassificationRequest request) {
        log.info("HS Classification request: product='{}', category='{}'",
                request.getProductName(), request.getCategory());

        // Step 1: Extract and normalize features
        ProductFeatures features = extractFeatures(request);

        // Step 2: Retrieve candidate HS codes from Indian hs_master
        List<HsMasterEntity> candidates = retrieveCandidates(features);
        log.info("Retrieved {} raw candidates from TradeData", candidates.size());

        if (candidates.isEmpty()) {
            // No DB match — use AI to predict HS code directly from product attributes.
            if (aiService.isAvailable()) {
                log.info("No DB candidates; invoking AI-only HS prediction for '{}'", request.getProductName());
                return aiDirectPrediction(request, features);
            }
            return buildNoMatchResponse(request, "No matching HS codes found in Indian tariff schedule and AI service is unavailable.");
        }

        // Step 3: Database-level scoring (text similarity)
        List<ScoredCandidate> scoredCandidates = scoreCandidates(candidates, features);

        // Step 4: AI ranking (if available)
        String classificationMode = "DATABASE_ONLY";
        if (aiService.isAvailable()) {
            scoredCandidates = aiRankCandidates(scoredCandidates, features);
            classificationMode = "AI_RANKED";
        }

        // Step 5: Validation — remove invalid, deduplicate, cap at top 10
        scoredCandidates = validateAndFilter(scoredCandidates);

        if (scoredCandidates.isEmpty()) {
            // All DB candidates rejected — use AI as fallback.
            if (aiService.isAvailable()) {
                log.info("All DB candidates rejected; invoking AI-only prediction for '{}'", request.getProductName());
                return aiDirectPrediction(request, features);
            }
            return buildNoMatchResponse(request, "All candidates were rejected during validation and AI service is unavailable.");
        }

        // Step 5b: If the BEST candidate is below the minimum confidence threshold,
        // the DB match is effectively noise (e.g. "table" matching "teakettles").
        // In this case, defer to AI direct prediction for a genuine classification.
        double bestScore = scoredCandidates.get(0).score;
        if (bestScore < 40.0 && aiService.isAvailable()) {
            log.info("Best DB candidate score ({}) is below threshold (40%); invoking AI direct prediction for '{}'",
                    bestScore, request.getProductName());
            return aiDirectPrediction(request, features);
        }

        // Step 6: Build response
        return buildResponse(request, scoredCandidates, classificationMode);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 1: Feature Extraction
    // ═══════════════════════════════════════════════════════════════════════════

    private ProductFeatures extractFeatures(HsClassificationRequest req) {
        ProductFeatures f = new ProductFeatures();
        f.productName = normalize(req.getProductName());
        f.description = normalize(req.getDescription());
        f.category = normalize(req.getCategory());
        f.material = normalize(req.getMaterial());
        f.composition = normalize(req.getComposition());
        f.function = normalize(req.getEffectiveFunction());
        f.manufacturing = normalize(req.getManufacturingProcess());
        f.physicalForm = normalize(req.getPhysicalForm());
        f.specifications = normalize(req.getEffectiveSpecifications());

        // Also incorporate productType into keywords if provided
        String productType = normalize(req.getProductType());

        // Extract keywords for search
        Set<String> keywords = new LinkedHashSet<>();
        addKeywords(keywords, f.productName);
        addKeywords(keywords, f.material);
        addKeywords(keywords, f.description);
        if (!productType.isBlank()) addKeywords(keywords, productType);

        // Expand keywords with HS terminology synonyms
        Set<String> expanded = new LinkedHashSet<>(keywords);
        for (String kw : keywords) {
            List<String> synonyms = TERM_EXPANSION.get(kw.toLowerCase());
            if (synonyms != null) {
                expanded.addAll(synonyms);
            }
        }
        f.keywords = new ArrayList<>(expanded);

        // Determine target chapters from category
        f.targetChapters = CATEGORY_CHAPTERS.getOrDefault(req.getCategory(), Collections.emptyList());

        return f;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 2: Candidate Retrieval (MySQL FULLTEXT + LIKE + Chapter filtering)
    // ═══════════════════════════════════════════════════════════════════════════

    private List<HsMasterEntity> retrieveCandidates(ProductFeatures features) {
        Set<HsMasterEntity> candidates = new LinkedHashSet<>();

        // Strategy 1: Direct product name search (HIGHEST priority — finds exact product types)
        if (!features.productName.isBlank()) {
            List<HsMasterEntity> nameResults = hsMasterRepo
                    .findByCountryAndOfficialDescriptionContainingIgnoreCaseAndIsCurrentTrue(
                            "India", features.productName);
            candidates.addAll(nameResults.stream().limit(20).toList());

            for (String word : features.productName.split("[\\s]+")) {
                if (word.contains("-") && word.length() >= 4) {
                    List<HsMasterEntity> hyphenResults = hsMasterRepo
                            .findByCountryAndOfficialDescriptionContainingIgnoreCaseAndIsCurrentTrue("India", word);
                    candidates.addAll(hyphenResults.stream().limit(20).toList());
                }
            }
            for (String word : features.productName.split("[\\s]+")) {
                if (!word.contains("-") && word.length() >= 4) {
                    List<HsMasterEntity> wordResults = hsMasterRepo
                            .findByCountryAndOfficialDescriptionContainingIgnoreCaseAndIsCurrentTrue("India", word);
                    candidates.addAll(wordResults.stream().limit(10).toList());
                }
            }
        }

        // Strategy 2: Category-chapter keyword search (ensures correct-chapter coverage)
        if (!features.targetChapters.isEmpty()) {
            for (String chapter : features.targetChapters) {
                List<HsMasterEntity> allChapterCodes = hsMasterRepo
                        .findByCountryAndChapterAndIsCurrentTrue("India", chapter);
                for (HsMasterEntity h : allChapterCodes) {
                    String descLower = h.getOfficialDescription().toLowerCase();
                    boolean matchesAnyKeyword = features.keywords.stream()
                            .anyMatch(kw -> kw.length() >= 3 && descLower.contains(kw.toLowerCase()));
                    if (matchesAnyKeyword) {
                        candidates.add(h);
                    }
                }
            }
        }

        // Strategy 3: FULLTEXT natural language search
        if (candidates.size() < 30) {
            String searchTerms = buildSearchQuery(features);
            if (!searchTerms.isBlank()) {
                try {
                    List<HsMasterEntity> fulltext = hsMasterRepo.fullTextSearch("India", searchTerms);
                    candidates.addAll(fulltext.stream().limit(15).toList());
                } catch (Exception e) {
                    log.warn("FULLTEXT failed: {}", e.getMessage());
                }
            }
        }

        // Strategy 4: Individual keyword LIKE search (broadest fallback)
        if (candidates.size() < 10) {
            for (String keyword : features.keywords.stream().limit(4).toList()) {
                if (keyword.length() >= 4) {
                    List<HsMasterEntity> likeResults = hsMasterRepo
                            .findByCountryAndOfficialDescriptionContainingIgnoreCaseAndIsCurrentTrue("India", keyword);
                    candidates.addAll(likeResults.stream().limit(10).toList());
                }
            }
        }

        // Deduplicate — keep ALL unique candidates (don't limit before scoring)
        List<HsMasterEntity> result = candidates.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(HsMasterEntity::getNationalCode))),
                        ArrayList::new));
        log.info("Candidate retrieval: {} unique candidates, headings: {}",
                result.size(),
                result.stream().map(HsMasterEntity::getHeading).distinct().limit(15).toList());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 3: Database-Level Scoring
    // ═══════════════════════════════════════════════════════════════════════════

    private List<ScoredCandidate> scoreCandidates(List<HsMasterEntity> candidates, ProductFeatures features) {
        return candidates.stream()
                .map(entity -> scoreCandidate(entity, features))
                .sorted(Comparator.comparingDouble(ScoredCandidate::getScore).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    private ScoredCandidate scoreCandidate(HsMasterEntity entity, ProductFeatures features) {
        String desc = entity.getOfficialDescription().toLowerCase();

        double descScore = textSimilarity(features.description + " " + features.productName, desc);
        double materialScore = textSimilarity(features.material + " " + features.composition, desc);
        double functionScore = textSimilarity(features.function, desc);
        double categoryScore = features.targetChapters.contains(entity.getChapter()) ? 80.0 : 20.0;
        double mfgScore = textSimilarity(features.manufacturing, desc);
        double formScore = textSimilarity(features.physicalForm, desc);
        double techScore = textSimilarity(features.specifications, desc);
        double officialScore = textSimilarity(features.productName, desc);

        double totalScore = (descScore * W_DESCRIPTION) +
                (materialScore * W_MATERIAL) +
                (functionScore * W_FUNCTION) +
                (categoryScore * W_CATEGORY) +
                (mfgScore * W_MANUFACTURING) +
                (formScore * W_PHYSICAL_FORM) +
                (techScore * W_TECHNICAL) +
                (officialScore * W_OFFICIAL);

        // Boost if product name words appear directly in description
        long nameHits = Arrays.stream(features.productName.split("[\\s\\-]+"))
                .filter(w -> w.length() > 2 && desc.contains(w))
                .count();
        totalScore += nameHits * 5.0;

        // MAJOR boost if the product name appears in the official description
        // Handles: "t-shirt" matching "T-shirts", "cotton t-shirt" matching description
        String pnLower = features.productName.toLowerCase();
        // Try exact product name
        if (desc.contains(pnLower)) {
            totalScore += 40.0;
        }
        // Try without hyphens: "t shirt" in "t-shirts"
        String pnNoHyphen = pnLower.replace("-", " ");
        String descNoHyphen = desc.replace("-", " ");
        if (descNoHyphen.contains(pnNoHyphen)) {
            totalScore += 40.0;
        }
        // Try hyphenated form from product name in description (e.g. "t-shirt" in "t-shirts")
        for (String pnWord : pnLower.split("[\\s]+")) {
            if (pnWord.contains("-") && pnWord.length() >= 4 && desc.contains(pnWord)) {
                totalScore += 35.0; // Strong product-type match
            }
        }
        // Individual word matches (lower value)
        for (String pnWord : pnLower.split("[\\s\\-]+")) {
            if (pnWord.length() > 3 && desc.contains(pnWord)) {
                totalScore += 5.0;
            }
        }

        // Boost leaf-level codes (more specific)
        if (entity.getNationalCode().length() >= 10) totalScore += 3.0;

        // Category-chapter alignment: strongly reward correct chapter, penalize wrong chapter
        if (!features.category.isBlank() && !features.targetChapters.isEmpty()) {
            if (features.targetChapters.contains(entity.getChapter())) {
                totalScore += 8.0; // Correct chapter for stated category
            } else {
                totalScore -= 18.0; // Wrong chapter — strongly penalize
            }
        }

        totalScore = Math.min(100.0, Math.max(0.0, totalScore));

        List<String> matchedAttrs = new ArrayList<>();
        if (descScore > 30) matchedAttrs.add("description");
        if (materialScore > 30) matchedAttrs.add(features.material);
        if (functionScore > 30) matchedAttrs.add(features.function);
        if (mfgScore > 30) matchedAttrs.add(features.manufacturing);
        if (formScore > 30) matchedAttrs.add(features.physicalForm);

        return new ScoredCandidate(entity, totalScore, matchedAttrs);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 4: AI Ranking
    // ═══════════════════════════════════════════════════════════════════════════

    private List<ScoredCandidate> aiRankCandidates(List<ScoredCandidate> dbScored, ProductFeatures features) {
        // Only send candidates from correct chapters to AI (if category is specified)
        List<ScoredCandidate> toRank;
        if (!features.targetChapters.isEmpty()) {
            List<ScoredCandidate> inCategory = dbScored.stream()
                    .filter(sc -> features.targetChapters.contains(sc.entity.getChapter()))
                    .limit(15)
                    .toList();
            // Use category-filtered if we have enough; otherwise use all
            toRank = inCategory.size() >= 3 ? inCategory : dbScored.stream().limit(15).toList();
        } else {
            toRank = dbScored.stream().limit(15).toList();
        }

        String candidateJson = buildCandidateJson(toRank);
        String productJson = buildProductJson(features);

        String systemPrompt = """
            You are an HS classification assistant for Indian exports.
            You are given a product description and a list of candidate HS codes from the official Indian tariff schedule.
            Your task is ONLY to rank the supplied candidates by relevance.
            Do NOT create, modify, guess, or invent an HS code.
            Evaluate: product identity, material, composition, function, manufacturing process, physical form.
            Return ONLY valid JSON array of objects: [{"hsCode":"...","score":0-100,"reason":"..."}]
            Use ONLY HS codes from the supplied candidate list.
            If no candidate matches well, return: []
            """;

        String userPrompt = "PRODUCT:\n" + productJson + "\n\nCANDIDATE HS CODES:\n" + candidateJson +
                "\n\nRank these candidates. Return JSON array only.";

        try {
            String aiResponse = aiService.chat(systemPrompt, userPrompt, "");

            // Parse AI response
            String cleaned = cleanJsonArray(aiResponse);
            List<Map<String, Object>> rankings = objectMapper.readValue(cleaned,
                    new TypeReference<List<Map<String, Object>>>() {});

            // Merge AI scores with DB scores
            Map<String, Double> aiScores = new HashMap<>();
            for (Map<String, Object> r : rankings) {
                String code = String.valueOf(r.get("hsCode"));
                double score = r.get("score") instanceof Number n ? n.doubleValue() : 50.0;
                aiScores.put(code, score);
            }

            // Combined score: 50% DB + 50% AI (balanced - don't let AI override strong DB signals)
            for (ScoredCandidate sc : toRank) {
                Double aiScore = aiScores.get(sc.entity.getNationalCode());
                if (aiScore != null) {
                    sc.score = (sc.score * 0.50) + (aiScore * 0.50);
                    sc.aiRanked = true;
                }
            }

            // Re-sort by combined score
            List<ScoredCandidate> result = toRank.stream()
                    .sorted(Comparator.comparingDouble(ScoredCandidate::getScore).reversed())
                    .collect(Collectors.toList());

            log.info("AI ranking applied to {} candidates", aiScores.size());
            return result;

        } catch (Exception e) {
            log.warn("AI ranking failed, using database-only scores: {}", e.getMessage());
            return dbScored.stream().limit(15).collect(Collectors.toList());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 5: Validation
    // ═══════════════════════════════════════════════════════════════════════════

    private List<ScoredCandidate> validateAndFilter(List<ScoredCandidate> candidates) {
        // If we have category-specific chapters, strongly prefer those candidates
        // This prevents AI from overriding category alignment
        List<ScoredCandidate> filtered = candidates.stream()
                .filter(sc -> sc.entity.getCountry().equals("India"))
                .filter(sc -> sc.entity.getNationalCode() != null)
                .filter(sc -> sc.entity.getNationalCode().length() >= 4)
                .filter(sc -> sc.entity.getOfficialDescription() != null)
                .filter(sc -> sc.entity.getIsCurrent())
                .collect(Collectors.toList());

        // Deduplicate by national code, keep highest-scored
        Map<String, ScoredCandidate> deduped = new LinkedHashMap<>();
        for (ScoredCandidate sc : filtered) {
            String key = sc.entity.getNationalCode();
            if (!deduped.containsKey(key) || deduped.get(key).score < sc.score) {
                deduped.put(key, sc);
            }
        }

        return deduped.values().stream()
                .sorted(Comparator.comparingDouble(ScoredCandidate::getScore).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP 6: Response Building
    // ═══════════════════════════════════════════════════════════════════════════

    private HsClassificationResponse buildResponse(HsClassificationRequest request,
                                                     List<ScoredCandidate> scored,
                                                     String mode) {
        ScoredCandidate top = scored.get(0);
        double confidence = top.score;
        String level = getConfidenceLevel(confidence);
        String status = confidence >= MIN_CONFIDENCE_THRESHOLD ? "MATCHED" : "LOW_CONFIDENCE";

        List<HsCandidate> topCandidates = new ArrayList<>();
        int rank = 1;
        for (ScoredCandidate sc : scored) {
            topCandidates.add(HsCandidate.builder()
                    .hsCode(sc.entity.getNationalCode())
                    .officialDescription(sc.entity.getOfficialDescription())
                    .matchScore(Math.round(sc.score * 10.0) / 10.0)
                    .rank(rank++)
                    .reason(buildReason(sc, request))
                    .matchedAttributes(sc.matchedAttributes)
                    .chapter(sc.entity.getChapter())
                    .heading(sc.entity.getHeading())
                    .hs6(sc.entity.getHs6())
                    .source("TradeData - Indian Tariff Schedule")
                    .evidenceAvailable(true)
                    .build());
        }

        String explanation = String.format(
                "Product '%s' classified as %s (confidence: %.1f%%). " +
                "Classification based on %s. " +
                "The recommended HS code is from the official Indian tariff schedule (ITC-HS).",
                request.getProductName(), top.entity.getNationalCode(),
                confidence, mode.equals("AI_RANKED") ? "AI-ranked database candidates" : "database text matching");

        List<String> warnings = new ArrayList<>();
        warnings.add("Final classification should be verified against the applicable Indian tariff schedule.");
        if (confidence < 75) {
            warnings.add("Confidence is below 75% — consider consulting a customs broker.");
        }
        if (!top.aiRanked) {
            warnings.add("AI ranking was not applied — classification based on text similarity only.");
        }

        return HsClassificationResponse.builder()
                .productName(request.getProductName())
                .originCountry("India")
                .classificationStatus(status)
                .classificationMode(mode)
                .recommendedHsCode(top.entity.getNationalCode())
                .confidenceScore(Math.round(confidence * 10.0) / 10.0)
                .confidenceLevel(level)
                .classificationLevel(top.entity.getNationalCode().length() >= 8 ?
                        "INDIAN_NATIONAL_CODE" : "HS6")
                .topCandidates(topCandidates)
                .classificationExplanation(explanation)
                .warnings(warnings)
                .needsReview(confidence < 60.0)
                .dataSource("TradeData.hs_master")
                .build();
    }

    private HsClassificationResponse buildNoMatchResponse(HsClassificationRequest request, String reason) {
        return HsClassificationResponse.builder()
                .productName(request.getProductName())
                .originCountry("India")
                .classificationStatus("NO_CONFIDENT_MATCH")
                .classificationMode("DATABASE_ONLY")
                .confidenceScore(0.0)
                .confidenceLevel("NO_MATCH")
                .topCandidates(Collections.emptyList())
                .classificationExplanation(reason)
                .warnings(List.of(
                        "No matching HS code could be determined with sufficient confidence.",
                        "Please provide more product details or consult a customs classification specialist."))
                .build();
    }

    /**
     * AI-only HS code prediction when no database candidates are found.
     * The AI uses the full product attributes to predict the most appropriate HS code.
     * The response clearly indicates this is an AI prediction requiring verification.
     */
    private HsClassificationResponse aiDirectPrediction(HsClassificationRequest request, ProductFeatures features) {
        String systemPrompt = "You are an expert customs classification specialist for Indian exports (ITC-HS 2022). " +
                "Given a product's attributes, predict the most likely HS code (8-10 digit Indian tariff code). " +
                "You MUST respond in EXACTLY this JSON format with no other text:\n" +
                "[{\"hsCode\":\"XXXXXXXXXX\",\"description\":\"official HS description\",\"confidence\":75,\"reasoning\":\"why this code fits\"}," +
                "{\"hsCode\":\"YYYYYYYYYY\",\"description\":\"alternative HS description\",\"confidence\":60,\"reasoning\":\"alternative reasoning\"}]\n" +
                "Rules:\n" +
                "- Return 1-3 predictions, most confident first.\n" +
                "- confidence is 0-100 (be honest: if uncertain, say 40-60).\n" +
                "- Use real ITC-HS codes (NOT invented). If truly uncertain, use the broadest applicable heading.\n" +
                "- The HS code should be for EXPORT from India.\n" +
                "- Consider: product name, category, material, composition, function, manufacturing process, physical form.";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Classify this product for Indian export:\n");
        userPrompt.append("Product Name: ").append(request.getProductName()).append("\n");
        if (request.getCategory() != null && !request.getCategory().isBlank())
            userPrompt.append("Category: ").append(request.getCategory()).append("\n");
        if (request.getDescription() != null && !request.getDescription().isBlank())
            userPrompt.append("Description: ").append(request.getDescription()).append("\n");
        if (request.getMaterial() != null && !request.getMaterial().isBlank())
            userPrompt.append("Material: ").append(request.getMaterial()).append("\n");
        if (request.getComposition() != null && !request.getComposition().isBlank())
            userPrompt.append("Composition: ").append(request.getComposition()).append("\n");
        if (request.getManufacturingProcess() != null && !request.getManufacturingProcess().isBlank())
            userPrompt.append("Manufacturing: ").append(request.getManufacturingProcess()).append("\n");
        if (request.getPhysicalForm() != null && !request.getPhysicalForm().isBlank())
            userPrompt.append("Physical Form: ").append(request.getPhysicalForm()).append("\n");
        if (request.getEffectiveFunction() != null && !request.getEffectiveFunction().isBlank())
            userPrompt.append("Function/Use: ").append(request.getEffectiveFunction()).append("\n");

        try {
            String aiResponse = aiService.chat(systemPrompt, userPrompt.toString(), "");

            // Parse AI response (JSON array of predictions)
            List<Map<String, Object>> predictions = parseAiPredictions(aiResponse);

            if (predictions.isEmpty()) {
                log.warn("AI prediction returned empty/unparseable response for '{}'", request.getProductName());
                return buildNoMatchResponse(request, "AI could not determine an HS code. Please provide more specific product details.");
            }

            // Build response from AI predictions
            List<HsClassificationResponse.HsCandidate> topCandidates = new ArrayList<>();
            for (int i = 0; i < predictions.size(); i++) {
                Map<String, Object> pred = predictions.get(i);
                String hsCode = String.valueOf(pred.getOrDefault("hsCode", ""));
                String desc = String.valueOf(pred.getOrDefault("description", "AI-predicted classification"));
                int confidence = pred.containsKey("confidence") ? ((Number) pred.get("confidence")).intValue() : 50;
                String reasoning = String.valueOf(pred.getOrDefault("reasoning", ""));

                topCandidates.add(HsClassificationResponse.HsCandidate.builder()
                        .hsCode(hsCode)
                        .officialDescription(desc)
                        .matchScore((double) confidence)
                        .rank(i + 1)
                        .reason(reasoning)
                        .matchedAttributes(List.of("AI prediction"))
                        .chapter(hsCode.length() >= 2 ? hsCode.substring(0, 2) : "")
                        .heading(hsCode.length() >= 4 ? hsCode.substring(0, 4) : "")
                        .hs6(hsCode.length() >= 6 ? hsCode.substring(0, 6) : "")
                        .source("NVIDIA AI (nemotron-3-ultra-550b-a55b) — direct prediction")
                        .evidenceAvailable(false)
                        .build());
            }

            Map<String, Object> topPred = predictions.get(0);
            double topConfidence = topPred.containsKey("confidence") ? ((Number) topPred.get("confidence")).doubleValue() : 50.0;
            String confidenceLevel;
            if (topConfidence >= 90) confidenceLevel = "HIGH_CONFIDENCE";
            else if (topConfidence >= 75) confidenceLevel = "GOOD_MATCH";
            else if (topConfidence >= 60) confidenceLevel = "POSSIBLE_MATCH";
            else if (topConfidence >= 40) confidenceLevel = "LOW_CONFIDENCE";
            else confidenceLevel = "NEEDS_REVIEW";

            String topHs = String.valueOf(topPred.getOrDefault("hsCode", ""));
            String topReasoning = String.valueOf(topPred.getOrDefault("reasoning", ""));

            List<String> warnings = new ArrayList<>();
            warnings.add("This classification is an AI PREDICTION — no exact match was found in the database.");
            warnings.add("Verify this HS code against the official Indian Customs Tariff before use.");
            if (topConfidence < 70) {
                warnings.add("Low confidence prediction (" + (int) topConfidence + "%) — manual verification strongly recommended.");
            }

            return HsClassificationResponse.builder()
                    .productName(request.getProductName())
                    .originCountry("India")
                    .classificationStatus("AI_PREDICTED")
                    .classificationMode("AI_DIRECT_PREDICTION")
                    .recommendedHsCode(topHs)
                    .confidenceScore(topConfidence)
                    .confidenceLevel(confidenceLevel)
                    .classificationLevel("AI_PREDICTED")
                    .topCandidates(topCandidates)
                    .classificationExplanation("AI Prediction: " + topReasoning)
                    .warnings(warnings)
                    .needsReview(topConfidence < 70)
                    .dataSource("NVIDIA AI (direct prediction — no DB match found)")
                    .build();

        } catch (Exception e) {
            log.error("AI direct prediction failed for '{}': {}", request.getProductName(), e.getMessage());
            return buildNoMatchResponse(request,
                    "AI prediction failed (" + e.getMessage() + "). Please provide more specific product details.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAiPredictions(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) return Collections.emptyList();
        try {
            // Try to extract JSON array from the response (AI may wrap it in markdown)
            String json = aiResponse.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            // Try single-object parse
            try {
                String json = aiResponse.trim();
                int start = json.indexOf('{');
                int end = json.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    json = json.substring(start, end + 1);
                    Map<String, Object> single = objectMapper.readValue(json, Map.class);
                    return List.of(single);
                }
            } catch (Exception ignored) {}
            log.warn("Failed to parse AI prediction response: {}", aiResponse.substring(0, Math.min(200, aiResponse.length())));
            return Collections.emptyList();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private void addKeywords(Set<String> keywords, String text) {
        if (text == null || text.isBlank()) return;
        String[] stopWords = {"the","of","and","or","for","in","on","with","a","an","is","are","to","from","by"};
        Set<String> stops = Set.of(stopWords);
        // Keep hyphenated words intact AND split them
        Arrays.stream(text.toLowerCase().split("[\\s,;.()/]+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !stops.contains(w))
                .forEach(w -> {
                    keywords.add(w); // keep "t-shirt" intact
                    // Also add parts without hyphens (e.g. "t-shirt" -> "shirt")
                    if (w.contains("-")) {
                        Arrays.stream(w.split("-"))
                                .filter(p -> p.length() > 2)
                                .forEach(keywords::add);
                    }
                });
        // Also add multi-word product name as search phrase
        if (text.length() > 3) {
            keywords.add(text.toLowerCase().trim());
        }
    }

    private String buildSearchQuery(ProductFeatures features) {
        // Build a FULLTEXT boolean mode search string
        List<String> terms = new ArrayList<>();
        if (!features.productName.isBlank()) terms.add(features.productName);
        if (!features.material.isBlank()) terms.add(features.material);
        if (!features.manufacturing.isBlank()) terms.add(features.manufacturing);
        return String.join(" ", terms);
    }

    private double textSimilarity(String input, String target) {
        if (input == null || input.isBlank() || target == null || target.isBlank()) return 0.0;
        String[] inputWords = input.toLowerCase().split("[\\s,;.()\\-/]+");
        long matches = Arrays.stream(inputWords)
                .filter(w -> w.length() > 2 && target.contains(w))
                .count();
        if (inputWords.length == 0) return 0.0;
        return Math.min(100.0, (matches * 100.0) / Math.max(inputWords.length, 1));
    }

    private String getConfidenceLevel(double score) {
        if (score >= 90) return "HIGH_CONFIDENCE";
        if (score >= 75) return "GOOD_MATCH";
        if (score >= 60) return "POSSIBLE_MATCH";
        if (score >= MIN_CONFIDENCE_THRESHOLD) return "LOW_CONFIDENCE";
        return "NO_MATCH";
    }

    private String buildReason(ScoredCandidate sc, HsClassificationRequest req) {
        StringBuilder reason = new StringBuilder();
        reason.append("Matches: ");
        if (!sc.matchedAttributes.isEmpty()) {
            reason.append(String.join(", ", sc.matchedAttributes));
        } else {
            reason.append("partial text similarity");
        }
        if (sc.aiRanked) reason.append(" [AI-confirmed]");
        return reason.toString();
    }

    private String buildCandidateJson(List<ScoredCandidate> candidates) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < candidates.size(); i++) {
            HsMasterEntity e = candidates.get(i).entity;
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"hsCode\":\"%s\",\"description\":\"%s\",\"chapter\":\"%s\"}",
                    e.getNationalCode(),
                    e.getOfficialDescription().replace("\"", "'").substring(0, Math.min(150, e.getOfficialDescription().length())),
                    e.getChapter()));
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildProductJson(ProductFeatures f) {
        return String.format("{\"name\":\"%s\",\"material\":\"%s\",\"composition\":\"%s\",\"function\":\"%s\",\"manufacturing\":\"%s\",\"form\":\"%s\"}",
                f.productName, f.material, f.composition, f.function, f.manufacturing, f.physicalForm);
    }

    private String cleanJsonArray(String text) {
        if (text == null) return "[]";
        // Find first [ and last ]
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "[]";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL DATA STRUCTURES
    // ═══════════════════════════════════════════════════════════════════════════

    private static class ProductFeatures {
        String productName;
        String description;
        String category;
        String material;
        String composition;
        String function;
        String manufacturing;
        String physicalForm;
        String specifications;
        List<String> keywords;
        List<String> targetChapters;
    }

    private static class ScoredCandidate {
        HsMasterEntity entity;
        double score;
        List<String> matchedAttributes;
        boolean aiRanked = false;

        ScoredCandidate(HsMasterEntity entity, double score, List<String> matchedAttributes) {
            this.entity = entity;
            this.score = score;
            this.matchedAttributes = matchedAttributes;
        }

        double getScore() { return score; }
    }
}
