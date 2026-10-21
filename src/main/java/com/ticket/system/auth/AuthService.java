package com.ticket.system.auth;

import com.ticket.system.auth.dto.LoginRequest;
import com.ticket.system.auth.dto.RegisterRequest;
import com.ticket.system.auth.dto.UserResponse;
import com.ticket.system.common.exception.ConflictException;
import com.ticket.system.common.exception.UnauthorizedException;
import com.ticket.system.security.JwtAuthFilter;
import com.ticket.system.security.JwtService;
import com.ticket.system.user.entity.User;
import com.ticket.system.user.enums.Role;
import com.ticket.system.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    public UserResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.email().trim().toLowerCase();

        // Same message for unknown email and wrong password, so callers can't tell which emails are registered
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email, user not registered"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        ResponseCookie cookie= ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, jwtService.
                generateToken(user))
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(jwtService.getExpirationSeconds())).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
