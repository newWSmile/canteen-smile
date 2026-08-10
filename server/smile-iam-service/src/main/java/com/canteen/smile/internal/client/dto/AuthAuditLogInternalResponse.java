package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/** Auth 审计查询内部响应契约。 */
public record AuthAuditLogInternalResponse(
        String id,
        String tenantId,
        String subjectType,
        String subjectId,
        String operatorType,
        String operatorId,
        String actionCode,
        String result,
        String loginMethod,
        String failureReasonCode,
        String maskedMobile,
        String deviceSummary,
        String traceId,
        OffsetDateTime occurredTime
) {
}
