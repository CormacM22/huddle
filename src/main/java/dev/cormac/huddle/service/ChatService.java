package dev.cormac.huddle.service;

import dev.cormac.huddle.domain.ChatMessage;
import dev.cormac.huddle.domain.MessageRepository;
import dev.cormac.huddle.domain.RoomId;
import org.springframework.stereotype.Service;
import org.springframework.util.IdGenerator;

import java.time.Clock;
import java.util.List;

/**
 * Application service for chat: creates messages and reads room history.
 * Knows nothing about HTTP or WebSockets, so any transport can reuse it.
 */
@Service
public class ChatService {

    public static final int MAX_HISTORY = 200;

    private final MessageRepository repository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    // Single constructor: Spring injects the beans automatically (no @Autowired needed).
    public ChatService(MessageRepository repository, Clock clock, IdGenerator idGenerator) {
        this.repository = repository;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    public ChatMessage post(RoomId roomId, String author, String content) {
        // The ChatMessage constructor validates; if it throws, nothing is saved.
        var message = new ChatMessage(idGenerator.generateId(), roomId, author, content, clock.instant());
        return repository.save(message);
    }

    public List<ChatMessage> history(RoomId roomId, int limit) {
        int clamped = Math.clamp(limit, 1, MAX_HISTORY); // Math.clamp: Java 21
        return repository.findRecent(roomId, clamped);
    }
}
