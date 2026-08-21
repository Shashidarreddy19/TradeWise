package com.cbec.ai.pipeline.trade.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class TradeFeatureEngineeringService {

    private final TradeImportStatisticsRepository importRepository;
    private final IndiaExportStatisticsRepository exportRepository;
    private final TariffStatisticsRepository tariffRepository;
    private final CompetitionStatisticsRepository competitionRepository;
    private final CountryEconomicIndicatorsRepository economicRepository;
    private final CountryRiskIndicatorsRepository riskRepository;
    private final MarketAccessIndicatorsRepository accessRepository;
    private final CurrencyIndicatorsRepository currencyRepository;
    private final CbecCountryRecommendationDatasetRepository datasetRepository;

    private final RegulationMasterRepository regulationMasterRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationProcedureRepository procedureRepository;

    public TradeFeatureEngineeringService(
            TradeImportStatisticsRepository importRepository,
            IndiaExportStatisticsRepository exportRepository,
            TariffStatisticsRepository tariffRepository,
            CompetitionStatisticsRepository competitionRepository,
            CountryEconomicIndicatorsRepository economicRepository,
            CountryRiskIndicatorsRepository riskRepository,
            MarketAccessIndicatorsRepository accessRepository,
            CurrencyIndicatorsRepository currencyRepository,
            CbecCountryRecommendationDatasetRepository datasetRepository,
            RegulationMasterRepository regulationMasterRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationProcedureRepository procedureRepository) {
        this.importRepository = importRepository;
        this.exportRepository = exportRepository;
        this.tariffRepository = tariffRepository;
        this.competitionRepository = competitionRepository;
        this.economicRepository = economicRepository;
        this.riskRepository = riskRepository;
        this.accessRepository = accessRepository;
        this.currencyRepository = currencyRepository;
        this.datasetRepository = datasetRepository;
        this.regulationMasterRepository = regulationMasterRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
        this.restrictionRepository = restrictionRepository;
        this.labelingRepository = labelingRepository;
        this.procedureRepository = procedureRepository;
    }

    @Transactional
    public long generateAndPersistRecommendationDataset() {
        log.info("Building cbec_country_recommendation_dataset ML training matrix...");
        List<TradeImportStatisticsEntity> imports = importRepository.findAll();
        long persistedCount = 0;

        for (TradeImportStatisticsEntity imp : imports) {
            String hs6 = imp.getHs6();
            String country = imp.getDestinationCountry();
            Integer year = imp.getYear();

            Optional<CbecCountryRecommendationDatasetEntity> existingOpt = datasetRepository.findByHs6AndDestinationCountryAndYear(hs6, country, year);

            Optional<IndiaExportStatisticsEntity> expOpt = exportRepository.findByHs6AndDestinationCountryAndYear(hs6, country, year);
            Optional<TariffStatisticsEntity> tariffOpt = tariffRepository.findByHs6AndDestinationCountryAndYear(hs6, country, year);
            Optional<CompetitionStatisticsEntity> compOpt = competitionRepository.findByHs6AndDestinationCountryAndYear(hs6, country, year);
            Optional<CountryEconomicIndicatorsEntity> econOpt = economicRepository.findByCountryAndYear(country, year);
            Optional<CountryRiskIndicatorsEntity> riskOpt = riskRepository.findByCountryAndYear(country, year);
            Optional<MarketAccessIndicatorsEntity> accessOpt = accessRepository.findByCountryAndYear(country, year);
            Optional<CurrencyIndicatorsEntity> currOpt = currencyRepository.findByCountryAndYear(country, year);

            // Integrate existing regulatory child table counts
            List<RegulationMasterEntity> countryRegs = regulationMasterRepository.findByCountry(country);
            List<Long> regIds = countryRegs.stream().map(RegulationMasterEntity::getId).toList();

            int docCount = regIds.isEmpty() ? 15 : documentRepository.findByRegulationIdIn(regIds).size();
            int certCount = regIds.isEmpty() ? 15 : certificationRepository.findByRegulationIdIn(regIds).size();
            int restrCount = regIds.isEmpty() ? 8 : restrictionRepository.findByRegulationIdIn(regIds).size();
            int labelCount = regIds.isEmpty() ? 15 : labelingRepository.findByRegulationIdIn(regIds).size();
            int procCount = regIds.isEmpty() ? 8 : procedureRepository.findByRegulationIdIn(regIds).size();

            int regBurdenScore = Math.min(100, docCount + certCount + (restrCount * 2) + labelCount + procCount);
            int complianceScore = Math.max(0, 100 - regBurdenScore);

            // Trade & Export Features
            double impVal = imp.getImportValueUsd() != null ? imp.getImportValueUsd() : 0.0;
            double impGrowth = imp.getImportGrowthPercent() != null ? imp.getImportGrowthPercent() : 0.0;
            double impCagr = imp.getThreeYearCagr() != null ? imp.getThreeYearCagr() : 0.0;
            int mktSizeScore = Math.min(100, (int) Math.round((impVal / 50_000_000.0) * 100.0));

            double expVal = expOpt.map(IndiaExportStatisticsEntity::getIndiaExportValueUsd).orElse(0.0);
            double mktShare = expOpt.map(IndiaExportStatisticsEntity::getIndiaMarketSharePercent).orElse(0.0);
            double expGrowth = expOpt.map(IndiaExportStatisticsEntity::getExportGrowthPercent).orElse(0.0);
            double expCagr = expOpt.map(IndiaExportStatisticsEntity::getThreeYearCagr).orElse(0.0);

            // Tariffs
            double mfn = tariffOpt.map(TariffStatisticsEntity::getMfnTariffPercent).orElse(5.0);
            double pref = tariffOpt.map(TariffStatisticsEntity::getPreferentialTariffPercent).orElse(mfn);
            double effTariff = Math.min(mfn, pref);
            int tariffAdvantage = Math.max(0, 100 - (int) Math.round(effTariff * 10.0));

            // Competition HHI
            double hhi = compOpt.map(CompetitionStatisticsEntity::getSupplierHhi).orElse(2500.0);
            double topShare = compOpt.map(CompetitionStatisticsEntity::getTopSupplierSharePercent).orElse(40.0);
            int numSuppliers = compOpt.map(CompetitionStatisticsEntity::getSupplierCountryCount).orElse(15);
            int compScore = Math.max(0, 100 - (int) Math.round((hhi / 10000.0) * 100.0));

            // Economic & Risk & Access & Currency
            double gdp = econOpt.map(CountryEconomicIndicatorsEntity::getGdpUsd).orElse(1_000_000_000_000.0);
            double gdpPerCapita = econOpt.map(CountryEconomicIndicatorsEntity::getGdpPerCapitaUsd).orElse(30_000.0);
            long pop = econOpt.map(CountryEconomicIndicatorsEntity::getPopulation).orElse(30_000_000L);
            double gdpGrowth = econOpt.map(CountryEconomicIndicatorsEntity::getGdpGrowthPercent).orElse(2.5);

            int riskScoreVal = riskOpt.map(CountryRiskIndicatorsEntity::getCountryRiskScore).orElse(85);
            int accessScoreVal = accessOpt.map(MarketAccessIndicatorsEntity::getMarketAccessScore).orElse(75);
            boolean fta = accessOpt.map(MarketAccessIndicatorsEntity::getIndiaFtaIndicator).orElse(false);
            boolean pta = accessOpt.map(MarketAccessIndicatorsEntity::getPtaIndicator).orElse(false);

            double exRate = currOpt.map(CurrencyIndicatorsEntity::getExchangeRateUsd).orElse(1.0);
            double exVol = currOpt.map(CurrencyIndicatorsEntity::getCurrencyVolatilityPercent).orElse(3.5);

            // Derived Scores
            int demandScore = mktSizeScore;
            int growthScore = Math.min(100, Math.max(0, (int) Math.round(impGrowth * 10.0)));
            int tariffScore = tariffAdvantage;
            int econScore = Math.min(100, (int) Math.round((gdpPerCapita / 80000.0) * 100.0));

            int potentialScore = (int) Math.round(
                    (demandScore * 0.20) +
                    (growthScore * 0.15) +
                    (tariffScore * 0.15) +
                    (compScore * 0.10) +
                    (complianceScore * 0.15) +
                    (econScore * 0.10) +
                    (riskScoreVal * 0.08) +
                    (accessScoreVal * 0.07)
            );

            CbecCountryRecommendationDatasetEntity datasetRow = existingOpt.orElseGet(CbecCountryRecommendationDatasetEntity::new);
            datasetRow.setHs6(hs6);
            datasetRow.setDestinationCountry(country);
            datasetRow.setYear(year);

            datasetRow.setImportValueUsd(impVal);
            datasetRow.setImportQuantity(impVal / 25.0);
            datasetRow.setImportGrowthRate(impGrowth);
            datasetRow.setImportCagr(impCagr);
            datasetRow.setMarketSizeScore(mktSizeScore);

            datasetRow.setIndiaExportValueUsd(expVal);
            datasetRow.setIndiaExportQuantity(expVal / 20.0);
            datasetRow.setIndiaMarketSharePercent(mktShare);
            datasetRow.setIndiaExportGrowthRate(expGrowth);
            datasetRow.setIndiaExportCagr(expCagr);
            datasetRow.setIndiaHistoricalExportTrend(expGrowth > 0 ? "EXPANDING" : "STABLE");

            datasetRow.setMfnTariffPercent(mfn);
            datasetRow.setPreferentialTariffPercent(pref);
            datasetRow.setEffectiveTariffPercent(effTariff);
            datasetRow.setTariffAdvantageScore(tariffAdvantage);

            datasetRow.setSupplierHhi(hhi);
            datasetRow.setTopSupplierShare(topShare);
            datasetRow.setNumberOfSupplierCountries(numSuppliers);
            datasetRow.setCompetitionScore(compScore);

            datasetRow.setComplianceScore(complianceScore);
            datasetRow.setDocumentCount(docCount);
            datasetRow.setCertificationCount(certCount);
            datasetRow.setRestrictionCount(restrCount);
            datasetRow.setLabelingRequirementCount(labelCount);
            datasetRow.setProcedureCount(procCount);
            datasetRow.setRegulatoryBurdenScore(regBurdenScore);

            datasetRow.setGdpUsd(gdp);
            datasetRow.setGdpPerCapitaUsd(gdpPerCapita);
            datasetRow.setPopulation(pop);
            datasetRow.setGdpGrowth(gdpGrowth);

            datasetRow.setCountryRiskScore(riskScoreVal);
            datasetRow.setPoliticalEconomicRiskIndicator(riskOpt.map(CountryRiskIndicatorsEntity::getPoliticalRiskIndicator).orElse("LOW"));
            datasetRow.setTradeRiskIndicator(riskOpt.map(CountryRiskIndicatorsEntity::getTradeRiskIndicator).orElse("STABLE"));

            datasetRow.setMarketAccessScore(accessScoreVal);
            datasetRow.setPreferentialTradeAgreementIndicator(pta);
            datasetRow.setIndiaTradeAgreementIndicator(fta);

            datasetRow.setExchangeRate(exRate);
            datasetRow.setExchangeRateVolatility(exVol);

            datasetRow.setDemandScore(demandScore);
            datasetRow.setGrowthScore(growthScore);
            datasetRow.setTariffScore(tariffScore);
            datasetRow.setComplianceScoreNormalized(complianceScore);
            datasetRow.setEconomicScore(econScore);
            datasetRow.setRiskScore(riskScoreVal);
            datasetRow.setAccessibilityScore(accessScoreVal);
            datasetRow.setIndiaPotentialScore(potentialScore);

            datasetRow.setTradeSource(imp.getSource());
            datasetRow.setTariffSource(tariffOpt.map(TariffStatisticsEntity::getSource).orElse("WTO TAO"));
            datasetRow.setEconomicSource(econOpt.map(CountryEconomicIndicatorsEntity::getSource).orElse("World Bank WDI"));
            datasetRow.setRiskSource("OECD / PRS ICRG");
            datasetRow.setDatasetStatus("DATA_AVAILABLE");

            datasetRepository.save(datasetRow);
            persistedCount++;
        }

        return persistedCount;
    }
}
