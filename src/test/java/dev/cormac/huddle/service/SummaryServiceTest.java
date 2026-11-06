package dev.cormac.huddle.service;

import dev.cormac.huddle.domain.ActionItem;
import dev.cormac.huddle.domain.ChatMessage;
import dev.cormac.huddle.domain.MeetingSummary;
import dev.cormac.huddle.domain.MessageRepository;
import dev.cormac.huddle.domain.RoomId;
import dev.cormac.huddle.domain.Summariser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    private static final RoomId ROOM = new RoomId("standup");

    @Mock
    private MessageRepository repository;

    @Mock
    private Summariser summariser;

    private SummaryService service;

    @BeforeEach
    void setUp() {
        service = new SummaryService(repository, summariser);
    }

    @Test
    void emptyRoomReturnsEmptySummaryWithoutCallingTheModel() {
        when(repository.findRecent(ROOM, SummaryService.MAX_MESSAGES)).thenReturn(List.of());

        MeetingSummary result = service.summarise(ROOM);

        assertThat(result).isEqualTo(MeetingSummary.empty());
        verifyNoInteractions(summariser); // no API cost for nothing
    }

    @Test
    void summarisesTheRoomsRecentMessages() {
        var transcript = List.of(new ChatMessage(UUID.randomUUID(), ROOM, "Aoife", "I'll fix the login bug",
                Instant.parse("2026-11-07T10:00:00Z")));
        var expected = new MeetingSummary("Aoife is fixing login.", List.of(new ActionItem("Aoife", "Fix login bug")));
        when(repository.findRecent(ROOM, SummaryService.MAX_MESSAGES)).thenReturn(transcript);
        when(summariser.summarise(transcript)).thenReturn(expected);

        assertThat(service.summarise(ROOM)).isEqualTo(expected);
    }
}
