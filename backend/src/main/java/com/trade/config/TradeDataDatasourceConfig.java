package com.trade.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Secondary datasource configuration for the TradeData database.
 * READ-ONLY from the main backend — the pipeline handles writes.
 * Handles: HS codes, regulations, compliance, evidence, official sources.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.trade.regulatory.repository",
    entityManagerFactoryRef = "tradeDataEntityManagerFactory",
    transactionManagerRef = "tradeDataTransactionManager"
)
public class TradeDataDatasourceConfig {

    @Value("${tradedata.jpa.ddl-auto:update}")
    private String ddlAuto;

    @Bean
    @ConfigurationProperties("tradedata.datasource")
    public DataSourceProperties tradeDataDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource tradeDataDataSource() {
        return tradeDataDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean tradeDataEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        Map<String, Object> props = new HashMap<>();
        // "update" lets a fresh deploy start without the pipeline having run.
        // Set TRADEDATA_DDL_AUTO=none in production once TradeData is pipeline-managed.
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

        return builder
                .dataSource(tradeDataDataSource())
                .packages("com.trade.regulatory.entity")
                .persistenceUnit("tradedata")
                .properties(props)
                .build();
    }

    @Bean
    public PlatformTransactionManager tradeDataTransactionManager(
            @Qualifier("tradeDataEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
