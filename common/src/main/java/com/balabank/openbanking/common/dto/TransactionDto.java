package com.balabank.openbanking.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/** OBIE OBTransaction6 (subset). */
public record TransactionDto(
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("CreditDebitIndicator") String creditDebitIndicator, // Credit | Debit
        @JsonProperty("Status") String status,               // Booked | Pending
        @JsonProperty("Amount") Money amount,
        @JsonProperty("BookingDateTime") OffsetDateTime bookingDateTime,
        @JsonProperty("TransactionInformation") String transactionInformation) {
}
