package com.trade.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.HashMap;

/**
 * Provides EntityManagerFactoryBuilder when Hibernate auto-config backs off
 * because this app defines two explicit LocalContainerEntityManagerFactory beans.
 */
@Configuration
public class JpaBuilderConfig {

    @Bean
    @ConditionalOnMissingBean(EntityManagerFactoryBuilder.class)
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(false);
        vendorAdapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
        return new EntityManagerFactoryBuilder(vendorAdapter, new HashMap<>(), null);
    }
}
