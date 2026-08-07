package com.canteen.smile.modules.tenant.vo;

/**
 * 租户初始化结果。
 *
 * @param tenant 创建后的租户摘要
 * @param ownerAccountId 首位机构所有者账号 ID
 * @param ownerStatus 首位机构所有者账号状态
 */
public record TenantCreationVO(
        TenantSummaryVO tenant,
        String ownerAccountId,
        String ownerStatus
) {
}
