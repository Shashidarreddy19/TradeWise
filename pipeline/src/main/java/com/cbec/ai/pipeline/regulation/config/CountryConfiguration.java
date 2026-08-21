package com.cbec.ai.pipeline.regulation.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
public class CountryConfiguration {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryMetadata {
        private String country;
        private String countryCode;
        private String authority;
        private String downloadDirectory;
        private String baseUrl;
        private String supportedFormats;
        private String htmlSelectors;
        private boolean pdfSupported;
        private boolean apiSupported;
        private boolean xmlSupported;
        private boolean excelSupported;
        private boolean csvSupported;
        private String userAgent;
        private int timeout;
        private int retryCount;
    }

    private final Map<String, CountryMetadata> countryRegistry = new HashMap<>();

    public CountryConfiguration() {
        initRegistry();
    }

    private void initRegistry() {
        registerCountry(CountryMetadata.builder()
                .country("United Kingdom")
                .countryCode("GB")
                .authority("HMRC / GOV.UK")
                .downloadDirectory("downloads/UnitedKingdom/Regulations")
                .baseUrl("https://www.gov.uk/guidance/import-controls")
                .supportedFormats("HTML,JSON,PDF")
                .htmlSelectors("h1,h2,h3,h4,p,li,table tr")
                .pdfSupported(true).apiSupported(true).xmlSupported(true).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("United States")
                .countryCode("US")
                .authority("US Customs and Border Protection (CBP)")
                .downloadDirectory("downloads/UnitedStates/Regulations")
                .baseUrl("https://www.cbp.gov/trade/basic-import-export")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(true).xmlSupported(true).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("Canada")
                .countryCode("CA")
                .authority("Canada Border Services Agency (CBSA)")
                .downloadDirectory("downloads/Canada/Regulations")
                .baseUrl("https://www.cbsa-asfc.gc.ca/import/menu-eng.html")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("Australia")
                .countryCode("AU")
                .authority("Australian Border Force (ABF)")
                .downloadDirectory("downloads/Australia/Regulations")
                .baseUrl("https://www.abf.gov.au/importing-exporting-and-manufacturing/importing")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("Germany")
                .countryCode("DE")
                .authority("Zoll (German Customs)")
                .downloadDirectory("downloads/Germany/Regulations")
                .baseUrl("https://www.zoll.de/EN/Businesses/Customs-Procedures/customs-procedures.html")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("Netherlands")
                .countryCode("NL")
                .authority("Douane (Dutch Customs)")
                .downloadDirectory("downloads/Netherlands/Regulations")
                .baseUrl("https://www.belastingdienst.nl/wps/wcm/connect/bldcontenten/belastingdienst/customs/")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("Japan")
                .countryCode("JP")
                .authority("Japan Customs (Customs.go.jp)")
                .downloadDirectory("downloads/Japan/Regulations")
                .baseUrl("https://www.customs.go.jp/english/procedure/index.htm")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("South Korea")
                .countryCode("KR")
                .authority("Korea Customs Service (KCS)")
                .downloadDirectory("downloads/SouthKorea/Regulations")
                .baseUrl("https://www.customs.go.kr/english/main.do")
                .supportedFormats("HTML,XLSX,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(true).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("Hong Kong")
                .countryCode("HK")
                .authority("Hong Kong Customs and Excise Department")
                .downloadDirectory("downloads/HongKong/Regulations")
                .baseUrl("https://www.customs.gov.hk/en/customs_clearance/index.html")
                .supportedFormats("HTML,CSV,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(true)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("UAE")
                .countryCode("AE")
                .authority("UAE Federal Customs Authority (FCA)")
                .downloadDirectory("downloads/UAE/Regulations")
                .baseUrl("https://www.fca.gov.ae/en/services/customs-clearance")
                .supportedFormats("HTML,XLSX,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(true).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());

        registerCountry(CountryMetadata.builder()
                .country("India")
                .countryCode("IN")
                .authority("CBIC / DGFT India")
                .downloadDirectory("downloads/India/Regulations")
                .baseUrl("https://www.cbic.gov.in/")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build());
    }

    public void registerCountry(CountryMetadata metadata) {
        countryRegistry.put(metadata.getCountry().toLowerCase(), metadata);
        if (metadata.getCountryCode() != null) {
            countryRegistry.put(metadata.getCountryCode().toLowerCase(), metadata);
        }
    }

    public CountryMetadata getCountryMetadata(String country) {
        if (country == null) return null;
        CountryMetadata meta = countryRegistry.get(country.trim().toLowerCase());
        if (meta != null) return meta;

        // Fallback default metadata for unlisted countries
        return CountryMetadata.builder()
                .country(country)
                .countryCode(country.length() >= 2 ? country.substring(0, 2).toUpperCase() : "XX")
                .authority("Customs Authority")
                .downloadDirectory("downloads/" + country.replaceAll(" ", "") + "/Regulations")
                .baseUrl("https://www.customs.gov")
                .supportedFormats("HTML,PDF")
                .htmlSelectors("h1,h2,h3,p,li")
                .pdfSupported(true).apiSupported(false).xmlSupported(false).excelSupported(false).csvSupported(false)
                .userAgent("Mozilla/5.0")
                .timeout(60000).retryCount(3)
                .build();
    }
}
