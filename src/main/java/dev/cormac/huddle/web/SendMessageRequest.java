package dev.cormac.huddle.web;

/** Payload a client sends to post a message. Validation lives in the domain (ChatMessage). */
public record SendMessageRequest(String author, String content) {
}
