package com.balabank.openbanking.common.dto;

import java.math.BigDecimal;

/** OBIE amount/currency pair. */
public record Money(BigDecimal amount, String currency) {
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }
}
