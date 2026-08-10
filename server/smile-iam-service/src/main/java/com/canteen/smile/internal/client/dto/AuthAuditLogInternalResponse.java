package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/**
 * Auth 审计查询内部响应契约。
 *
 * @param id 日志 ID
 * @param tenantId 可选租户 ID
 * @param subjectType 可选认证主体类型
 * @param subjectId 可选认证主体 ID
 * @param subjectUsernameSnapshot 主体用户名快照
 * @param subjectDisplayNameSnapshot 主体显示名称快照
 * @param operatorType 操作者类型
 * @param operatorId 操作者 ID
 * @param operatorUsernameSnapshot 操作者用户名快照
 * @param operatorDisplayNameSnapshot 操作者显示名称快照
 * @param actionCode 动作编码
 * @param actionNameSnapshot 中文动作名称快照
 * @param result 结果
 * @param loginMethod 可选登录方式
 * @param failureReasonCode 可选失败原因码
 * @param maskedMobile 可选脱敏手机号
 * @param deviceSummary 可选脱敏设备摘要
 * @param traceId 链路 ID
 * @param occurredTime 发生时间
 */
public record AuthAuditLogInternalResponse(
        String id,
        String tenantId,
        String subjectType,
        String subjectId,
        String subjectUsernameSnapshot,
        String subjectDisplayNameSnapshot,
        String operatorType,
        String operatorId,
        String operatorUsernameSnapshot,
        String operatorDisplayNameSnapshot,
        String actionCode,
        String actionNameSnapshot,
        String result,
        String loginMethod,
        String failureReasonCode,
        String maskedMobile,
        String deviceSummary,
        String traceId,
        OffsetDateTime occurredTime
) {
}
