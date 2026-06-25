package com.balabank.openbanking.common.dto;

import java.time.OffsetDateTime;

/** OBIE OBTransaction6 (subset). */
public record TransactionDto(
        String transactionId,
        String accountId,
        String creditDebitIndicator, // Credit | Debit
        String status,               // Booked | Pending
        Money amount,
        OffsetDateTime bookingDateTime,
        String transactionInformation) {
}
