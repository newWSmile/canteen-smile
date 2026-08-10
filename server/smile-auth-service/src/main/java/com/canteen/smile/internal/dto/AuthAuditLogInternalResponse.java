package com.canteen.smile.internal.dto;

import java.time.OffsetDateTime;

/**
 * Auth 返回给 IAM 的审计记录，不包含 IP 摘要。
 *
 * @param id 日志 ID
 * @param tenantId 可选租户 ID
 * @param subjectType 可选认证主体类型
 * @param subjectId 可选认证主体 ID
 * @param operatorType 操作者类型
 * @param operatorId 操作者 ID
 * @param actionCode 动作编码
 * @param result 结果
 * @param loginMethod 可选登录方式
 * @param failureReasonCode 可选失败原因码
 * @param maskedMobile 可选脱敏手机号
 * @param deviceSummary 可选脱敏设备摘要
 * @param traceId 链路 ID
 * @param occurredTime 事件发生时间
 */
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
