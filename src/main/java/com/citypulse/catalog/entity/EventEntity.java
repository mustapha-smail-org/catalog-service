package com.citypulse.catalog.entity;

import com.citypulse.catalog.utils.EventSlugGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_start_date", columnList = "start_date"),
                @Index(name = "idx_events_end_date", columnList = "end_date"),
                @Index(name = "idx_events_city", columnList = "city"),
                @Index(name = "idx_events_zipcode", columnList = "zipcode"),
                @Index(
                        name = "idx_events_source_updated_at",
                        columnList = "source_updated_at"
                ),
                @Index(name = "idx_events_slug", columnList = "slug")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventEntity {

    @Id
    @Column(name = "id", nullable = false, length = 150)
    private String id;

    @Column(name = "source_event_id", unique = true)
    private Long sourceEventId;

    @Column(name = "slug", nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lead_text", columnDefinition = "TEXT")
    private String leadText;

    @Column(name = "date_description", columnDefinition = "TEXT")
    private String dateDescription;

    @Column(name = "url", length = 2_048)
    private String url;

    @Column(name = "image_url", length = 2_048)
    private String imageUrl;

    @Column(name = "image_alt", length = 1_000)
    private String imageAlt;

    @Column(name = "image_credit", length = 1_000)
    private String imageCredit;

    @Column(name = "transport", columnDefinition = "TEXT")
    private String transport;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Embedded
    private EventLocationEmbeddable location =
            new EventLocationEmbeddable();

    @Embedded
    private EventAccessibilityEmbeddable accessibility =
            new EventAccessibilityEmbeddable();

    @Embedded
    private EventPricingEmbeddable pricing =
            new EventPricingEmbeddable();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "event_categories",
            joinColumns = @JoinColumn(name = "event_id"),
            foreignKey = @ForeignKey(
                    name = "fk_event_categories_event"
            )
    )
    @Column(name = "category", nullable = false, length = 100)
    private Set<String> categories = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "event",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("position ASC")
    private List<EventOccurrenceEntity> occurrences =
            new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 10)
    private EventEnvironment environment = EventEnvironment.UNKNOWN;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public EventEntity(String id, String title, Instant startDate) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.slug = EventSlugGenerator.generate(this.title, this.id);
        this.startDate = java.util.Objects.requireNonNull(
                startDate,
                "startDate must not be null"
        );
    }

    public void replaceOccurrences(
            List<EventOccurrenceEntity> newOccurrences
    ) {
        occurrences.clear();

        if (newOccurrences == null) {
            return;
        }

        newOccurrences.forEach(this::addOccurrence);
    }

    public void addOccurrence(EventOccurrenceEntity occurrence) {
        java.util.Objects.requireNonNull(
                occurrence,
                "occurrence must not be null"
        );

        occurrence.attachTo(this);
        occurrences.add(occurrence);
    }

    public void replaceCategories(Set<String> newCategories) {
        categories.clear();

        if (newCategories == null) {
            return;
        }

        newCategories.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(category -> !category.isBlank())
                .forEach(categories::add);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value;
    }
}
