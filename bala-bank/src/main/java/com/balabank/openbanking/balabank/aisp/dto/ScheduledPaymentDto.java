package com.balabank.openbanking.balabank.aisp.dto;

import com.balabank.openbanking.common.dto.Money;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/** OBIE OBScheduledPayment3 (subset). */
public record ScheduledPaymentDto(
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("ScheduledPaymentId") String scheduledPaymentId,
        @JsonProperty("ScheduledPaymentDateTime") OffsetDateTime scheduledPaymentDateTime,
        @JsonProperty("ScheduledType") String scheduledType,
        @JsonProperty("InstructedAmount") Money instructedAmount,
        @JsonProperty("CreditorAccount") CreditorAccount creditorAccount,
        @JsonProperty("Reference") String reference) {

    public record CreditorAccount(
            @JsonProperty("SchemeName") String schemeName,
            @JsonProperty("Identification") String identification,
            @JsonProperty("Name") String name) {
    }
}
