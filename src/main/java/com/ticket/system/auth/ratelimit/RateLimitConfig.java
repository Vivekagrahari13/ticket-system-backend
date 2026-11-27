package com.ticket.system.auth.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {
    @Bean
    public TokenBucketLimiter loginLimiter() {
        return new TokenBucketLimiter(5, Duration.ofMinutes(1), System::nanoTime);
    }
}
