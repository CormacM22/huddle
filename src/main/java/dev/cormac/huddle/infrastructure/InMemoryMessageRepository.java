package dev.cormac.huddle.infrastructure;

import dev.cormac.huddle.domain.ChatMessage;
import dev.cormac.huddle.domain.MessageRepository;
import dev.cormac.huddle.domain.RoomId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, bounded, in-memory message store.
 *
 * <p>Each room keeps at most {@code maxMessagesPerRoom} messages; the oldest are evicted first.
 * The map is concurrent so rooms are created safely, and each room's deque is guarded by its
 * own lock, so writers in different rooms never block each other.
 */
@Repository
public class InMemoryMessageRepository implements MessageRepository {

    private final Map<RoomId, Deque<ChatMessage>> rooms = new ConcurrentHashMap<>();
    private final int maxMessagesPerRoom;

    public InMemoryMessageRepository(@Value("${huddle.history.max-per-room:500}") int maxMessagesPerRoom) {
        if (maxMessagesPerRoom <= 0) {
            throw new IllegalArgumentException("maxMessagesPerRoom must be positive");
        }
        this.maxMessagesPerRoom = maxMessagesPerRoom;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        Deque<ChatMessage> room = rooms.computeIfAbsent(message.roomId(), id -> new ArrayDeque<>());
        synchronized (room) {
            room.addLast(message);
            while (room.size() > maxMessagesPerRoom) {
                room.removeFirst();
            }
        }
        return message;
    }

    @Override
    public List<ChatMessage> findRecent(RoomId roomId, int limit) {
        Deque<ChatMessage> room = rooms.get(roomId);
        if (room == null || limit <= 0) {
            return List.of();
        }
        synchronized (room) {
            int skip = Math.max(0, room.size() - limit);
            return room.stream().skip(skip).toList(); // Stream.toList() is unmodifiable
        }
    }
}
