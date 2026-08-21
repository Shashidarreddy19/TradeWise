package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EuCnRdfExtractorService {

    private final HsRawRepository hsRawRepository;

    // Pattern for 8-digit CN code inside prefLabel: "2004 90 91 --- Onions..." or "20049091 --- Onions..."
    private static final Pattern CN8_LABEL_PATTERN = Pattern.compile("^(\\d{2}\\s*\\d{2}\\s*\\d{2}\\s*\\d{2}|\\d{4}\\s*\\d{4}|\\d{8})[\\s\\-:]+(.*)$");

    public EuCnRdfExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
    }

    @Data
    @Builder
    public static class RdfExtractionSummary {
        private String country;
        private String rdfFilePath;
        private long totalDescriptionsParsed;
        private long totalConceptsExtracted;
        private long rawInserted;
        private long skippedCount;
        private long executionTimeMs;
    }

    /**
     * Low-memory streaming StAX (XMLStreamReader) parser for ESTAT-CN2026.rdf (171 MB).
     */
    public RdfExtractionSummary extractRdfCnData(String country, String rdfPath, Long executionId, String datasetVersion) {
        long startTime = System.currentTimeMillis();
        String targetCountry = country != null ? country : "Germany";
        String filePath = rdfPath != null ? rdfPath : "ESTAT-CN2026.rdf";
        String version = datasetVersion != null ? datasetVersion : "CN_2026";

        log.info("Starting StAX streaming RDF/XML extraction of EU CN 2026 for country: {} from: {}", targetCountry, filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("RDF/XML file not found at path: {}", filePath);
            throw new IllegalArgumentException("RDF/XML file not found at path: " + filePath);
        }

        // Language preference: "de" for Germany, "nl" for Netherlands, fallback "en"
        String targetLang = getTargetLanguage(targetCountry);

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        long totalDescriptionsParsed = 0;
        long totalConceptsExtracted = 0;
        long rawInserted = 0;
        long skippedCount = 0;

        List<HsRawEntity> batch = new ArrayList<>();
        Set<String> seenCodesInFile = new HashSet<>();

        try (InputStream is = new FileInputStream(file)) {
            XMLStreamReader reader = factory.createXMLStreamReader(is);

            boolean inDescription = false;
            boolean isConcept = false;
            Map<String, String> prefLabelsByLang = new HashMap<>();

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String localName = reader.getLocalName();
                        if ("Description".equals(localName)) {
                            inDescription = true;
                            isConcept = false;
                            prefLabelsByLang.clear();
                        } else if (inDescription) {
                            if ("type".equals(localName)) {
                                String resource = reader.getAttributeValue(null, "resource");
                                if (resource != null && resource.contains("skos/core#Concept")) {
                                    isConcept = true;
                                }
                            } else if ("prefLabel".equals(localName)) {
                                String lang = reader.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang");
                                String labelText = reader.getElementText();
                                if (labelText != null && !labelText.trim().isEmpty()) {
                                    String langKey = lang != null ? lang.toLowerCase() : "en";
                                    prefLabelsByLang.put(langKey, labelText.trim());
                                }
                            }
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        if ("Description".equals(reader.getLocalName())) {
                            totalDescriptionsParsed++;

                            if (isConcept && !prefLabelsByLang.isEmpty()) {
                                String chosenLabel = selectBestLabel(prefLabelsByLang, targetLang);
                                if (chosenLabel != null) {
                                    CnParsedItem item = parseCnLabel(chosenLabel);
                                    if (item != null && item.getCode() != null && item.getCode().matches("^\\d{8}$")) {
                                        if (!seenCodesInFile.contains(item.getCode())) {
                                            seenCodesInFile.add(item.getCode());
                                            totalConceptsExtracted++;

                                            HsRawEntity raw = HsRawEntity.builder()
                                                    .executionId(executionId != null ? executionId : 1L)
                                                    .customsTerritory("EU")
                                                    .country(targetCountry)
                                                    .rawNationalCode(item.getCode())
                                                    .rawDescription(item.getDescription())
                                                    .unit(null)
                                                    .sourceName("EU Combined Nomenclature")
                                                    .datasetVersion(version)
                                                    .build();

                                            batch.add(raw);

                                            if (batch.size() >= 500) {
                                                hsRawRepository.saveAll(batch);
                                                rawInserted += batch.size();
                                                batch.clear();
                                            }
                                        }
                                    } else {
                                        skippedCount++;
                                    }
                                }
                            }
                            inDescription = false;
                            isConcept = false;
                        }
                        break;
                }
            }

            if (!batch.isEmpty()) {
                hsRawRepository.saveAll(batch);
                rawInserted += batch.size();
                batch.clear();
            }

        } catch (Exception e) {
            log.error("Fatal error during StAX streaming parse of RDF file {}", filePath, e);
            throw new RuntimeException("Failed parsing ESTAT-CN2026.rdf: " + e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Completed StAX XML parsing of {} for {}. Descriptions: {}, Concepts: {}, Inserted hs_raw: {}, Time: {} ms",
                filePath, targetCountry, totalDescriptionsParsed, totalConceptsExtracted, rawInserted, duration);

        return RdfExtractionSummary.builder()
                .country(targetCountry)
                .rdfFilePath(filePath)
                .totalDescriptionsParsed(totalDescriptionsParsed)
                .totalConceptsExtracted(totalConceptsExtracted)
                .rawInserted(rawInserted)
                .skippedCount(skippedCount)
                .executionTimeMs(duration)
                .build();
    }

    private String getTargetLanguage(String country) {
        if ("Germany".equalsIgnoreCase(country) || "DE".equalsIgnoreCase(country)) return "de";
        if ("Netherlands".equalsIgnoreCase(country) || "NL".equalsIgnoreCase(country)) return "nl";
        if ("France".equalsIgnoreCase(country) || "FR".equalsIgnoreCase(country)) return "fr";
        if ("Spain".equalsIgnoreCase(country) || "ES".equalsIgnoreCase(country)) return "es";
        if ("Italy".equalsIgnoreCase(country) || "IT".equalsIgnoreCase(country)) return "it";
        return "en";
    }

    private String selectBestLabel(Map<String, String> prefLabels, String targetLang) {
        if (prefLabels.containsKey(targetLang)) {
            return prefLabels.get(targetLang);
        }
        if (prefLabels.containsKey("en")) {
            return prefLabels.get("en");
        }
        return prefLabels.values().iterator().next();
    }

    private CnParsedItem parseCnLabel(String label) {
        Matcher matcher = CN8_LABEL_PATTERN.matcher(label.trim());
        if (matcher.find()) {
            String rawCodePart = matcher.group(1);
            String rawDescPart = matcher.group(2);

            String cleanCode = rawCodePart.replaceAll("[^0-9]", "").trim();
            String cleanDesc = rawDescPart.replaceAll("^[\\-\\:\\s]+", "").trim();

            if (cleanCode.length() == 8 && !cleanDesc.isEmpty()) {
                return new CnParsedItem(cleanCode, cleanDesc);
            }
        }
        return null;
    }

    @Data
    private static class CnParsedItem {
        private final String code;
        private final String description;
    }
}
