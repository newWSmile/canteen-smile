package com.canteen.smile.modules.account.model;

/**
 * 租户账号生命周期状态。
 */
public enum AccountStatus {

    /** 账号已创建但尚未设置初始密码。 */
    PENDING_ACTIVATION,

    /** 账号已经激活并可正常使用。 */
    ACTIVE,

    /** 账号已经激活，但下次登录前必须重置密码。 */
    PASSWORD_RESET_REQUIRED,

    /** 账号被管理员停用，可按权限恢复。 */
    DISABLED,

    /** 账号已经不可恢复地注销。 */
    CANCELLED
}
