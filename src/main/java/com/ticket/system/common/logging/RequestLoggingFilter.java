package com.ticket.system.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

// Gives every request an id that appears on every log line and in the response header,
// and writes one access log line per request. Runs before Spring Security.
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String USER_ID_KEY = "userId";

    // Accept a caller's id only if it is short and safe, so it cannot inject log lines
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            logAccess(request, response, millis);
            MDC.clear();
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, long millis) {
        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/favicon")) {
            return;
        }
        // Preflight requests are noise, so keep them at debug
        if ("OPTIONS".equals(request.getMethod())) {
            log.debug("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), millis);
        } else {
            log.info("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), millis);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && SAFE_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
