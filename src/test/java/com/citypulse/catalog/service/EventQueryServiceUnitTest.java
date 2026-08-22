package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.request.PricingFilter;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.exception.EventNotFoundException;
import com.citypulse.catalog.mapper.EventResponseMapper;
import com.citypulse.catalog.repository.EventRepository;
import com.citypulse.catalog.utils.DateRangeResolver;
import com.citypulse.catalog.utils.EventCursorCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceUnitTest {

    @Mock
    private EventRepository repository;
    @Mock
    private EventResponseMapper mapper;
    @Mock
    private DateRangeResolver dateRangeResolver;
    @Mock
    private EventCursorCodec cursorCodec;

    private EventQueryService service;

    @BeforeEach
    void setUp() {
        service = new EventQueryService(repository, mapper, dateRangeResolver, cursorCodec);
    }

    @Test
    void shouldReturnLimitedSummaryPageAndEncodeNextCursor() {
        EventSearchRequest request = new EventSearchRequest(
                null, "  Cinema  ", null, "1", "  summer  ", null, 2, "cursor"
        );
        EventEntity first = event("event-1", "2026-08-20T18:00:00Z");
        EventEntity second = event("event-2", "2026-08-21T18:00:00Z");
        EventEntity lookahead = event("event-3", "2026-08-22T18:00:00Z");
        EventSummaryResponse firstResponse = org.mockito.Mockito.mock(EventSummaryResponse.class);
        EventSummaryResponse secondResponse = org.mockito.Mockito.mock(EventSummaryResponse.class);

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second, lookahead)));
        when(mapper.toSummary(first)).thenReturn(firstResponse);
        when(mapper.toSummary(second)).thenReturn(secondResponse);
        when(cursorCodec.encode(second.getStartDate(), "event-2")).thenReturn("next-cursor");

        var result = service.findEvents(request);

        assertThat(result.items()).containsExactly(firstResponse, secondResponse);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("next-cursor");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
        assertThat(pageable.getValue().getSort().getOrderFor("startDate")).isNotNull();
        verify(cursorCodec).decode("cursor");
    }

    @Test
    void shouldReturnMapPageWithoutCursorWhenNoLookaheadExists() {
        EventSearchRequest request = new EventSearchRequest(
                null, "  ", PricingFilter.FREE, null, "  ", null, 10, null
        );
        EventEntity event = event("event-1", "2026-08-20T18:00:00Z");
        EventMapMarkerResponse marker = org.mockito.Mockito.mock(EventMapMarkerResponse.class);

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(mapper.toMapMarker(event)).thenReturn(marker);

        var result = service.findMapEvents(request);

        assertThat(result.items()).containsExactly(marker);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void shouldReturnMappedDetail() {
        EventEntity event = event("event-42", "2026-08-20T18:00:00Z");
        EventDetailResponse detail = org.mockito.Mockito.mock(EventDetailResponse.class);
        when(repository.findById("event-42")).thenReturn(Optional.of(event));
        when(mapper.toDetail(event)).thenReturn(detail);

        assertThat(service.findById("event-42")).isSameAs(detail);
    }

    @Test
    void shouldReturnMappedDetailBySlug() {
        EventEntity event = event("event-42", "2026-08-20T18:00:00Z");
        EventDetailResponse detail = org.mockito.Mockito.mock(EventDetailResponse.class);
        when(repository.findBySlug(event.getSlug())).thenReturn(Optional.of(event));
        when(mapper.toDetail(event)).thenReturn(detail);

        assertThat(service.findBySlug(event.getSlug())).isSameAs(detail);
    }

    @Test
    void shouldRejectUnknownEventId() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing"))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void shouldReturnRepositoryCategories() {
        when(repository.findDistinctCategories()).thenReturn(List.of("Cinema", "Music"));

        assertThat(service.findCategories()).containsExactly("Cinema", "Music");
    }

    private EventEntity event(String id, String start) {
        return new EventEntity(id, "Title", Instant.parse(start));
    }
}
