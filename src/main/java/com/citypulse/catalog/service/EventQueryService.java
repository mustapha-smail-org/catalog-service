package com.citypulse.catalog.service;

import com.citypulse.catalog.config.CachingConfig;
import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.request.EventSort;
import com.citypulse.catalog.exception.InvalidCursorException;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventFacetsResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.exception.EventNotFoundException;
import com.citypulse.catalog.mapper.EventResponseMapper;
import com.citypulse.catalog.repository.EventFacetRepository;
import com.citypulse.catalog.repository.EventRepository;
import com.citypulse.catalog.specification.EventSpecification;
import com.citypulse.catalog.utils.DateRange;
import com.citypulse.catalog.utils.DateRangeResolver;
import com.citypulse.catalog.utils.EventCursorCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final EventFacetRepository eventFacetRepository;
    private final EventResponseMapper mapper;
    private final DateRangeResolver dateRangeResolver;
    private final EventCursorCodec cursorCodec;

    @Cacheable(CachingConfig.EVENTS)
    public CursorPageResponse<EventSummaryResponse> findEvents(EventSearchRequest request) {
        log.info("Searching for events: {}", request);
        return query(request, false, mapper::toSummary);
    }

    @Cacheable(CachingConfig.EVENTS_MAP)
    public CursorPageResponse<EventMapMarkerResponse> findMapEvents(EventSearchRequest request) {
        log.info("Searching for map events: {}", request);
        return query(request, true, mapper::toMapMarker);
    }

    @Cacheable(CachingConfig.EVENT_BY_ID)
    public EventDetailResponse findById(String eventId) {
        log.info("Searching for event: {}", eventId);
        return eventRepository.findById(eventId).map(mapper::toDetail).orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Cacheable(CachingConfig.EVENT_BY_SLUG)
    public EventDetailResponse findBySlug(String slug) {
        log.info("Searching for event slug: {}", slug);
        return eventRepository.findBySlug(slug).map(mapper::toDetail).orElseThrow(() -> new EventNotFoundException(slug));
    }

    @Cacheable(CachingConfig.CATEGORIES)
    public List<String> findCategories() {
        log.info("Searching for categories");
        return eventRepository.findDistinctCategories();
    }

    @Cacheable(CachingConfig.FACETS)
    public EventFacetsResponse findFacets(EventSearchRequest request) {
        log.info("Searching for facets: {}", request);

        DateRange dateRange = resolveDateRange(request);
        // Drill-down faceting: each dimension's counts honour every other active
        // filter but drop its own selection, so picking one category still shows
        // its siblings as addable options. Facets ignore the cursor (whole match set).
        EventSearchCriteria withoutCategories = new EventSearchCriteria(dateRange, List.of(), request.effectivePricing(), request.effectiveArrondissements(), normalize(request.query()), null);
        EventSearchCriteria withoutArrondissements = new EventSearchCriteria(dateRange, request.effectiveCategories(), request.effectivePricing(), List.of(), normalize(request.query()), null);

        return new EventFacetsResponse(
                eventFacetRepository.countByCategory(withoutCategories),
                eventFacetRepository.countByArrondissement(withoutArrondissements)
        );
    }

    private DateRange resolveDateRange(EventSearchRequest request) {
        return request.date() != null
                ? dateRangeResolver.resolveDate(request.date())
                : dateRangeResolver.resolve(request.period());
    }

    private <T> CursorPageResponse<T> query(EventSearchRequest request, boolean requireCoordinates, java.util.function.Function<EventEntity, T> responseMapper) {
        int limit = request.effectiveLimit();

        DateRange dateRange = resolveDateRange(request);

        EventSort sort = request.effectiveSort();
        EventSearchCriteria criteria = new EventSearchCriteria(dateRange, request.effectiveCategories(), request.effectivePricing(), request.effectiveArrondissements(), normalize(request.query()), decodeCursor(request.cursor(), sort));

        Specification<EventEntity> specification = EventSpecification.matching(criteria);

        if (requireCoordinates) {
            specification = specification.and(EventSpecification.hasCoordinates());
        }

        PageRequest pageRequest = PageRequest.of(0, limit + 1, sortFor(sort));

        List<EventEntity> content = eventRepository.findAll(specification, pageRequest).getContent();

        boolean hasNext = content.size() > limit;

        List<EventEntity> selected = hasNext ? content.subList(0, limit) : content;

        String nextCursor = null;

        if (hasNext && !selected.isEmpty()) {
            nextCursor = cursorCodec.encode(cursorFor(sort, selected.getLast()));
        }

        return new CursorPageResponse<>(selected.stream().map(responseMapper).toList(), nextCursor, hasNext);
    }

    private Sort sortFor(EventSort sort) {
        return switch (sort) {
            case START_DATE -> Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id"));
            case RELEVANCE -> Sort.by(
                    new Sort.Order(Sort.Direction.DESC, "rankScore").nullsLast(),
                    Sort.Order.asc("id"));
        };
    }

    private EventSearchCriteria.CursorPosition cursorFor(EventSort sort, EventEntity last) {
        return switch (sort) {
            case START_DATE -> new EventSearchCriteria.CursorPosition(
                    sort, last.getStartDate(), null, last.getId());
            case RELEVANCE -> new EventSearchCriteria.CursorPosition(
                    sort, null, last.getRankScore(), last.getId());
        };
    }

    private EventSearchCriteria.CursorPosition decodeCursor(String cursor, EventSort sort) {
        EventSearchCriteria.CursorPosition position = cursorCodec.decode(cursor);
        if (position != null && position.sort() != sort) {
            // A cursor issued for one sort cannot be replayed under another.
            throw new InvalidCursorException(
                    new IllegalArgumentException("cursor sort mismatch"));
        }
        return position;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
