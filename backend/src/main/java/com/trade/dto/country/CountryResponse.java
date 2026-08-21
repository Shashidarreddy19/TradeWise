package com.trade.dto.country;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a country record.
 */
@Data
@Builder
public class CountryResponse {

    private Long id;
    private String code;
    private String name;
    private String currency;
}
