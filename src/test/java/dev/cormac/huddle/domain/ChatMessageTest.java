package dev.cormac.huddle.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {

    private static final RoomId ROOM = new RoomId("general");
    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    @Test
    void stripsSurroundingWhitespaceFromAuthorAndContent() {
        var message = new ChatMessage(UUID.randomUUID(), ROOM, "  Cormac ", "  hello  ", NOW);

        assertThat(message.author()).isEqualTo("Cormac");
        assertThat(message.content()).isEqualTo("hello");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankAuthor(String author) {
        assertThatThrownBy(() -> new ChatMessage(UUID.randomUUID(), ROOM, author, "hi", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("author");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankContent(String content) {
        assertThatThrownBy(() -> new ChatMessage(UUID.randomUUID(), ROOM, "Cormac", content, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content");
    }

    @Test
    void rejectsOverlongAuthor() {
        String author = "a".repeat(ChatMessage.MAX_AUTHOR_LENGTH + 1);

        assertThatThrownBy(() -> new ChatMessage(UUID.randomUUID(), ROOM, author, "hi", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsContentAtMaximumLengthButNotBeyond() {
        String max = "x".repeat(ChatMessage.MAX_CONTENT_LENGTH);

        assertThat(new ChatMessage(UUID.randomUUID(), ROOM, "Cormac", max, NOW).content()).hasSize(max.length());
        assertThatThrownBy(() -> new ChatMessage(UUID.randomUUID(), ROOM, "Cormac", max + "x", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresIdRoomAndTimestamp() {
        assertThatThrownBy(() -> new ChatMessage(null, ROOM, "Cormac", "hi", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ChatMessage(UUID.randomUUID(), null, "Cormac", "hi", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ChatMessage(UUID.randomUUID(), ROOM, "Cormac", "hi", null))
                .isInstanceOf(NullPointerException.class);
    }
}
