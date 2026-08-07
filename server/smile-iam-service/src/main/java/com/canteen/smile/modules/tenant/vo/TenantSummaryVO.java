package com.canteen.smile.modules.tenant.vo;

import com.canteen.smile.modules.tenant.model.TenantProvisionStatus;
import com.canteen.smile.modules.tenant.model.TenantStatus;

import java.time.OffsetDateTime;

/**
 * 平台端租户分页摘要。
 *
 * @param id 租户 ID，以十进制字符串返回以避免前端精度损失
 * @param tenantCode 租户业务编码
 * @param name 租户名称
 * @param status 租户生命周期状态
 * @param rootOrganizationId 根机构 ID，初始化期间可以为空
 * @param securityVersion 租户安全版本
 * @param templateVersion 机构类型模板版本
 * @param provisionStatus Auth 初始化编排状态
 * @param createdTime 租户创建时间
 * @param version 乐观锁版本
 */
public record TenantSummaryVO(
        String id,
        String tenantCode,
        String name,
        TenantStatus status,
        String rootOrganizationId,
        long securityVersion,
        long templateVersion,
        TenantProvisionStatus provisionStatus,
        OffsetDateTime createdTime,
        long version
) {
}
