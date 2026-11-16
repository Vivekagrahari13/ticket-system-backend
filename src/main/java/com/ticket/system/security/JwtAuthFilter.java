package com.ticket.system.security;


import com.ticket.system.user.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.ticket.system.common.logging.RequestLoggingFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "token";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = readToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.validateToken(token);
                CustomUserDetails principal = new CustomUserDetails(
                        Long.valueOf(claims.getSubject()),
                        claims.get("email", String.class),
                        Role.valueOf(claims.get("role", String.class)));

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                MDC.put(RequestLoggingFilter.USER_ID_KEY, String.valueOf(principal.getId()));
            } catch (JwtException | IllegalArgumentException e) {
                // An expired login is normal, so debug only. The request stays unauthenticated and gets a 401.
                log.debug("Authentication rejected: {}", e.getClass().getSimpleName());
            }
        }
        chain.doFilter(request, response);
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}