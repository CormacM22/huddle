package dev.cormac.huddle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;

import java.time.Clock;

/**
 * Beans for "ambient" dependencies (time, ids). Declaring them as beans instead of
 * calling Instant.now() / UUID.randomUUID() inline is what makes the service testable.
 */
@Configuration
public class CoreConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGenerator idGenerator() {
        return new JdkIdGenerator(); // random UUIDs
    }
}
