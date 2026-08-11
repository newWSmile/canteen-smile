package com.canteen.smile.modules.audit.model;

/** Auth 已实现登录方式对应的审计动作定义。 */
public enum AuthLoginAuditAction {

    /** 用户名密码登录。 */
    PASSWORD("PASSWORD", "auth:login:password", "用户名密码登录"),

    /** 手机号验证码登录。 */
    SMS("SMS", "auth:login:sms", "手机号验证码登录"),

    /** 一次性恢复码登录。 */
    RECOVERY_CODE("RECOVERY_CODE", "auth:login:recovery-code", "恢复码登录");

    /** 设备会话记录的登录方式。 */
    private final String loginMethod;

    /** 稳定审计动作编码。 */
    private final String actionCode;

    /** 写入审计记录的中文动作名称快照。 */
    private final String actionName;

    /** 初始化不可变登录审计动作。 */
    AuthLoginAuditAction(String loginMethod, String actionCode, String actionName) {
        this.loginMethod = loginMethod;
        this.actionCode = actionCode;
        this.actionName = actionName;
    }

    /** @return 稳定审计动作编码 */
    public String actionCode() {
        return actionCode;
    }

    /** @return 中文动作名称快照 */
    public String actionName() {
        return actionName;
    }

    /** @param loginMethod 已验证登录方式 @return 对应审计动作 */
    public static AuthLoginAuditAction fromLoginMethod(String loginMethod) {
        for (AuthLoginAuditAction action : values()) {
            if (action.loginMethod.equals(loginMethod)) return action;
        }
        throw new IllegalArgumentException("Unsupported login method for audit");
    }
}
