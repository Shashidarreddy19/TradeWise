package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.entity.CountryMasterEntity;
import com.cbec.ai.pipeline.model.entity.SourceMasterEntity;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;
import com.cbec.ai.pipeline.repository.CountryMasterRepository;
import com.cbec.ai.pipeline.repository.SourceMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class Stage1SourceDiscoveryService {

    private final CountryMasterRepository countryMasterRepository;
    private final SourceMasterRepository sourceMasterRepository;

    public Stage1SourceDiscoveryService(
            CountryMasterRepository countryMasterRepository,
            SourceMasterRepository sourceMasterRepository) {
        this.countryMasterRepository = countryMasterRepository;
        this.sourceMasterRepository = sourceMasterRepository;
    }

    /**
     * Stage 1: Source Discovery.
     * Looks up `country_master` and `source_master` from DB to retrieve authority, customs territory, nomenclature type, and expected code length.
     */
    public SourceMetadataDto discoverSource(String country, String sourceName, String sourcePathOrUrl, String version) {
        log.info("Stage 1 [Source Discovery] - Resolving official source metadata for country: {}, source: {}", country, sourceName);

        Optional<CountryMasterEntity> countryOpt = countryMasterRepository.findByCountryName(country);
        String territory = countryOpt.map(CountryMasterEntity::getCustomsTerritory).orElse(getTerritoryFromCountry(country));

        if (countryOpt.isEmpty()) {
            countryMasterRepository.save(CountryMasterEntity.builder()
                    .countryCode(getCountryCodeForName(country))
                    .countryName(country)
                    .customsTerritory(territory)
                    .active(true)
                    .build());
        }

        FileTypeEnum detectedType = FileTypeEnum.fromFileNameOrUrl(sourcePathOrUrl);

        // Fetch source configuration from source_master if exists
        String authority = "Official Customs Authority";
        String nomenclature = getNomenclatureForCountry(country);
        int expectedLength = getExpectedCodeLengthForCountry(country);
        Long sourceId = null;

        List<SourceMasterEntity> sources = sourceMasterRepository.findByCountry(country);
        if (sources.isEmpty()) {
            sources = sourceMasterRepository.findByCustomsTerritory(territory);
        }
        if (!sources.isEmpty()) {
            SourceMasterEntity s = sources.get(0);
            sourceId = s.getId();
            authority = s.getAuthority();
            nomenclature = s.getNomenclatureType();
            expectedLength = s.getMaximumCodeLength();
        }

        return SourceMetadataDto.builder()
                .sourceId(sourceId)
                .customsTerritory(territory)
                .country(country)
                .authority(authority)
                .sourceName(sourceName)
                .sourceUrlOrPath(sourcePathOrUrl)
                .sourceType("BULK_DOWNLOAD")
                .fileType(detectedType)
                .nomenclatureType(nomenclature)
                .expectedCodeLength(expectedLength)
                .version(version != null ? version : "2026.1")
                .discoveredAt(LocalDateTime.now())
                .build();
    }

    private String getTerritoryFromCountry(String country) {
        if (country == null) return "GLOBAL";
        String lower = country.toLowerCase();
        if (lower.contains("netherlands") || lower.contains("germany") || lower.contains("eu")) return "EU";
        if (lower.contains("united arab emirates") || lower.contains("saudi arabia") || lower.contains("uae") || lower.contains("gcc")) return "GCC";
        if (lower.contains("india")) return "IN";
        if (lower.contains("united states") || lower.contains("us")) return "US";
        if (lower.contains("united kingdom") || lower.contains("uk") || lower.contains("gb")) return "GB";
        return country.substring(0, Math.min(country.length(), 3)).toUpperCase();
    }

    private String getNomenclatureForCountry(String country) {
        if (country == null) return "NATIONAL_TARIFF";
        String lower = country.toLowerCase();
        if (lower.contains("india")) return "ITC_HS";
        if (lower.contains("united states") || lower.contains("us")) return "HTS";
        if (lower.contains("netherlands") || lower.contains("germany")) return "CN";
        if (lower.contains("united kingdom") || lower.contains("uk")) return "UK_TARIFF";
        if (lower.contains("united arab emirates") || lower.contains("saudi arabia") || lower.contains("uae")) return "GCC_TARIFF";
        return "NATIONAL_TARIFF";
    }

    private int getExpectedCodeLengthForCountry(String country) {
        if (country == null) return 8;
        String lower = country.toLowerCase();
        if (lower.contains("united arab emirates") || lower.contains("saudi arabia") || lower.contains("uae")) return 12;
        if (lower.contains("united states") || lower.contains("us") || lower.contains("united kingdom") || lower.contains("uk") || lower.contains("china")) return 10;
        return 8;
    }

    private String getCountryCodeForName(String country) {
        if (country == null) return "XX";
        String lower = country.toLowerCase();
        if (lower.contains("india")) return "IN";
        if (lower.contains("united states") || lower.contains("us")) return "US";
        if (lower.contains("united kingdom") || lower.contains("uk")) return "GB";
        if (lower.contains("germany")) return "DE";
        if (lower.contains("netherlands")) return "NL";
        if (lower.contains("saudi arabia")) return "SA";
        if (lower.contains("united arab emirates") || lower.contains("uae")) return "AE";
        if (lower.contains("singapore")) return "SG";
        if (lower.contains("bangladesh")) return "BD";
        if (lower.contains("hong kong")) return "HK";
        if (lower.contains("china")) return "CN";
        String clean = country.replaceAll("[^a-zA-Z]", "").toUpperCase();
        return clean.substring(0, Math.min(2, clean.length()));
    }
}
