package com.canteen.smile.modules.auth.model;

/** 手机号验证后账号选择票据允许绑定的唯一业务流程。 */
public enum AccountSelectorFlow {

    /** 使用手机号验证码登录。 */
    LOGIN,

    /** 使用手机号验证码自助找回密码。 */
    PASSWORD_RESET
}
