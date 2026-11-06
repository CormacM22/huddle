package dev.cormac.huddle.config;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import dev.cormac.huddle.domain.Summariser;
import dev.cormac.huddle.infrastructure.ai.ClaudeSummariser;
import dev.cormac.huddle.infrastructure.ai.DisabledSummariser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Chooses the Summariser implementation. The key only ever comes from configuration/env. */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    Summariser summariser(@Value("${huddle.ai.api-key:}") String apiKey,
                          @Value("${huddle.ai.model:claude-opus-5-5}") String model) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY not set: AI summaries are disabled");
            return new DisabledSummariser();
        }
        var client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(60))
                .build(); // SDK default: 2 retries with backoff on 429/5xx/network errors
        log.info("AI summaries enabled with model {}", model);
        return new ClaudeSummariser(client, model);
    }
}
