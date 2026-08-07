package com.canteen.smile.modules.tenant.model;

/** 租户生命周期状态。 */
public enum TenantStatus {

    /** 租户仍在初始化。 */
    INITIALIZING,

    /** 租户处于正常可用状态。 */
    ACTIVE,

    /** 租户被暂停。 */
    SUSPENDED,

    /** 租户已到期。 */
    EXPIRED,

    /** 租户已不可恢复地注销。 */
    CANCELLED
}
