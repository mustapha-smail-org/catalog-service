package com.citypulse.catalog.utils;

import com.citypulse.catalog.exception.InvalidCursorException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventCursorCodecTest {

    private final EventCursorCodec codec = new EventCursorCodec();

    @Test
    void shouldRoundTripCursorWithColonsInEventId() {
        Instant start = Instant.parse("2026-08-13T10:30:00Z");

        assertThat(codec.decode(codec.encode(start, "source:event:42")))
                .satisfies(cursor -> {
                    assertThat(cursor.startDate()).isEqualTo(start);
                    assertThat(cursor.eventId()).isEqualTo("source:event:42");
                });
    }

    @Test
    void shouldTreatMissingCursorAsAbsent() {
        assertThat(codec.decode(null)).isNull();
        assertThat(codec.decode("  ")).isNull();
    }

    @Test
    void shouldRejectMalformedCursorVariants() {
        String missingTimestamp = encoded(":event");
        String missingEvent = encoded("123:");
        String invalidTimestamp = encoded("not-a-number:event");

        assertThatThrownBy(() -> codec.decode("not-base64!"))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(missingTimestamp))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(missingEvent))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(invalidTimestamp))
                .isInstanceOf(InvalidCursorException.class);
    }

    private String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
