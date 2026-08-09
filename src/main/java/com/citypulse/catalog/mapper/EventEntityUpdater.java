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
        target.setUrl(source.getUrl());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());

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