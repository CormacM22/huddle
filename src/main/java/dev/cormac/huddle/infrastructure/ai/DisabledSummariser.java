package dev.cormac.huddle.infrastructure.ai;

import dev.cormac.huddle.domain.ChatMessage;
import dev.cormac.huddle.domain.MeetingSummary;
import dev.cormac.huddle.domain.Summariser;
import dev.cormac.huddle.domain.SummaryUnavailableException;

import java.util.List;

/**
 * Null Object used when no API key is configured: chat works, summaries fail clearly.
 * Lets the app (and CI) run without secrets.
 */
public class DisabledSummariser implements Summariser {

    @Override
    public MeetingSummary summarise(List<ChatMessage> transcript) {
        throw new SummaryUnavailableException("AI summaries are disabled: set ANTHROPIC_API_KEY to enable them");
    }
}
