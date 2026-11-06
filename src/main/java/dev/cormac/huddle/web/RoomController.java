package dev.cormac.huddle.web;

import dev.cormac.huddle.domain.RoomId;
import dev.cormac.huddle.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST endpoints for a room. Thin: parse input, call the service, map to DTOs. */
@RestController
@RequestMapping("/api/rooms/{roomId}")
public class RoomController {

    private final ChatService chatService;

    public RoomController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/messages")
    public List<MessageView> history(@PathVariable String roomId,
                                     @RequestParam(defaultValue = "50") int limit) {
        return chatService.history(new RoomId(roomId), limit).stream()
                .map(MessageView::from)
                .toList();
    }
}
