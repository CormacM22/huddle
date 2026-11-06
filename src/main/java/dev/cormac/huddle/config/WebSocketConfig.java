package dev.cormac.huddle.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket.
 * <ul>
 *   <li>Clients connect to {@code /ws}.</li>
 *   <li>Frames sent to {@code /app/**} are routed to {@code @MessageMapping} methods.</li>
 *   <li>{@code /topic/**} is broadcast (pub/sub); {@code /user/queue/**} is private to one session.</li>
 * </ul>
 * The in-memory simple broker is fine for one instance; scaling out would swap in a
 * broker relay (RabbitMQ/ActiveMQ) here without touching controllers.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOriginPatterns;
    private final MessageChannel clientOutboundChannel;

    // @Lazy: the outbound channel bean is created by the very configuration this class feeds,
    // so we take a lazy proxy to break the circular dependency.
    public WebSocketConfig(@Value("${huddle.websocket.allowed-origin-patterns:}") String[] allowedOriginPatterns,
                           @Lazy @Qualifier("clientOutboundChannel") MessageChannel clientOutboundChannel) {
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.clientOutboundChannel = clientOutboundChannel;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new SubscriptionReceiptInterceptor(clientOutboundChannel));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOriginPatterns); // empty = same-origin only
        registry.setPreserveReceiveOrder(true); // handle each client's frames in the order sent
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setPreservePublishOrder(true); // deliver broadcasts to each client in order
    }
}
