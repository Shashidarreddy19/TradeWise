package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.regulation.service.GlobalDatabaseSeedService;
import com.cbec.ai.pipeline.repository.HsMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/TradeData?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    "spring.datasource.username=root",
    "spring.datasource.password=root",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.sql.init.mode=never",
    "cbec.test.mode=false"
})
public class IngestRealProductionDatabaseTest {

    @Autowired private GlobalDatabaseSeedService seedService;
    @Autowired private HsMasterRepository hsMasterRepository;

    @Test
    @DisplayName("Execute Real Production Ingestion directly into MySQL TradeData")
    public void ingestRealProductionDataIntoMySql() {
        log.info("Starting direct Real Production Ingestion into MySQL TradeData...");
        seedService.seedAllCountriesInDatabase();
        log.info("Finished Ingestion. Total hs_master count in MySQL TradeData: {}", hsMasterRepository.count());
    }
}
