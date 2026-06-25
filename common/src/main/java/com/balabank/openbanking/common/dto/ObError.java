package com.balabank.openbanking.common.dto;

import java.util.List;

/**
 * OBIE error model (OBErrorResponse1). {@code code} is the HTTP-level summary,
 * each {@code errors[]} entry carries an OBIE {@code errorCode}.
 */
public record ObError(String code, String id, String message, List<Detail> errors) {

    public record Detail(String errorCode, String message, String path) {}

    public static ObError of(String httpCode, String errorCode, String message) {
        return new ObError(httpCode, java.util.UUID.randomUUID().toString(), message,
                List.of(new Detail(errorCode, message, null)));
    }
}
