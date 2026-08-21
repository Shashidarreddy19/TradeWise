package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.config.TradeTariffConfiguration;
import com.cbec.ai.pipeline.exception.OAuthAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OAuthTokenServiceTest {

    @Autowired
    private OAuthTokenService tokenService;

    @Autowired
    private TradeTariffConfiguration config;

    @BeforeEach
    void setUp() {
        tokenService.clearCacheForTesting();
    }

    @Test
    void testLiveOAuthTokenAcquisition() {
        assertDoesNotThrow(() -> {
            String token = tokenService.getAccessToken();
            assertNotNull(token);
            assertFalse(token.trim().isEmpty());
            assertTrue(token.length() > 20);

            // Test caching: Calling getAccessToken() again returns the same cached token without throwing
            String cachedToken = tokenService.getAccessToken();
            assertEquals(token, cachedToken);
        });
    }

    @Test
    void testForceRefreshToken() {
        String token1 = tokenService.getAccessToken();
        assertNotNull(token1);

        String token2 = tokenService.forceRefreshToken();
        assertNotNull(token2);
        assertFalse(token2.isEmpty());
    }

    @Test
    void testAuthenticationFailureHandling() {
        TradeTariffConfiguration invalidConfig = new TradeTariffConfiguration() {
            @Override
            public String getClientSecret() {
                return "INVALID_SECRET_123456789";
            }
        };

        OAuthTokenService invalidTokenService = new OAuthTokenService(invalidConfig);

        assertThrows(OAuthAuthenticationException.class, () -> {
            invalidTokenService.getAccessToken();
        });
    }
}
