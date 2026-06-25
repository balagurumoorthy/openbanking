package com.balabank.openbanking.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * OBIE response envelope: {@code Data}, plus standard {@code Links} (pagination/self)
 * and {@code Meta} (total pages) blocks present on every Read/Write response. Per the
 * standard, {@code Data} wraps the resource array under its named key, e.g.
 * {@code {"Data":{"Account":[...]},"Links":{...},"Meta":{...}}}.
 */
public record ObResponse<T>(
        @JsonProperty("Data") T data,
        @JsonProperty("Links") Map<String, String> links,
        @JsonProperty("Meta") Map<String, Object> meta) {

    public static <T> ObResponse<T> of(T data, String self) {
        return new ObResponse<>(data, Map.of("Self", self), Map.of("TotalPages", 1));
    }

    /** Wrap a resource array under its OBIE named key (e.g. {@code "Account"}, {@code "Balance"}). */
    public static ObResponse<Map<String, Object>> ofResource(String resourceName, Object items, String self) {
        return new ObResponse<>(Map.of(resourceName, items), Map.of("Self", self), Map.of("TotalPages", 1));
    }
}
