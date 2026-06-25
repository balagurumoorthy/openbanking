package com.balabank.openbanking.common.dto;

import java.util.Map;

/**
 * OBIE response envelope: {@code Data}, plus standard {@code Links} (pagination/self)
 * and {@code Meta} (total pages) blocks present on every Read/Write response.
 */
public record ObResponse<T>(T data, Map<String, String> links, Map<String, Object> meta) {

    public static <T> ObResponse<T> of(T data, String self) {
        return new ObResponse<>(data, Map.of("Self", self), Map.of("TotalPages", 1));
    }
}
