package com.citypulse.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventReportRequest(
        @NotNull
        EventReportType type,

        @Size(max = 5_000)
        String message,

        @Email
        @Size(max = 320)
        String email
) {
}
