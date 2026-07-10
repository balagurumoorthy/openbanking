package com.balabank.openbanking.balabank.aisp.dto;

import com.balabank.openbanking.common.dto.Money;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/** OBIE OBOffer1 (subset). */
public record OfferDto(
        @JsonProperty("AccountId") String accountId,
        @JsonProperty("OfferId") String offerId,
        @JsonProperty("OfferType") String offerType,
        @JsonProperty("Description") String description,
        @JsonProperty("Amount") Money amount,
        @JsonProperty("StartDateTime") OffsetDateTime startDateTime,
        @JsonProperty("EndDateTime") OffsetDateTime endDateTime) {
}
