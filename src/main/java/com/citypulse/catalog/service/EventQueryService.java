package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.exception.EventNotFoundException;
import com.citypulse.catalog.mapper.EventResponseMapper;
import com.citypulse.catalog.repository.EventRepository;
import com.citypulse.catalog.specification.EventSpecification;
import com.citypulse.catalog.utils.DateRangeResolver;
import com.citypulse.catalog.utils.EventCursorCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService {

    private final EventRepository eventRepository;
    private final EventResponseMapper mapper;
    private final DateRangeResolver dateRangeResolver;
    private final EventCursorCodec cursorCodec;

    public CursorPageResponse<EventSummaryResponse> findEvents(EventSearchRequest request) {
        log.info("Searching for events: {}", request);
        return query(request, false, mapper::toSummary);
    }

    public CursorPageResponse<EventMapMarkerResponse> findMapEvents(EventSearchRequest request) {
        log.info("Searching for map events: {}", request);
        return query(request, true, mapper::toMapMarker);
    }

    public EventDetailResponse findById(String eventId) {
        log.info("Searching for event: {}", eventId);
        return eventRepository.findById(eventId).map(mapper::toDetail).orElseThrow(() -> new EventNotFoundException(eventId));
    }

    public List<String> findCategories() {
        log.info("Searching for categories");
        return eventRepository.findDistinctCategories();
    }

    private <T> CursorPageResponse<T> query(EventSearchRequest request, boolean requireCoordinates, java.util.function.Function<EventEntity, T> responseMapper) {
        int limit = request.effectiveLimit();

        EventSearchCriteria criteria = new EventSearchCriteria(dateRangeResolver.resolve(request.period()), normalize(request.category()), request.effectivePricing(), request.arrondissement(), normalize(request.query()), cursorCodec.decode(request.cursor()));

        Specification<EventEntity> specification = EventSpecification.matching(criteria);

        if (requireCoordinates) {
            specification = specification.and(EventSpecification.hasCoordinates());
        }

        PageRequest pageRequest = PageRequest.of(0, limit + 1, Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id")));

        List<EventEntity> content = eventRepository.findAll(specification, pageRequest).getContent();

        boolean hasNext = content.size() > limit;

        List<EventEntity> selected = hasNext ? content.subList(0, limit) : content;

        String nextCursor = null;

        if (hasNext && !selected.isEmpty()) {
            EventEntity last = selected.getLast();

            nextCursor = cursorCodec.encode(last.getStartDate(), last.getId());
        }

        return new CursorPageResponse<>(selected.stream().map(responseMapper).toList(), nextCursor, hasNext);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}