package com.ticket.system.auth;

import com.ticket.system.auth.dto.LoginRequest;
import com.ticket.system.auth.dto.RegisterRequest;
import com.ticket.system.auth.dto.UserResponse;
import com.ticket.system.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid RegisterRequest registerRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody @Valid LoginRequest loginRequest, HttpServletResponse httpServletResponse){
        return ResponseEntity.ok(authService.login(loginRequest, httpServletResponse));

    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails me) {
        return ResponseEntity.ok(new UserResponse(me.getId(), me.getEmail(), me.getRole()));
    }
}
