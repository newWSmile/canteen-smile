package com.canteen.smile.audit.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 可由本地异步事件或未来 MQ 传输的版本化通用审计事件。
 *
 * @param eventId 全局唯一事件 ID，用于未来 MQ 幂等消费
 * @param schemaVersion 事件契约版本
 * @param source 来源服务或业务域编码
 * @param categoryPath 与菜单无关的任意长度中文分类路径快照
 * @param actionCode 稳定动作编码
 * @param actionName 中文动作名称快照
 * @param targetType 目标类型
 * @param targetId 目标 ID
 * @param targetName 目标名称快照
 * @param targetCode 目标业务编码快照
 * @param reason 可选操作原因
 * @param result SUCCESS、FAILURE 或 DENIED
 * @param failureReasonCode 可选失败原因码
 * @param maskedMobile 可选脱敏手机号
 * @param loginMethod 可选登录或再认证方式
 * @param deviceSummary 可选脱敏设备摘要
 * @param ipAddress 完整客户端 IP；非 HTTP 系统任务可以为空
 * @param ipHash 客户端 IP 不可逆摘要
 * @param actor 登录人不可变快照
 * @param traceId 请求链路 ID
 * @param occurredTime 业务操作实际发生时间
 * @param durationMs 方法执行耗时毫秒数
 */
public record AuditEvent(
        String eventId,
        int schemaVersion,
        String source,
        List<String> categoryPath,
        String actionCode,
        String actionName,
        String targetType,
        String targetId,
        String targetName,
        String targetCode,
        String reason,
        String result,
        String failureReasonCode,
        String maskedMobile,
        String loginMethod,
        String deviceSummary,
        String ipAddress,
        String ipHash,
        AuditActor actor,
        String traceId,
        OffsetDateTime occurredTime,
        long durationMs
) {
}
