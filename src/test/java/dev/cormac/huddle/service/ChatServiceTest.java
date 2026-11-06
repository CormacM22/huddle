package dev.cormac.huddle.service;

import dev.cormac.huddle.domain.ChatMessage;
import dev.cormac.huddle.domain.MessageRepository;
import dev.cormac.huddle.domain.RoomId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final RoomId ROOM = new RoomId("general");
    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private MessageRepository repository;

    private ChatService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new ChatService(repository, fixedClock, () -> ID);
    }

    @Test
    void postCreatesTimestampedMessageAndSavesIt() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage posted = service.post(ROOM, "Cormac", "Standup in 5");

        assertThat(posted).isEqualTo(new ChatMessage(ID, ROOM, "Cormac", "Standup in 5", NOW));
        verify(repository).save(posted);
    }

    @Test
    void invalidMessageIsRejectedBeforeReachingTheRepository() {
        assertThatThrownBy(() -> service.post(ROOM, "Cormac", "   "))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void historyDelegatesToRepository() {
        var stored = List.of(new ChatMessage(ID, ROOM, "Cormac", "hi", NOW));
        when(repository.findRecent(ROOM, 50)).thenReturn(stored);

        assertThat(service.history(ROOM, 50)).isEqualTo(stored);
    }

    @Test
    void historyLimitIsClampedToMaximum() {
        service.history(ROOM, 1_000_000);

        verify(repository).findRecent(ROOM, ChatService.MAX_HISTORY);
    }

    @Test
    void historyLimitIsAtLeastOne() {
        service.history(ROOM, -5);

        verify(repository).findRecent(ROOM, 1);
    }
}
