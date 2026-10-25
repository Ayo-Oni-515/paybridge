package com.academy.paybridge.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableAsync       // notification listeners run off the request thread
@EnableScheduling  // settlement's nightly ledger check
public class AppConfig {

    /** Inject a Clock instead of calling Instant.now(): tests can then freeze time (quote expiry). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
