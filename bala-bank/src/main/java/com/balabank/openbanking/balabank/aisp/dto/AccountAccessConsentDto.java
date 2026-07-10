package com.balabank.openbanking.balabank.aisp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

/** OBIE OBReadConsentResponse1 (subset) — ASPSP-side account-access-consent record. */
public record AccountAccessConsentDto(
        @JsonProperty("ConsentId") String consentId,
        @JsonProperty("CreationDateTime") OffsetDateTime creationDateTime,
        @JsonProperty("Status") String status,
        @JsonProperty("StatusUpdateDateTime") OffsetDateTime statusUpdateDateTime,
        @JsonProperty("Permissions") List<String> permissions,
        @JsonProperty("ExpirationDateTime") OffsetDateTime expirationDateTime,
        @JsonProperty("TransactionFromDateTime") OffsetDateTime transactionFromDateTime,
        @JsonProperty("TransactionToDateTime") OffsetDateTime transactionToDateTime) {
}
