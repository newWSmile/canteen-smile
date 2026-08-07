package com.canteen.smile.modules.tenant.service;

/**
 * IAM 本地初始化完成后的跨 Auth 编排上下文。
 *
 * @param tenantId 租户 ID
 * @param accountId 首位所有者账号 ID
 * @param organizationId 根机构 ID
 * @param outboxId 凭证初始化 Outbox 主键
 */
record TenantProvisionContext(long tenantId, long accountId, long organizationId, long outboxId) {
}
