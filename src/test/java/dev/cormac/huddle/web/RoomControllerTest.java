package dev.cormac.huddle.web;

import dev.cormac.huddle.domain.ActionItem;
import dev.cormac.huddle.domain.ChatMessage;
import dev.cormac.huddle.domain.MeetingSummary;
import dev.cormac.huddle.domain.RoomId;
import dev.cormac.huddle.domain.SummaryUnavailableException;
import dev.cormac.huddle.service.ChatService;
import dev.cormac.huddle.service.SummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    private static final RoomId GENERAL = new RoomId("general");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private SummaryService summaryService;

    @Test
    void returnsRoomHistoryAsJson() {
        var message = new ChatMessage(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                GENERAL, "Cormac", "hello", Instant.parse("2026-11-07T10:00:00Z"));
        when(chatService.history(GENERAL, 50)).thenReturn(List.of(message));

        var response = mvc.get().uri("/api/rooms/general/messages");

        assertThat(response).hasStatusOk();
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                [{
                  "id": "00000000-0000-0000-0000-000000000001",
                  "roomId": "general",
                  "author": "Cormac",
                  "content": "hello",
                  "sentAt": "2026-11-07T10:00:00Z"
                }]
                """);
    }

    @Test
    void passesLimitThrough() {
        when(chatService.history(GENERAL, 10)).thenReturn(List.of());

        assertThat(mvc.get().uri("/api/rooms/general/messages?limit=10")).hasStatusOk();
        verify(chatService).history(GENERAL, 10);
    }

    @Test
    void summariseReturnsSummaryAndActionItems() {
        when(summaryService.summarise(GENERAL)).thenReturn(new MeetingSummary("Shipped v1.",
                List.of(new ActionItem("Cian", "Write release notes"))));

        var response = mvc.post().uri("/api/rooms/general/summary");

        assertThat(response).hasStatusOk();
        assertThat(response).bodyJson().isLenientlyEqualTo("""
                {"summary": "Shipped v1.", "actionItems": [{"owner": "Cian", "task": "Write release notes"}]}
                """);
    }

    @Test
    void summaryUnavailableIsServiceUnavailable() {
        when(summaryService.summarise(GENERAL)).thenThrow(new SummaryUnavailableException("AI is off"));

        var response = mvc.post().uri("/api/rooms/general/summary");

        assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("AI is off");
    }

    @Test
    void invalidRoomIdIsBadRequestWithProblemDetail() {
        var response = mvc.get().uri("/api/rooms/NOT_VALID/messages");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
        verifyNoInteractions(chatService);
    }
}
