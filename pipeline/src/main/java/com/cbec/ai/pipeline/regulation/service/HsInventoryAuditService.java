package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.HsMasterEntity;
import com.cbec.ai.pipeline.repository.HsMasterRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step 1: HS Code Inventory Audit Service.
 *
 * For every supported country, reads hs_master and reports:
 * - Total HS codes
 * - Distinct HS2 chapters
 * - Distinct HS4 headings
 * - Distinct HS6 subheadings
 * - Total national-level codes (8+ digit entries)
 *
 * Does NOT modify hs_master.
 */
@Service
@Slf4j
public class HsInventoryAuditService {

    private final HsMasterRepository hsMasterRepository;

    public HsInventoryAuditService(HsMasterRepository hsMasterRepository) {
        this.hsMasterRepository = hsMasterRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryHsInventoryDto {
        private String country;
        private long totalHsCodes;
        private long hs2Count;
        private long hs4Count;
        private long hs6Count;
        private long nationalCodeCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalHsInventoryReportDto {
        private int countriesAudited;
        private long grandTotalHsCodes;
        private Map<String, CountryHsInventoryDto> countryInventories;
    }

    /**
     * Build HS inventory for a single country from hs_master.
     */
    public CountryHsInventoryDto buildInventoryForCountry(String country) {
        List<HsMasterEntity> allCodes = hsMasterRepository.findByCountry(country);
        long total = allCodes.size();
        long hs2 = allCodes.stream().map(HsMasterEntity::getChapter).filter(c -> c != null && !c.isBlank()).distinct().count();
        long hs4 = allCodes.stream().map(HsMasterEntity::getHeading).filter(h -> h != null && !h.isBlank()).distinct().count();
        long hs6 = allCodes.stream().map(HsMasterEntity::getHs6).filter(h -> h != null && !h.isBlank()).distinct().count();
        // National codes are all entries (8+ digit)
        long nationalCodes = allCodes.stream()
                .filter(e -> e.getCodeLength() != null && e.getCodeLength() >= 8)
                .count();
        log.info("HS Inventory [{}]: total={}, HS2={}, HS4={}, HS6={}, national(8+)={}", country, total, hs2, hs4, hs6, nationalCodes);
        return CountryHsInventoryDto.builder()
                .country(country)
                .totalHsCodes(total)
                .hs2Count(hs2)
                .hs4Count(hs4)
                .hs6Count(hs6)
                .nationalCodeCount(nationalCodes)
                .build();
    }

    /**
     * Build HS inventory for all 11 supported countries.
     */
    public GlobalHsInventoryReportDto buildGlobalInventory() {
        log.info("Building global HS inventory across all supported countries...");
        Map<String, CountryHsInventoryDto> inventories = new LinkedHashMap<>();
        long grandTotal = 0;
        for (String country : GlobalCoverageAuditService.ALL_11_COUNTRIES) {
            CountryHsInventoryDto inv = buildInventoryForCountry(country);
            inventories.put(country, inv);
            grandTotal += inv.getTotalHsCodes();
        }
        return GlobalHsInventoryReportDto.builder()
                .countriesAudited(inventories.size())
                .grandTotalHsCodes(grandTotal)
                .countryInventories(inventories)
                .build();
    }
}
