package com.canteen.smile.modules.tenant.model;

/** 租户跨 Auth 初始化编排状态。 */
public enum TenantProvisionStatus {

    /** 初始化尚未完成。 */
    INITIALIZING,

    /** 初始化完成并可正常使用。 */
    ACTIVE,

    /** 初始化失败，等待幂等重试。 */
    PROVISION_FAILED
}
