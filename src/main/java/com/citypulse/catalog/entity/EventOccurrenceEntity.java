package com.citypulse.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(
        name = "event_occurrences",
        indexes = {
                @Index(
                        name = "idx_occurrences_event",
                        columnList = "event_id"
                ),
                @Index(
                        name = "idx_occurrences_start_date",
                        columnList = "start_date"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventOccurrenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_occurrences_event"
            )
    )
    private EventEntity event;

    @Column(name = "start_date", nullable = false)
    private Instant start;

    @Column(name = "end_date")
    private Instant end;

    public EventOccurrenceEntity(Instant start, Instant end) {
        this.start = Objects.requireNonNull(
                start,
                "Occurrence start must not be null"
        );
        this.end = end;
    }

    void attachTo(EventEntity event) {
        this.event = Objects.requireNonNull(
                event,
                "Event must not be null"
        );
    }
}