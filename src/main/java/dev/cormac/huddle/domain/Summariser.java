package dev.cormac.huddle.domain;

import java.util.List;

/**
 * Port for turning a transcript into a summary. Implemented by an LLM adapter in production
 * and by mocks in tests, so no test ever needs network access or an API key.
 */
public interface Summariser {

    /**
     * @param transcript non-empty, oldest first
     * @throws SummaryUnavailableException if the summary cannot be produced right now
     */
    MeetingSummary summarise(List<ChatMessage> transcript);
}
