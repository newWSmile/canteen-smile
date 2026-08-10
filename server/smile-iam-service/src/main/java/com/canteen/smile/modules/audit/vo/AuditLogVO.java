package com.canteen.smile.modules.audit.vo;

import java.time.OffsetDateTime;

/**
 * 前端可见的统一审计日志视图，不暴露 IP 摘要等敏感内部字段。
 *
 * @param id 审计日志 ID
 * @param source 审计来源 IAM 或 AUTH
 * @param tenantId 可选租户 ID
 * @param operatorType 操作者身份类型
 * @param operatorId 操作者 ID
 * @param actionCode 动作编码
 * @param targetType IAM 目标类型或 Auth 主体类型
 * @param targetId IAM 目标 ID 或 Auth 主体 ID
 * @param result 操作结果
 * @param reason IAM 敏感操作原因
 * @param loginMethod Auth 登录方式
 * @param failureReasonCode Auth 失败原因码
 * @param maskedMobile 脱敏手机号
 * @param deviceSummary 脱敏设备摘要
 * @param traceId 请求链路 ID
 * @param occurredTime 事件发生时间
 */
public record AuditLogVO(
        String id,
        String source,
        String tenantId,
        String operatorType,
        String operatorId,
        String actionCode,
        String targetType,
        String targetId,
        String result,
        String reason,
        String loginMethod,
        String failureReasonCode,
        String maskedMobile,
        String deviceSummary,
        String traceId,
        OffsetDateTime occurredTime
) {
}
