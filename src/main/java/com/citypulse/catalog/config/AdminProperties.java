package com.citypulse.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credentials for the admin read endpoints (feedback + event reports).
 *
 * @param token shared secret required in the {@code X-Admin-Token} header; when
 *              blank the admin endpoints deny every request (secure by default)
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        String token
) {
}
