package dev.cormac.huddle.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Sends a STOMP RECEIPT for SUBSCRIBE frames that ask for one.
 *
 * <p>Spring's in-memory simple broker ignores the {@code receipt} header (only external broker
 * relays honour it). Without receipts a client cannot know when its subscription is live, which
 * makes "subscribe then send" racy. We reply only after the handler that actually registers the
 * subscription has run: the user-destination resolver for {@code /user/**}, the broker otherwise.
 */
class SubscriptionReceiptInterceptor implements ExecutorChannelInterceptor {

    private static final String USER_PREFIX = "/user/";

    private final MessageChannel clientOutboundChannel;

    SubscriptionReceiptInterceptor(MessageChannel clientOutboundChannel) {
        this.clientOutboundChannel = clientOutboundChannel;
    }

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
        StompHeaderAccessor inbound = StompHeaderAccessor.wrap(message);
        if (ex != null || inbound.getCommand() != StompCommand.SUBSCRIBE || inbound.getReceipt() == null) {
            return;
        }
        if (!isSubscriptionOwner(handler, inbound.getDestination())) {
            return;
        }
        StompHeaderAccessor receipt = StompHeaderAccessor.create(StompCommand.RECEIPT);
        receipt.setReceiptId(inbound.getReceipt());
        receipt.setSessionId(inbound.getSessionId());
        clientOutboundChannel.send(MessageBuilder.createMessage(new byte[0], receipt.getMessageHeaders()));
    }

    private static boolean isSubscriptionOwner(MessageHandler handler, String destination) {
        boolean userDestination = destination != null && destination.startsWith(USER_PREFIX);
        return userDestination
                ? handler instanceof UserDestinationMessageHandler
                : handler instanceof SimpleBrokerMessageHandler;
    }
}
