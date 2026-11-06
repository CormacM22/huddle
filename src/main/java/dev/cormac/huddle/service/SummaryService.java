package dev.cormac.huddle.service;

import dev.cormac.huddle.domain.MeetingSummary;
import dev.cormac.huddle.domain.MessageRepository;
import dev.cormac.huddle.domain.RoomId;
import dev.cormac.huddle.domain.Summariser;
import org.springframework.stereotype.Service;

/** Summarises a room's recent conversation via whichever Summariser is configured. */
@Service
public class SummaryService {

    /** Bounds prompt size (and cost) regardless of how long the room's history is. */
    public static final int MAX_MESSAGES = 200;

    private final MessageRepository repository;
    private final Summariser summariser;

    public SummaryService(MessageRepository repository, Summariser summariser) {
        this.repository = repository;
        this.summariser = summariser;
    }

    public MeetingSummary summarise(RoomId roomId) {
        var transcript = repository.findRecent(roomId, MAX_MESSAGES);
        if (transcript.isEmpty()) {
            return MeetingSummary.empty();
        }
        return summariser.summarise(transcript);
    }
}
