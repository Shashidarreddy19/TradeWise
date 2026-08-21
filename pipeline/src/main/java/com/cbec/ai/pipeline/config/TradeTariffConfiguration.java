package com.cbec.ai.pipeline.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class TradeTariffConfiguration {

    @Value("${uk.trade.oauth.token-url:https://auth.id.trade-tariff.service.gov.uk/oauth2/token}")
    private String tokenUrl = "https://auth.id.trade-tariff.service.gov.uk/oauth2/token";

    @Value("${uk.trade.oauth.client-id:7du8toe6mq733ih89kqnvbmdov}")
    private String clientId = "7du8toe6mq733ih89kqnvbmdov";

    @Value("${uk.trade.oauth.client-secret:k4s380umf3r9tlfkpoblge6g0l80q8ekdji6p3c478qg0up8juk}")
    private String clientSecret = "k4s380umf3r9tlfkpoblge6g0l80q8ekdji6p3c478qg0up8juk";

    @Value("${uk.trade.api.base-url:https://www.trade-tariff.service.gov.uk/api/v2}")
    private String apiBaseUrl = "https://www.trade-tariff.service.gov.uk/api/v2";

    @Value("${download.directory:downloads}")
    private String downloadDirectory = "downloads";
}
