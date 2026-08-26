package com.citypulse.catalog.utils;

import com.citypulse.catalog.dto.request.EventSort;
import com.citypulse.catalog.exception.InvalidCursorException;
import com.citypulse.catalog.service.EventSearchCriteria.CursorPosition;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventCursorCodecTest {

    private final EventCursorCodec codec = new EventCursorCodec();

    @Test
    void roundTripsStartDateCursorWithColonsInEventId() {
        Instant start = Instant.parse("2026-08-13T10:30:00Z");
        CursorPosition position = new CursorPosition(
                EventSort.START_DATE, start, null, "source:event:42");

        CursorPosition decoded = codec.decode(codec.encode(position));

        assertThat(decoded.sort()).isEqualTo(EventSort.START_DATE);
        assertThat(decoded.startDate()).isEqualTo(start);
        assertThat(decoded.eventId()).isEqualTo("source:event:42");
    }

    @Test
    void roundTripsRelevanceCursorWithRankScore() {
        CursorPosition position = new CursorPosition(
                EventSort.RELEVANCE, null, 0.66, "event-1");

        CursorPosition decoded = codec.decode(codec.encode(position));

        assertThat(decoded.sort()).isEqualTo(EventSort.RELEVANCE);
        assertThat(decoded.rankScore()).isEqualTo(0.66);
        assertThat(decoded.eventId()).isEqualTo("event-1");
    }

    @Test
    void roundTripsRelevanceCursorInTheUnenrichedTail() {
        CursorPosition position = new CursorPosition(
                EventSort.RELEVANCE, null, null, "event-2");

        CursorPosition decoded = codec.decode(codec.encode(position));

        assertThat(decoded.sort()).isEqualTo(EventSort.RELEVANCE);
        assertThat(decoded.rankScore()).isNull();
        assertThat(decoded.eventId()).isEqualTo("event-2");
    }

    @Test
    void treatsMissingCursorAsAbsent() {
        assertThat(codec.decode(null)).isNull();
        assertThat(codec.decode("  ")).isNull();
    }

    @Test
    void rejectsMalformedCursors() {
        assertThatThrownBy(() -> codec.decode("not-base64!"))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(encoded("S:123")))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(encoded("S:123:")))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(encoded("X:1:event")))
                .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(encoded("S:not-a-number:event")))
                .isInstanceOf(InvalidCursorException.class);
    }

    private String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
