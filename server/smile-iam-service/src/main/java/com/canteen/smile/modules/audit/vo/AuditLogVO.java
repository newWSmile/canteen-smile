package com.canteen.smile.modules.audit.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 前端可见的统一审计日志视图，不暴露 IP 摘要等敏感内部字段。
 *
 * @param id 审计日志 ID
 * @param source 审计来源 IAM 或 AUTH
 * @param tenantId 可选租户 ID
 * @param operatorType 操作者身份类型
 * @param operatorTypeName 操作者身份类型中文名称
 * @param operatorId 操作者 ID
 * @param operatorUsername 操作者用户名快照
 * @param operatorDisplayName 操作者显示名称快照
 * @param actionCode 动作编码
 * @param actionName 动作中文名称
 * @param targetType IAM 目标类型或 Auth 主体类型
 * @param targetTypeName 目标类型中文名称
 * @param targetId IAM 目标 ID 或 Auth 主体 ID
 * @param targetName 目标名称快照
 * @param targetCode 目标业务编码或用户名快照
 * @param result 操作结果
 * @param reason IAM 敏感操作原因
 * @param loginMethod Auth 登录方式
 * @param loginMethodName Auth 登录方式中文名称
 * @param failureReasonCode Auth 失败原因码
 * @param failureReason Auth 失败原因中文说明
 * @param maskedMobile 脱敏手机号
 * @param deviceSummary 脱敏设备摘要
 * @param traceId 请求链路 ID
 * @param occurredTime 事件发生时间
 * @param appCode 操作人所在应用端编码快照
 * @param categoryPath 任意层级中文审计分类路径快照
 * @param durationMs 业务方法执行耗时毫秒数
 */
public record AuditLogVO(
        String id,
        String source,
        String tenantId,
        String operatorType,
        String operatorTypeName,
        String operatorId,
        String operatorUsername,
        String operatorDisplayName,
        String actionCode,
        String actionName,
        String targetType,
        String targetTypeName,
        String targetId,
        String targetName,
        String targetCode,
        String result,
        String reason,
        String loginMethod,
        String loginMethodName,
        String failureReasonCode,
        String failureReason,
        String maskedMobile,
        String deviceSummary,
        String traceId,
        OffsetDateTime occurredTime,
        String appCode,
        List<String> categoryPath,
        Long durationMs
) {
}
