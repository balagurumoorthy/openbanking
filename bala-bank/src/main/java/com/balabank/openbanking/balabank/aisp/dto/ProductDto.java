package com.balabank.openbanking.balabank.aisp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** OBIE OBProduct2 (subset). */
public record ProductDto(
        @JsonProperty("ProductId") String productId,
        @JsonProperty("ProductName") String productName,
        @JsonProperty("ProductType") String productType,
        @JsonProperty("MarketingStateId") String marketingStateId) {
}
