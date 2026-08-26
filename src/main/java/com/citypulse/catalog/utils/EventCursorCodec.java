package com.citypulse.catalog.utils;

import com.citypulse.catalog.dto.request.EventSort;
import com.citypulse.catalog.exception.InvalidCursorException;
import com.citypulse.catalog.service.EventSearchCriteria.CursorPosition;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Opaque keyset cursor: {@code <sort>:<key>:<eventId>} base64url-encoded.
 * {@code sort} is {@code S} (START_DATE, key = epoch millis) or {@code R}
 * (RELEVANCE, key = rank score, empty for the unenriched tail). The eventId is
 * the last segment so a ':' inside it is preserved.
 */
@Component
public class EventCursorCodec {

    public String encode(CursorPosition position) {
        String value = switch (position.sort()) {
            case START_DATE -> "S:" + position.startDate().toEpochMilli()
                    + ":" + position.eventId();
            case RELEVANCE -> "R:"
                    + (position.rankScore() == null ? "" : position.rankScore())
                    + ":" + position.eventId();
        };

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public CursorPosition decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );

            String[] parts = decoded.split(":", 3);
            if (parts.length != 3 || parts[2].isEmpty()) {
                throw new IllegalArgumentException("malformed cursor");
            }

            return switch (parts[0]) {
                case "S" -> new CursorPosition(
                        EventSort.START_DATE,
                        Instant.ofEpochMilli(Long.parseLong(parts[1])),
                        null,
                        parts[2]);
                case "R" -> new CursorPosition(
                        EventSort.RELEVANCE,
                        null,
                        parts[1].isEmpty() ? null : Double.parseDouble(parts[1]),
                        parts[2]);
                default -> throw new IllegalArgumentException("unknown sort");
            };
        } catch (RuntimeException exception) {
            throw new InvalidCursorException(exception);
        }
    }
}
