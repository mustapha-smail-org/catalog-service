package com.citypulse.catalog.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(
        @NotBlank String bootstrapServers,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String saslMechanism,
        Resource caCertificate,
        @Valid Topic topic,
        @Valid ConsumerSettings consumer,
        @Valid RetrySettings retry,
        @Valid SchemaRegistry schemaRegistry
) {

    public record Topic(
            @NotBlank String events,
            @NotBlank String eventsDlt
    ) {
    }

    public record ConsumerSettings(
            @NotBlank String groupId,
            @NotBlank String autoOffsetReset,
            @Min(1) int concurrency,
            boolean autoStartup
    ) {
    }

    public record RetrySettings(
            @Min(1) long maxAttempts,
            @Min(0) long backoffMs
    ) {
    }

    public record SchemaRegistry(
            @NotBlank String url,
            @NotBlank String username,
            @NotBlank String password,
            boolean autoRegisterSchemas
    ) {
    }
}
