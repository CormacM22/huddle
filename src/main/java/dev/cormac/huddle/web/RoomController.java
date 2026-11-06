package dev.cormac.huddle.web;

import dev.cormac.huddle.domain.MeetingSummary;
import dev.cormac.huddle.domain.RoomId;
import dev.cormac.huddle.service.ChatService;
import dev.cormac.huddle.service.SummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST endpoints for a room. Thin: parse input, call the service, map to DTOs. */
@RestController
@RequestMapping("/api/rooms/{roomId}")
public class RoomController {

    private final ChatService chatService;
    private final SummaryService summaryService;

    public RoomController(ChatService chatService, SummaryService summaryService) {
        this.chatService = chatService;
        this.summaryService = summaryService;
    }

    /** POST, not GET: each call does fresh, paid work and its result isn't cacheable. */
    @PostMapping("/summary")
    public MeetingSummary summarise(@PathVariable String roomId) {
        return summaryService.summarise(new RoomId(roomId));
    }

    @GetMapping("/messages")
    public List<MessageView> history(@PathVariable String roomId,
                                     @RequestParam(defaultValue = "50") int limit) {
        return chatService.history(new RoomId(roomId), limit).stream()
                .map(MessageView::from)
                .toList();
    }
}
