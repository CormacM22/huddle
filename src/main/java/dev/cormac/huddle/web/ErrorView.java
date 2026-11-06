package dev.cormac.huddle.web;

/** Error sent privately to a WebSocket client whose message was rejected. */
public record ErrorView(String error) {
}
