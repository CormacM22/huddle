package dev.cormac.huddle.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingSummaryTest {

    @Test
    void defensivelyCopiesActionItems() {
        var items = new ArrayList<>(List.of(new ActionItem("Aoife", "Fix login")));
        var summary = new MeetingSummary("text", items);

        items.clear();

        assertThat(summary.actionItems()).hasSize(1);
        assertThatThrownBy(() -> summary.actionItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullActionItemsBecomeEmpty() {
        assertThat(new MeetingSummary("text", null).actionItems()).isEmpty();
    }

    @Test
    void requiresSummaryText() {
        assertThatThrownBy(() -> new MeetingSummary(null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
