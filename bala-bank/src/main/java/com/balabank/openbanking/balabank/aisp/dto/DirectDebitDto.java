package com.balabank.openbanking.balabank.aisp.dto;

import com.balabank.openbanking.common.dto.Money;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/** OBIE OBDirectDebit3 (subset). */
public record DirectDebitDto(
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("DirectDebitId") String directDebitId,
        @JsonProperty("Frequency") String frequency,
        @JsonProperty("DirectDebitStatusCode") String directDebitStatusCode,
        @JsonProperty("PreviousPaymentDateTime") OffsetDateTime previousPaymentDateTime,
        @JsonProperty("PreviousPaymentAmount") Money previousPaymentAmount,
        @JsonProperty("Name") String name) {
}
