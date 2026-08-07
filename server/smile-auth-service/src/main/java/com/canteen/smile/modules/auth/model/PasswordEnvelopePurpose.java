package com.canteen.smile.modules.auth.model;

/** 密码信封允许绑定的业务用途，禁止跨接口复用挑战。 */
public enum PasswordEnvelopePurpose {

    /** 首位平台超级管理员初始化密码。 */
    PLATFORM_BOOTSTRAP,

    /** 平台超级管理员用户名密码登录。 */
    PLATFORM_PASSWORD_LOGIN
}
