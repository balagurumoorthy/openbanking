package com.balabank.openbanking.common.dto;

import java.time.OffsetDateTime;

/** OBIE OBReadBalance1 entry (subset). */
public record BalanceDto(
        String accountId,
        Money amount,
        String creditDebitIndicator, // Credit | Debit
        String type,                 // InterimAvailable | ClosingBooked | ...
        OffsetDateTime dateTime) {
}
