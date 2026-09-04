package com.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Trade Platform Backend.
 * Provides export intelligence APIs for Indian SMEs.
 */
@SpringBootApplication
public class TradeBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(TradeBackendApplication.class, args);
    }
}