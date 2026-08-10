package com.canteen.smile.modules.sms.model;

/** 已由 Auth 数据库约束确认的短信业务用途。 */
public enum SmsPurpose {

    /** 手机号验证码登录。 */
    LOGIN,

    /** 账号激活。 */
    ACTIVATION,

    /** 自助找回或重置密码。 */
    PASSWORD_RESET,

    /** 首次绑定手机号。 */
    MOBILE_BIND,

    /** 更换已绑定手机号。 */
    MOBILE_CHANGE,

    /** 管理员敏感操作再认证。 */
    ADMIN_REAUTH,

    /** 平台身份高风险登录二次验证。 */
    PLATFORM_SECOND_FACTOR
}
