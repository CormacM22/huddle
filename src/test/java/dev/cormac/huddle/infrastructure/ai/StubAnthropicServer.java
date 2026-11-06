package dev.cormac.huddle.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny fake of the Anthropic Messages API built on the JDK's own HTTP server.
 * Lets tests drive the real SDK (serialisation, headers, error mapping) with no network or API key.
 */
final class StubAnthropicServer implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private volatile int status = 200;
    private volatile String responseBody = "{}";
    private volatile String lastRequestBody;
    private volatile Headers lastRequestHeaders;

    StubAnthropicServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); // port 0 = any free port
        server.createContext("/v1/messages", exchange -> {
            var headers = new Headers(); // copy: the exchange's own headers are invalid after close()
            headers.putAll(exchange.getRequestHeaders());
            lastRequestHeaders = headers;
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void respondWith(int status, String body) {
        this.status = status;
        this.responseBody = body;
    }

    /** A successful Messages API response whose single text block is {@code text}. */
    void respondWithText(String text, String stopReason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", "msg_test");
        body.put("type", "message");
        body.put("role", "assistant");
        body.put("model", "claude-opus-5-5");
        body.put("content", List.of(Map.of("type", "text", "text", text)));
        body.put("stop_reason", stopReason);
        body.put("stop_sequence", null);
        body.put("usage", Map.of("input_tokens", 100, "output_tokens", 50));
        try {
            respondWith(200, JSON.writeValueAsString(body)); // real encoder: no hand-rolled escaping bugs
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    String lastRequestBody() {
        return lastRequestBody;
    }

    String lastRequestHeader(String name) {
        return lastRequestHeaders == null ? null : lastRequestHeaders.getFirst(name);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
