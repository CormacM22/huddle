package dev.cormac.huddle.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import dev.cormac.huddle.domain.ActionItem;
import dev.cormac.huddle.domain.MeetingSummary;

import java.util.List;

/**
 * The JSON shape we ask Claude to return. The SDK derives a JSON Schema from this record
 * (structured outputs), so the model's reply is guaranteed to deserialise into it.
 * Kept separate from the domain so schema annotations never leak into domain classes.
 */
record ClaudeSummaryResponse(
        @JsonPropertyDescription("2-4 sentence plain-English summary of the discussion and any decisions made")
        String summary,
        @JsonPropertyDescription("Concrete follow-up tasks; empty if none were agreed")
        List<Item> actionItems) {

    record Item(
            @JsonPropertyDescription("Name of the person responsible exactly as written in the chat, or \"Unassigned\"")
            String owner,
            @JsonPropertyDescription("The task, phrased as a short imperative")
            String task) {
    }

    MeetingSummary toDomain() {
        List<ActionItem> items = actionItems == null ? List.of()
                : actionItems.stream().map(i -> new ActionItem(i.owner(), i.task())).toList();
        return new MeetingSummary(summary, items);
    }
}
