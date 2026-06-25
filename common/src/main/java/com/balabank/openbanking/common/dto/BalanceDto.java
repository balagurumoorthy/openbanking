package com.balabank.openbanking.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/** OBIE OBReadBalance1 entry (subset). */
public record BalanceDto(
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("Amount") Money amount,
        @JsonProperty("CreditDebitIndicator") String creditDebitIndicator, // Credit | Debit
        @JsonProperty("Type") String type,                 // InterimAvailable | ClosingBooked | ...
        @JsonProperty("DateTime") OffsetDateTime dateTime) {
}
