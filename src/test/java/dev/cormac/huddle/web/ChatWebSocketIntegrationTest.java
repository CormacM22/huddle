package dev.cormac.huddle.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end over a real socket: real server on a random port, real STOMP clients,
 * real service and repository. Each test uses its own room so tests stay independent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {

    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private ThreadPoolTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        // Receipts have a timeout, so the client needs a scheduler to detect lost ones.
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        stompClient.setTaskScheduler(scheduler);
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
        scheduler.shutdown();
    }

    @Test
    void messageIsBroadcastToEveryoneInTheRoomAndStoredInHistory() throws Exception {
        StompSession alice = connect();
        StompSession bob = connect();
        BlockingQueue<MessageView> aliceInbox = subscribe(alice, "/topic/rooms/broadcast-test", MessageView.class);
        BlockingQueue<MessageView> bobInbox = subscribe(bob, "/topic/rooms/broadcast-test", MessageView.class);

        alice.send("/app/rooms/broadcast-test/messages", new SendMessageRequest("Alice", "Standup in 5"));

        MessageView received = bobInbox.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.author()).isEqualTo("Alice");
        assertThat(received.content()).isEqualTo("Standup in 5");
        assertThat(received.roomId()).isEqualTo("broadcast-test");
        assertThat(aliceInbox.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(received);

        List<MessageView> history = RestClient.create("http://localhost:" + port).get()
                .uri("/api/rooms/broadcast-test/messages")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        assertThat(history).containsExactly(received);
    }

    @Test
    void messagesDoNotLeakBetweenRooms() throws Exception {
        StompSession alice = connect();
        StompSession bob = connect();
        BlockingQueue<MessageView> bobInbox = subscribe(bob, "/topic/rooms/leak-a", MessageView.class);

        alice.send("/app/rooms/leak-b/messages", new SendMessageRequest("Alice", "for room b"));
        alice.send("/app/rooms/leak-a/messages", new SendMessageRequest("Alice", "for room a"));

        // Messages from one session are processed in order, so if room b leaked, it would arrive first.
        MessageView first = bobInbox.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(first).isNotNull();
        assertThat(first.content()).isEqualTo("for room a");
    }

    @Test
    void invalidMessageIsReportedToSenderOnlyAndNotBroadcast() throws Exception {
        StompSession alice = connect();
        StompSession bob = connect();
        BlockingQueue<ErrorView> aliceErrors = subscribe(alice, "/user/queue/errors", ErrorView.class);
        BlockingQueue<ErrorView> bobErrors = subscribe(bob, "/user/queue/errors", ErrorView.class);
        BlockingQueue<MessageView> bobInbox = subscribe(bob, "/topic/rooms/invalid-test", MessageView.class);

        alice.send("/app/rooms/invalid-test/messages", new SendMessageRequest("Alice", "   "));

        ErrorView error = aliceErrors.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.error()).contains("content");
        assertThat(bobInbox.poll(500, TimeUnit.MILLISECONDS)).isNull();
        assertThat(bobErrors).isEmpty();
    }

    private StompSession connect() throws Exception {
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        session.setAutoReceipt(true); // ask the server to confirm each SUBSCRIBE
        return session;
    }

    /** Subscribes and blocks until the server confirms, avoiding a send-before-subscribe race. */
    private <T> BlockingQueue<T> subscribe(StompSession session, String destination, Class<T> type)
            throws Exception {
        BlockingQueue<T> inbox = new LinkedBlockingQueue<>();
        CompletableFuture<Void> receipt = new CompletableFuture<>();

        StompSession.Subscription subscription = session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return type;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.add(type.cast(payload));
            }
        });
        subscription.addReceiptTask(() -> receipt.complete(null));
        subscription.addReceiptLostTask(() -> receipt.completeExceptionally(new AssertionError("No receipt")));
        receipt.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return inbox;
    }
}
