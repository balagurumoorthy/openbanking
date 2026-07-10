package com.balabank.openbanking.balabank.aisp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** OBIE OBParty2 (subset). */
public record PartyDto(
        @JsonProperty("PartyId") String partyId,
        @JsonProperty("PartyNumber") String partyNumber,
        @JsonProperty("FullLegalName") String fullLegalName,
        @JsonProperty("PartyType") String partyType,
        @JsonProperty("EmailAddress") String emailAddress,
        @JsonProperty("PhoneNumber") String phoneNumber) {
}
