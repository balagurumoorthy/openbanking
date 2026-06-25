package com.balabank.openbanking.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OBIE error model (OBErrorResponse1). {@code Code} is the HTTP-level summary,
 * each {@code Errors[]} entry carries an OBIE {@code ErrorCode}.
 */
public record ObError(
        @JsonProperty("Code") String code,
        @JsonProperty("Id") String id,
        @JsonProperty("Message") String message,
        @JsonProperty("Errors") List<Detail> errors) {

    public record Detail(
            @JsonProperty("ErrorCode") String errorCode,
            @JsonProperty("Message") String message,
            @JsonProperty("Path") String path) {
    }

    public static ObError of(String httpCode, String errorCode, String message) {
        return new ObError(httpCode, java.util.UUID.randomUUID().toString(), message,
                List.of(new Detail(errorCode, message, null)));
    }
}
