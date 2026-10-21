package com.ticket.system.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> fieldErrors, Instant timestamp) {

    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(status, message, fieldErrors, Instant.now());
    }
}
