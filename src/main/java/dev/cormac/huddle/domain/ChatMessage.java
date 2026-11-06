package dev.cormac.huddle.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A single immutable chat message. All invariants are enforced at construction,
 * so any ChatMessage instance in the system is known to be valid.
 */
public record ChatMessage(UUID id, RoomId roomId, String author, String content, Instant sentAt) {

    public static final int MAX_AUTHOR_LENGTH = 32;
    public static final int MAX_CONTENT_LENGTH = 2000;

    public ChatMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(roomId, "roomId");
        Objects.requireNonNull(sentAt, "sentAt");
        author = requireText(author, "author", MAX_AUTHOR_LENGTH);
        content = requireText(content, "content", MAX_CONTENT_LENGTH);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return stripped;
    }
}
