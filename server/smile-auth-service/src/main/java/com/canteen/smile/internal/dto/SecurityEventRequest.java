package com.canteen.smile.internal.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** IAM Outbox 投递到 Auth 的 v1 安全事件信封。 */
public record SecurityEventRequest(
        @NotBlank @Size(max = 64) String eventId,
        @NotBlank @Size(max = 128) String eventType,
        @NotBlank @Size(max = 64) String aggregateType,
        @NotBlank @Size(max = 128) String aggregateId,
        @Pattern(regexp = "^[1-9][0-9]*$") String tenantId,
        @NotNull OffsetDateTime occurredAt,
        @Min(1) @Max(1) int schemaVersion,
        @Size(max = 128) String traceId,
        @NotNull JsonNode payload
) {
}
