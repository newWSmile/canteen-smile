package com.canteen.smile.modules.platform.model;

/** 平台身份生命周期状态。 */
public enum PlatformIdentityStatus {

    /** Auth 凭证尚未完成初始化。 */
    INITIALIZING,

    /** 平台身份可以正常登录和授权。 */
    ACTIVE,

    /** 平台身份被管理员停用。 */
    DISABLED
}
