package dev.cormac.huddle.domain;

import java.util.regex.Pattern;

/**
 * Identifies a chat room. A value object: two RoomIds with the same slug are equal,
 * and an invalid RoomId cannot be constructed.
 */
public record RoomId(String value) {

    // 1-32 chars, lowercase letters/digits, hyphens only between other characters.
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,30}[a-z0-9])?$");

    /** Compact constructor: runs before the field is assigned, so we validate here. */
    public RoomId {
        if (value == null || !SLUG.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Room id must be 1-32 lowercase letters, digits or inner hyphens: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
