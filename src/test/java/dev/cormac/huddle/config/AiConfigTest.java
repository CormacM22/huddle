package dev.cormac.huddle.config;

import dev.cormac.huddle.domain.SummaryUnavailableException;
import dev.cormac.huddle.infrastructure.ai.ClaudeSummariser;
import dev.cormac.huddle.infrastructure.ai.DisabledSummariser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiConfigTest {

    private final AiConfig config = new AiConfig();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void withoutApiKeySummariesAreDisabledWithAHelpfulMessage(String apiKey) {
        var summariser = config.summariser(apiKey, "claude-opus-5-5");

        assertThat(summariser).isInstanceOf(DisabledSummariser.class);
        assertThatThrownBy(() -> summariser.summarise(List.of()))
                .isInstanceOf(SummaryUnavailableException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }

    @Test
    void withApiKeyUsesClaude() {
        assertThat(config.summariser("sk-ant-test", "claude-opus-5-5")).isInstanceOf(ClaudeSummariser.class);
    }
}
