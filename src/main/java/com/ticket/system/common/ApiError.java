package com.ticket.system.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ticket.system.common.logging.RequestLoggingFilter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> fieldErrors, Instant timestamp, String requestId) {

    // The request id lets a user quote a reference that matches the server logs
    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(status, message, fieldErrors, Instant.now(),
                MDC.get(RequestLoggingFilter.REQUEST_ID_KEY));
    }
}
