package dev.cormac.huddle.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomIdTest {

    @ParameterizedTest
    @ValueSource(strings = {"general", "team-standup", "q3-planning", "a"})
    void acceptsLowercaseSlugs(String value) {
        assertThat(new RoomId(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "General", "has space", "-leading", "trailing-", "emoji😀",
            "this-room-name-is-far-too-long-to-be-valid"})
    void rejectsInvalidSlugs(String value) {
        assertThatThrownBy(() -> new RoomId(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roomsWithSameSlugAreEqual() {
        assertThat(new RoomId("general")).isEqualTo(new RoomId("general"));
    }
}
