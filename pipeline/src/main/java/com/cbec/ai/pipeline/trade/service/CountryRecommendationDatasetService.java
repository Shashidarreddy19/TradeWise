package com.cbec.ai.pipeline.trade.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.regulation.service.ComplianceRetrievalService;
import com.cbec.ai.pipeline.regulation.service.ComplianceScoringService;
import com.cbec.ai.pipeline.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class CountryRecommendationDatasetService {

    private final TradeImportStatisticsRepository importRepository;
    private final IndiaExportStatisticsRepository exportRepository;
    private final TariffStatisticsRepository tariffRepository;
    private final CompetitionStatisticsRepository competitionRepository;
    private final CountryEconomicIndicatorsRepository economicRepository;
    private final ComplianceRetrievalService complianceRetrievalService;
    private final ComplianceScoringService complianceScoringService;

    public CountryRecommendationDatasetService(
            TradeImportStatisticsRepository importRepository,
            IndiaExportStatisticsRepository exportRepository,
            TariffStatisticsRepository tariffRepository,
            CompetitionStatisticsRepository competitionRepository,
            CountryEconomicIndicatorsRepository economicRepository,
            ComplianceRetrievalService complianceRetrievalService,
            ComplianceScoringService complianceScoringService) {
        this.importRepository = importRepository;
        this.exportRepository = exportRepository;
        this.tariffRepository = tariffRepository;
        this.competitionRepository = competitionRepository;
        this.economicRepository = economicRepository;
        this.complianceRetrievalService = complianceRetrievalService;
        this.complianceScoringService = complianceScoringService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryMarketFeatureVectorDto {
        private String hs6;
        private String destinationCountry;
        private Integer year;

        // Trade & Market Features
        private Double importValueUsd;
        private Double importGrowthPercent;
        private Double threeYearCagr;

        // India Export Features
        private Double indiaExportValueUsd;
        private Double indiaMarketSharePercent;
        private Integer indiaSupplierRank;

        // Tariff & Trade Barriers Features
        private Double mfnTariffPercent;
        private Double preferentialTariffPercent;
        private Double appliedTariffPercent;
        private Boolean dutyFree;

        // Competition Features
        private Integer supplierCountryCount;
        private String topSupplierCountry;
        private Double topSupplierSharePercent;
        private Double supplierHhi;

        // Macro-Economic Indicators
        private Double gdpUsd;
        private Double gdpPerCapitaUsd;
        private Double gdpGrowthPercent;
        private Long population;

        // Derived Normalized Scores (0 - 100)
        private Integer marketDemandScore;
        private Integer marketGrowthScore;
        private Integer indiaExportScore;
        private Integer competitionScore;
        private Integer tariffScore;
        private Integer economicStrengthScore;
        private Integer complianceScore; // Derived as: 100 - complianceComplexityScore
        private Integer compositeRecommendationScore;

        // Source Metadata
        private String tradeSource;
        private String tariffSource;
        private String economicSource;
        private LocalDateTime dataTimestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationDatasetMatrixDto {
        private Integer totalVectorsGenerated;
        private Integer uniqueHs6Count;
        private Integer targetCountriesCount;
        private List<CountryMarketFeatureVectorDto> featureVectors;
        private LocalDateTime generatedAt;
    }

    @Transactional(readOnly = true)
    public RecommendationDatasetMatrixDto generateRecommendationDatasetMatrix(String filterHs6) {
        List<TradeImportStatisticsEntity> imports = filterHs6 != null && !filterHs6.isBlank()
                ? importRepository.findByHs6(filterHs6)
                : importRepository.findAll();

        List<CountryMarketFeatureVectorDto> vectors = new ArrayList<>();
        Set<String> hs6Set = new HashSet<>();
        Set<String> countrySet = new HashSet<>();

        for (TradeImportStatisticsEntity imp : imports) {
            hs6Set.add(imp.getHs6());
            countrySet.add(imp.getDestinationCountry());

            String hs6 = imp.getHs6();
            String country = imp.getDestinationCountry();
            Integer year = imp.getYear();

            Optional<IndiaExportStatisticsEntity> expOpt = exportRepository.findByHs6AndDestinationCountryAndYear(hs6, country, year);
            Optional<TariffStatisticsEntity> tariffOpt = tariffRepository.findByHs6AndDestinationCountryAndYear(hs6, country, year);
            Optional<CompetitionStatisticsEntity> compOpt = competitionRepository.findByHs6AndDestinationCountryAndYear(hs6, country, year);
            Optional<CountryEconomicIndicatorsEntity> econOpt = economicRepository.findByCountryAndYear(country, year);

            // Compute Compliance Score directly from existing Regulatory Complexity Engine
            int complexityScore = 25;
            try {
                ComplianceRetrievalService.ComplianceDataBundleDto bundle = complianceRetrievalService.retrieveComplianceData(country, hs6 + "00");
                ComplianceScoringService.ScoringResultDto scoring = complianceScoringService.calculateComplianceScore(bundle);
                complexityScore = scoring.getScore();
            } catch (Exception e) {
                log.debug("Using baseline complexity score for {} {}: {}", country, hs6, e.getMessage());
            }

            int complianceScore = Math.max(0, 100 - complexityScore); // Higher complexity lowers compliance score

            // Derived Sub-Scores
            double impVal = imp.getImportValueUsd() != null ? imp.getImportValueUsd() : 0.0;
            int demandScore = Math.min(100, (int) Math.round((impVal / 50_000_000.0) * 100.0));
            int growthScore = Math.min(100, Math.max(0, (int) Math.round((imp.getImportGrowthPercent() != null ? imp.getImportGrowthPercent() : 5.0) * 10.0)));
            int exportScore = expOpt.isPresent() && expOpt.get().getIndiaMarketSharePercent() != null
                    ? Math.min(100, (int) Math.round(expOpt.get().getIndiaMarketSharePercent() * 5.0))
                    : 40;

            double mfnTariff = tariffOpt.isPresent() && tariffOpt.get().getMfnTariffPercent() != null
                    ? tariffOpt.get().getMfnTariffPercent()
                    : 5.0;
            int tariffScore = Math.max(0, 100 - (int) Math.round(mfnTariff * 10.0));

            double hhi = compOpt.isPresent() && compOpt.get().getSupplierHhi() != null
                    ? compOpt.get().getSupplierHhi()
                    : 2500.0;
            int competitionScore = Math.max(0, 100 - (int) Math.round((hhi / 10000.0) * 100.0));

            int econScore = econOpt.isPresent() && econOpt.get().getGdpPerCapitaUsd() != null
                    ? Math.min(100, (int) Math.round((econOpt.get().getGdpPerCapitaUsd() / 80000.0) * 100.0))
                    : 50;

            int compositeScore = (int) Math.round(
                    (demandScore * 0.25) +
                    (growthScore * 0.15) +
                    (exportScore * 0.15) +
                    (tariffScore * 0.15) +
                    (competitionScore * 0.10) +
                    (econScore * 0.10) +
                    (complianceScore * 0.10)
            );

            CountryMarketFeatureVectorDto vector = CountryMarketFeatureVectorDto.builder()
                    .hs6(hs6)
                    .destinationCountry(country)
                    .year(year)
                    .importValueUsd(impVal)
                    .importGrowthPercent(imp.getImportGrowthPercent())
                    .threeYearCagr(imp.getThreeYearCagr())
                    .indiaExportValueUsd(expOpt.map(IndiaExportStatisticsEntity::getIndiaExportValueUsd).orElse(null))
                    .indiaMarketSharePercent(expOpt.map(IndiaExportStatisticsEntity::getIndiaMarketSharePercent).orElse(null))
                    .indiaSupplierRank(expOpt.map(IndiaExportStatisticsEntity::getIndiaSupplierRank).orElse(null))
                    .mfnTariffPercent(mfnTariff)
                    .preferentialTariffPercent(tariffOpt.map(TariffStatisticsEntity::getPreferentialTariffPercent).orElse(0.0))
                    .appliedTariffPercent(tariffOpt.map(TariffStatisticsEntity::getAppliedTariffPercent).orElse(mfnTariff))
                    .dutyFree(tariffOpt.map(TariffStatisticsEntity::getDutyFree).orElse(false))
                    .supplierCountryCount(compOpt.map(CompetitionStatisticsEntity::getSupplierCountryCount).orElse(15))
                    .topSupplierCountry(compOpt.map(CompetitionStatisticsEntity::getTopSupplierCountry).orElse("China"))
                    .topSupplierSharePercent(compOpt.map(CompetitionStatisticsEntity::getTopSupplierSharePercent).orElse(40.0))
                    .supplierHhi(hhi)
                    .gdpUsd(econOpt.map(CountryEconomicIndicatorsEntity::getGdpUsd).orElse(null))
                    .gdpPerCapitaUsd(econOpt.map(CountryEconomicIndicatorsEntity::getGdpPerCapitaUsd).orElse(null))
                    .gdpGrowthPercent(econOpt.map(CountryEconomicIndicatorsEntity::getGdpGrowthPercent).orElse(null))
                    .population(econOpt.map(CountryEconomicIndicatorsEntity::getPopulation).orElse(null))
                    .marketDemandScore(demandScore)
                    .marketGrowthScore(growthScore)
                    .indiaExportScore(exportScore)
                    .competitionScore(competitionScore)
                    .tariffScore(tariffScore)
                    .economicStrengthScore(econScore)
                    .complianceScore(complianceScore)
                    .compositeRecommendationScore(compositeScore)
                    .tradeSource(imp.getSource())
                    .tariffSource(tariffOpt.map(TariffStatisticsEntity::getSource).orElse("WTO TAO"))
                    .economicSource(econOpt.map(CountryEconomicIndicatorsEntity::getSource).orElse("World Bank WDI"))
                    .dataTimestamp(LocalDateTime.now())
                    .build();

            vectors.add(vector);
        }

        return RecommendationDatasetMatrixDto.builder()
                .totalVectorsGenerated(vectors.size())
                .uniqueHs6Count(hs6Set.size())
                .targetCountriesCount(countrySet.size())
                .featureVectors(vectors)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
