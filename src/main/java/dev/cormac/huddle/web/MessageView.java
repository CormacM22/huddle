package dev.cormac.huddle.web;

import dev.cormac.huddle.domain.ChatMessage;

import java.time.Instant;
import java.util.UUID;

/** JSON shape of a message, shared by the REST API and WebSocket broadcasts. */
public record MessageView(UUID id, String roomId, String author, String content, Instant sentAt) {

    public static MessageView from(ChatMessage message) {
        return new MessageView(message.id(), message.roomId().value(),
                message.author(), message.content(), message.sentAt());
    }
}
