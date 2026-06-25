package com.balabank.openbanking.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** OBIE OBActiveOrHistoricCurrencyAndAmount — {@code Amount} is a string per the standard. */
public record Money(
        @JsonProperty("Amount") @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        @JsonProperty("Currency") String currency) {
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }
}
