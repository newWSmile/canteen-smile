package com.canteen.smile.internal.dto;

import com.canteen.smile.common.api.PageConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * IAM 经 HMAC 传入的 Auth 审计分页查询条件。
 *
 * @param pageNo 页码
 * @param pageSize 页大小
 * @param scopeType 平台或租户作用域
 * @param tenantId 租户作用域的租户 ID
 * @param accountId 普通租户管理员的账号 ID
 * @param tenantWide 是否允许租户全部记录
 * @param actionCode 可选动作编码
 * @param result 可选结果
 * @param operatorId 可选操作者 ID
 * @param startTime 可选开始时间
 * @param endTime 可选结束时间
 */
public record AuthAuditLogSearchRequest(
        @Min(1) int pageNo,
        @Min(1) @Max(PageConstants.MAX_PAGE_SIZE) int pageSize,
        @Pattern(regexp = "PLATFORM|TENANT") String scopeType,
        @Positive Long tenantId,
        @Positive Long accountId,
        boolean tenantWide,
        @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9:_-]+") String actionCode,
        @Pattern(regexp = "SUCCESS|FAILURE|DENIED") String result,
        @PositiveOrZero Long operatorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
