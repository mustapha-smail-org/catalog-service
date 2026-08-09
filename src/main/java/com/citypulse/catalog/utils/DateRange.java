package com.citypulse.catalog.utils;

import java.time.Instant;

public record DateRange(
        Instant startInclusive,
        Instant endExclusive
) {
}