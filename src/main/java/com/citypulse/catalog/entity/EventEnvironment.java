package com.citypulse.catalog.entity;

/**
 * Indoor/outdoor setting carried from ingestion (derived from the Paris API
 * {@code event_indoor} flag). Unrecognised or missing values map to
 * {@link #UNKNOWN} so a malformed message never breaks persistence.
 */
public enum EventEnvironment {
    INDOOR,
    OUTDOOR,
    UNKNOWN;

    public static EventEnvironment fromValue(CharSequence value) {
        if (value == null) {
            return UNKNOWN;
        }

        try {
            return valueOf(value.toString().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
