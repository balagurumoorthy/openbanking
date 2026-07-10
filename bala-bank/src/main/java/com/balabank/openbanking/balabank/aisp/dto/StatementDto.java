package com.balabank.openbanking.balabank.aisp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/** OBIE OBStatement2 (subset). */
public record StatementDto(
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("StatementId") String statementId,
        @JsonProperty("StatementType") String statementType,
        @JsonProperty("StartDateTime") OffsetDateTime startDateTime,
        @JsonProperty("EndDateTime") OffsetDateTime endDateTime,
        @JsonProperty("CreationDateTime") OffsetDateTime creationDateTime) {
}
