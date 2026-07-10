package com.balabank.openbanking.balabank.aisp.dto;

import com.balabank.openbanking.common.dto.Money;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/** OBIE OBStandingOrder6 (subset). */
public record StandingOrderDto(
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("StandingOrderId") String standingOrderId,
        @JsonProperty("Frequency") String frequency,
        @JsonProperty("StandingOrderStatusCode") String standingOrderStatusCode,
        @JsonProperty("NextPaymentDateTime") OffsetDateTime nextPaymentDateTime,
        @JsonProperty("NextPaymentAmount") Money nextPaymentAmount,
        @JsonProperty("FinalPaymentAmount") Money finalPaymentAmount,
        @JsonProperty("CreditorAccount") CreditorAccount creditorAccount) {

    public record CreditorAccount(
            @JsonProperty("SchemeName") String schemeName,
            @JsonProperty("Identification") String identification,
            @JsonProperty("Name") String name) {
    }
}
