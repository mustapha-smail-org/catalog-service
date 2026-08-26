package com.citypulse.catalog.mapper;

import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventOccurrenceEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

@Component
public class EventEntityUpdater {

    public void update(EventEntity target, EventEntity source) {
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setLeadText(source.getLeadText());
        target.setDateDescription(source.getDateDescription());
        target.setUrl(source.getUrl());
        target.setImageUrl(source.getImageUrl());
        target.setImageAlt(source.getImageAlt());
        target.setImageCredit(source.getImageCredit());
        target.setTransport(source.getTransport());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setEnvironment(source.getEnvironment());

        if (target.getSlug() == null || target.getSlug().isBlank()) {
            target.setSlug(source.getSlug());
        }

        target.setLocation(source.getLocation());
        target.setAccessibility(source.getAccessibility());
        target.setPricing(source.getPricing());

        target.setSourceUpdatedAt(source.getSourceUpdatedAt());

        target.replaceCategories(
                new LinkedHashSet<>(source.getCategories())
        );

        target.replaceOccurrences(
                source.getOccurrences()
                        .stream()
                        .map(occurrence ->
                                new EventOccurrenceEntity(
                                        occurrence.getStart(),
                                        occurrence.getEnd()
                                )
                        )
                        .toList()
        );
    }
}
