package com.citypulse.catalog.service;

import com.citypulse.catalog.config.CachingConfig;
import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.mapper.EventResponseMapper;
import com.citypulse.catalog.repository.EventFacetRepository;
import com.citypulse.catalog.repository.EventRepository;
import com.citypulse.catalog.utils.DateRange;
import com.citypulse.catalog.utils.DateRangeResolver;
import com.citypulse.catalog.utils.EventCursorCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the read-path cache actually short-circuits the repository. Boots only
 * the caching infrastructure ({@link CachingConfig}) plus the service and mocked
 * collaborators — no JPA/Kafka — so the {@code @Cacheable} proxy is live but the
 * test stays fast. The collaborators are singleton beans, so each test resets and
 * re-stubs them (and clears the caches) to start from a clean slate.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CachingConfig.class, EventQueryCacheTest.TestBeans.class})
@TestPropertySource(properties = {
        "app.cache.enabled=true",
        "app.cache.ttl=10m",
        "app.cache.max-size=1000"
})
class EventQueryCacheTest {

    @Autowired
    private EventQueryService service;
    @Autowired
    private EventRepository repository;
    @Autowired
    private EventResponseMapper mapper;
    @Autowired
    private DateRangeResolver dateRangeResolver;
    @Autowired
    private EventCursorCodec cursorCodec;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void reset_and_stub() {
        reset(repository, mapper, dateRangeResolver, cursorCodec);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

        DateRange range = new DateRange(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
        when(dateRangeResolver.resolve(any())).thenReturn(range);
        when(dateRangeResolver.resolveDate(any())).thenReturn(range);
        when(cursorCodec.decode(any())).thenReturn(null);
        Page<EventEntity> empty = new PageImpl<>(List.of());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(empty);
        when(mapper.toDetail(any())).thenReturn(mock(EventDetailResponse.class));
    }

    @Test
    void secondIdenticalSearchIsServedFromCache() {
        EventSearchRequest request = search("Concert");

        service.findEvents(request);
        service.findEvents(request);

        // Repository hit once; the second call came from the cache.
        verify(repository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void differentCriteriaAreCachedSeparately() {
        service.findEvents(search("Concert"));
        service.findEvents(search("Theatre"));

        verify(repository, times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findByIdIsCachedPerId() {
        when(repository.findById("evt-1")).thenReturn(Optional.of(event("evt-1")));

        service.findById("evt-1");
        service.findById("evt-1");

        verify(repository, times(1)).findById("evt-1");
    }

    @Test
    void evictingTheCacheForcesAReload() {
        EventSearchRequest request = search("Concert");

        service.findEvents(request);
        cacheManager.getCache(CachingConfig.EVENTS).clear();
        service.findEvents(request);

        verify(repository, times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    // --- fixtures ---------------------------------------------------------

    private EventSearchRequest search(String query) {
        return new EventSearchRequest(
                null, null, null, null, null, null, null, query, null, 30, null
        );
    }

    private static EventEntity event(String id) {
        return new EventEntity(id, "Event " + id, Instant.parse("2026-08-20T18:00:00Z"));
    }

    @Configuration
    static class TestBeans {

        @Bean
        EventRepository repository() {
            return mock(EventRepository.class);
        }

        @Bean
        EventFacetRepository facetRepository() {
            return mock(EventFacetRepository.class);
        }

        @Bean
        EventResponseMapper mapper() {
            return mock(EventResponseMapper.class);
        }

        @Bean
        DateRangeResolver dateRangeResolver() {
            return mock(DateRangeResolver.class);
        }

        @Bean
        EventCursorCodec cursorCodec() {
            return mock(EventCursorCodec.class);
        }

        @Bean
        EventQueryService eventQueryService(EventRepository repository,
                                            EventFacetRepository facetRepository,
                                            EventResponseMapper mapper,
                                            DateRangeResolver dateRangeResolver,
                                            EventCursorCodec cursorCodec) {
            return new EventQueryService(repository, facetRepository, mapper, dateRangeResolver, cursorCodec);
        }
    }
}
