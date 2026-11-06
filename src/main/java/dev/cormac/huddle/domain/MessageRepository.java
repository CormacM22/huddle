package dev.cormac.huddle.domain;

import java.util.List;

/**
 * Port for storing chat messages. The domain owns this interface;
 * infrastructure provides implementations (in-memory today, a database tomorrow).
 */
public interface MessageRepository {

    ChatMessage save(ChatMessage message);

    /**
     * @return up to {@code limit} of the most recent messages in the room, oldest first.
     *         The returned list is immutable.
     */
    List<ChatMessage> findRecent(RoomId roomId, int limit);
}
