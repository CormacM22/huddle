package dev.cormac.huddle.domain;

import java.util.List;
import java.util.Objects;

/** The result of summarising a room: a short overview plus extracted action items. */
public record MeetingSummary(String summary, List<ActionItem> actionItems) {

    public MeetingSummary {
        Objects.requireNonNull(summary, "summary");
        // List.copyOf: immutable snapshot, so callers can't mutate our state afterwards.
        actionItems = actionItems == null ? List.of() : List.copyOf(actionItems);
    }

    public static MeetingSummary empty() {
        return new MeetingSummary("No messages to summarise yet.", List.of());
    }
}
