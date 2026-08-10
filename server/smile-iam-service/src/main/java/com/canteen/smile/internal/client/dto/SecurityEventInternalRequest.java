package com.canteen.smile.internal.client.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

/** IAM 向 Auth 投递的 v1 安全事件信封。 */
public record SecurityEventInternalRequest(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String tenantId,
        OffsetDateTime occurredAt,
        int schemaVersion,
        String traceId,
        JsonNode payload
) {
}
