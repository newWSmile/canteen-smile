package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/**
 * IAM 发送给 Auth 的审计分页查询契约。
 *
 * @param pageNo 页码
 * @param pageSize 页大小
 * @param scopeType 平台或租户作用域
 * @param tenantId 可选租户 ID
 * @param accountId 可选当前账号 ID
 * @param tenantWide 是否允许租户全部记录
 * @param actionCode 可选动作编码
 * @param result 可选结果
 * @param operatorId 可选操作者 ID
 * @param startTime 可选开始时间
 * @param endTime 可选结束时间
 */
public record AuthAuditLogSearchInternalRequest(
        int pageNo,
        int pageSize,
        String scopeType,
        Long tenantId,
        Long accountId,
        boolean tenantWide,
        String actionCode,
        String result,
        Long operatorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
