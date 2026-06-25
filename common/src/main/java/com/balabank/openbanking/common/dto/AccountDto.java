package com.balabank.openbanking.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** OBIE OBAccount6 (subset): an account exposed to a consented TPP. */
public record AccountDto(
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("Status") String status,
        @JsonProperty("Currency") String currency,
        @JsonProperty("AccountType") String accountType,       // Personal | Business
        @JsonProperty("AccountSubType") String accountSubType, // CurrentAccount | Savings | ...
        @JsonProperty("Nickname") String nickname,
        @JsonProperty("Account") List<Identifier> account) {

    /** OBIE OBCashAccount5 — the account-number scheme/identification/name. */
    public record Identifier(
            @JsonProperty("SchemeName") String schemeName,        // UK.OBIE.SortCodeAccountNumber
            @JsonProperty("Identification") String identification, // sort code + account number
            @JsonProperty("Name") String name) {
    }
}
