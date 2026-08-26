package com.citypulse.catalog.mapper;

import com.citypulse.catalog.entity.*;
import com.citypulse.catalog.utils.EventSlugGenerator;
import com.citypulse.events.avro.EventAccessibilityAvro;
import com.citypulse.events.avro.EventAvro;
import com.citypulse.events.avro.EventLocationAvro;
import com.citypulse.events.avro.EventPricingAvro;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;

@Component
public class EventAvroMapper {

    public EventEntity toEntity(EventAvro source) {
        Objects.requireNonNull(source, "EventAvro must not be null");

        EventEntity event = new EventEntity(requireText(source.getId(), "id"), requireText(source.getTitle(), "title"), Objects.requireNonNull(source.getStartDate(), "startDate must not be null"));

        event.setSourceEventId(source.getSourceEventId());
        event.setSlug(EventSlugGenerator.generate(
                source.getTitle(),
                source.getSourceEventId() == null
                        ? source.getId()
                        : source.getSourceEventId().toString()
        ));
        event.setDescription(source.getDescription());
        event.setLeadText(source.getLeadText());
        event.setDateDescription(source.getDateDescription());
        event.setUrl(source.getUrl());
        event.setImageUrl(source.getImageUrl());
        event.setImageAlt(source.getImageAlt());
        event.setImageCredit(source.getImageCredit());
        event.setTransport(source.getTransport());
        event.setEndDate(source.getEndDate());
        event.setEnvironment(EventEnvironment.fromValue(source.getEnvironment()));
        event.setSourceUpdatedAt(source.getSourceUpdatedAt());

        event.setLocation(mapLocation(source.getLocation()));
        event.setAccessibility(mapAccessibility(source.getAccessibility()));
        event.setPricing(mapPricing(source.getPricing()));

        event.replaceCategories(source.getCategories() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source.getCategories()));

        if (source.getOccurrences() != null) {
            source.getOccurrences().forEach(occurrence -> event.addOccurrence(new EventOccurrenceEntity(occurrence.getStart(), occurrence.getEnd())));
        }

        return event;
    }

    private EventLocationEmbeddable mapLocation(EventLocationAvro source) {
        if (source == null) {
            return new EventLocationEmbeddable();
        }

        return new EventLocationEmbeddable(source.getName(), source.getStreet(), source.getZipcode(), source.getCity(), source.getLatitude(), source.getLongitude());
    }

    private EventAccessibilityEmbeddable mapAccessibility(EventAccessibilityAvro source) {
        if (source == null) {
            return new EventAccessibilityEmbeddable();
        }

        return new EventAccessibilityEmbeddable(source.getWheelchairAccessible(), source.getBlindAccessible(), source.getDeafAccessible(), source.getSignLanguage(), source.getMentalAccessibility());
    }

    private EventPricingEmbeddable mapPricing(EventPricingAvro source) {
        if (source == null) {
            return new EventPricingEmbeddable();
        }

        return new EventPricingEmbeddable(source.getPriceType(), source.getPriceDetail(), source.getAccessType(), source.getBookingUrl(), source.getBookingLinkText());
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value;
    }
}
