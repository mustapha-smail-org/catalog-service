package com.citypulse.catalog.dto.response;

import java.util.List;

public record EventFacetsResponse(
        List<FacetCountResponse> categories,
        List<FacetCountResponse> arrondissements
) {
}
