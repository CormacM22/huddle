package dev.cormac.huddle.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behaviour every MessageRepository implementation must satisfy.
 * Concrete test classes extend this and supply the implementation under test.
 */
public abstract class MessageRepositoryContract {

    protected static final RoomId GENERAL = new RoomId("general");
    protected static final RoomId RANDOM = new RoomId("random");

    protected MessageRepository repository;

    /** Implementations return a fresh, empty repository that holds at least 10 messages per room. */
    protected abstract MessageRepository createRepository();

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    protected static ChatMessage message(RoomId room, String content, int secondsAfterEpoch) {
        return new ChatMessage(UUID.randomUUID(), room, "Cormac", content, Instant.ofEpochSecond(secondsAfterEpoch));
    }

    @Test
    void emptyRoomHasNoMessages() {
        assertThat(repository.findRecent(GENERAL, 50)).isEmpty();
    }

    @Test
    void returnsSavedMessagesOldestFirst() {
        repository.save(message(GENERAL, "first", 1));
        repository.save(message(GENERAL, "second", 2));

        assertThat(repository.findRecent(GENERAL, 50))
                .extracting(ChatMessage::content)
                .containsExactly("first", "second");
    }

    @Test
    void limitKeepsTheMostRecentMessages() {
        for (int i = 1; i <= 5; i++) {
            repository.save(message(GENERAL, "m" + i, i));
        }

        assertThat(repository.findRecent(GENERAL, 2))
                .extracting(ChatMessage::content)
                .containsExactly("m4", "m5");
    }

    @Test
    void roomsAreIsolated() {
        repository.save(message(GENERAL, "in general", 1));
        repository.save(message(RANDOM, "in random", 2));

        assertThat(repository.findRecent(GENERAL, 50))
                .extracting(ChatMessage::content)
                .containsExactly("in general");
    }

    @Test
    void returnedListCannotBeUsedToMutateTheStore() {
        repository.save(message(GENERAL, "original", 1));

        var result = repository.findRecent(GENERAL, 50);

        assertThatThrownBy(() -> result.add(message(GENERAL, "sneaky", 2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
