package com.citypulse.catalog.controller;

import com.citypulse.catalog.dto.request.EventSearchRequest;
import com.citypulse.catalog.dto.response.CursorPageResponse;
import com.citypulse.catalog.dto.response.EventDetailResponse;
import com.citypulse.catalog.dto.response.EventMapMarkerResponse;
import com.citypulse.catalog.dto.response.EventSummaryResponse;
import com.citypulse.catalog.service.EventQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventControllerTest {

    private final EventQueryService service = mock(EventQueryService.class);
    private final EventController controller = new EventController(service);
    private final EventSearchRequest request = new EventSearchRequest(
            null, null, null, null, null, null, null, null
    );

    @Test
    void shouldDelegateEventSearch() {
        CursorPageResponse<EventSummaryResponse> expected = new CursorPageResponse<>(List.of(), null, false);
        when(service.findEvents(request)).thenReturn(expected);

        assertThat(controller.findEvents(request)).isSameAs(expected);
    }

    @Test
    void shouldDelegateMapSearch() {
        CursorPageResponse<EventMapMarkerResponse> expected = new CursorPageResponse<>(List.of(), null, false);
        when(service.findMapEvents(request)).thenReturn(expected);

        assertThat(controller.findMapEvents(request)).isSameAs(expected);
    }

    @Test
    void shouldDelegateDetailLookup() {
        EventDetailResponse expected = mock(EventDetailResponse.class);
        when(service.findById("event-42")).thenReturn(expected);

        assertThat(controller.findEvent("event-42")).isSameAs(expected);
    }

    @Test
    void shouldDelegateCategoryLookup() {
        when(service.findCategories()).thenReturn(List.of("Cinema", "Music"));

        assertThat(controller.findCategories()).containsExactly("Cinema", "Music");
        verify(service).findCategories();
    }
}
