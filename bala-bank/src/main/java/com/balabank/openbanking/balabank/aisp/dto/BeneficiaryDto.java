package com.balabank.openbanking.balabank.aisp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** OBIE OBBeneficiary5 (subset). */
public record BeneficiaryDto(
        @JsonProperty("BeneficiaryId") String beneficiaryId,
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("Reference") String reference,
        @JsonProperty("CreditorAccount") CreditorAccount creditorAccount) {

    public record CreditorAccount(
            @JsonProperty("SchemeName") String schemeName,
            @JsonProperty("Identification") String identification,
            @JsonProperty("Name") String name,
            @JsonProperty("SecondaryIdentification") String secondaryIdentification) {
    }
}
