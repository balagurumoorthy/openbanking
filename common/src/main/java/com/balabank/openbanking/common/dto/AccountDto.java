package com.balabank.openbanking.common.dto;

/** OBIE OBAccount6 (subset): an account exposed to a consented TPP. */
public record AccountDto(
        String accountId,
        String status,
        String currency,
        String accountType,       // Personal | Business
        String accountSubType,    // CurrentAccount | Savings | ...
        String nickname,
        String identification,    // sort code + account number
        String name) {
}
