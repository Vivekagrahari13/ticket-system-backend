package com.ticket.system.config;

import com.ticket.system.user.entity.User;
import com.ticket.system.user.enums.Role;
import com.ticket.system.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class AdminSeeder {

    @Bean
    CommandLineRunner seedAdmin(UserRepository repo,
                                PasswordEncoder encoder,
                                @Value("${app.admin-email:}") String email,
                                @Value("${app.admin-password:}") String password) {
        return args -> {
            if (email.isBlank() || password.isBlank()) {
                log.warn("Admin seed skipped: set ADMIN_EMAIL and ADMIN_PASSWORD environment variables");
                return;
            }
            String normalized = email.trim().toLowerCase();
            if (repo.existsByEmail(normalized)) {
                log.info("Admin seed skipped: {} already exists", normalized);
                return;
            }
            repo.save(User.builder()
                    .email(normalized)
                    .password(encoder.encode(password))
                    .role(Role.ADMIN)
                    .build());
            log.info("Admin user created: {}", normalized);
        };
    }
}
