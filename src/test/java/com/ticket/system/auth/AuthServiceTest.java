package com.ticket.system.auth;

import com.ticket.system.auth.dto.LoginRequest;
import com.ticket.system.auth.dto.RegisterRequest;
import com.ticket.system.auth.dto.UserResponse;
import com.ticket.system.common.exception.ConflictException;
import com.ticket.system.common.exception.UnauthorizedException;
import com.ticket.system.security.JwtService;
import com.ticket.system.user.entity.User;
import com.ticket.system.user.enums.Role;
import com.ticket.system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User existingUser() {
        User user = User.builder()
                .email("jane@test.com")
                .password("hashed-password")
                .role(Role.USER)
                .build();
        user.setId(5L);
        return user;
    }

    // ---- register ----

    @Test
    void register_newEmail_savesAUserWithHashedPassword() {
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret#123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("jane@test.com", "Secret#123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.email()).isEqualTo("jane@test.com");
    }

    @Test
    void register_trimsAndLowercasesTheEmail() {
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret#123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("  Jane@TEST.com ", "Secret#123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@test.com");
    }

    @Test
    void register_duplicateEmail_throwsConflictAndSavesNothing() {
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("jane@test.com", "Secret#123")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    // ---- login ----

    @Test
    void login_unknownEmail_throwsUnauthorizedAndSetsNoCookie() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@test.com", "x"), response))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    @Test
    void login_wrongPassword_throwsUnauthorizedAndSetsNoCookie() {
        when(userRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@test.com", "wrong"), response))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    @Test
    void login_success_setsAnHttpOnlyCookieAndReturnsTheUser() {
        User user = existingUser();
        when(userRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret#123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-value");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserResponse result = authService.login(new LoginRequest("Jane@Test.com", "Secret#123"), response);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.role()).isEqualTo(Role.USER);
        assertThat(response.getHeader("Set-Cookie"))
                .contains("token=jwt-value")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Path=/")
                .contains("Max-Age=3600");
    }

    // ---- logout ----

    @Test
    void logout_clearsTheCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authService.logout(response);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("token=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
    }
}