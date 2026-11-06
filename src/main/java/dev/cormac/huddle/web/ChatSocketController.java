package dev.cormac.huddle.web;

import dev.cormac.huddle.domain.RoomId;
import dev.cormac.huddle.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * STOMP entry point. A client SENDs to /app/rooms/{roomId}/messages; the returned
 * view is broadcast to every subscriber of /topic/rooms/{roomId}.
 */
@Controller
public class ChatSocketController {

    private final ChatService chatService;

    public ChatSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/rooms/{roomId}/messages")
    @SendTo("/topic/rooms/{roomId}")
    public MessageView send(@DestinationVariable String roomId, @Payload SendMessageRequest request) {
        var message = chatService.post(new RoomId(roomId), request.author(), request.content());
        return MessageView.from(message);
    }

    /** Rejections go only to the sender's session (no login, so Spring keys it by session id). */
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorView handleInvalidMessage(IllegalArgumentException ex) {
        return new ErrorView(ex.getMessage());
    }
}
