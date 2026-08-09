package com.citypulse.catalog.utils;

import com.citypulse.catalog.exception.InvalidCursorException;
import com.citypulse.catalog.service.EventSearchCriteria.CursorPosition;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class EventCursorCodec {

    public String encode(Instant startDate, String eventId) {
        String value = startDate.toEpochMilli() + ":" + eventId;

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

            int separator = decoded.indexOf(':');

            if (separator <= 0 || separator == decoded.length() - 1) {
                throw new IllegalArgumentException();
            }

            return new CursorPosition(
                    Instant.ofEpochMilli(
                            Long.parseLong(decoded.substring(0, separator))
                    ),
                    decoded.substring(separator + 1)
            );
        } catch (RuntimeException exception) {
            throw new InvalidCursorException(exception);
        }
    }
}