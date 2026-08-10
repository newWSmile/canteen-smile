package com.canteen.smile.modules.audit.model;

/**
 * 审计稳定编码的中文展示目录。
 *
 * <p>动作名称由事件发生时固化的快照提供；本目录只维护稳定类型等有限展示语义。</p>
 */
public final class AuditDisplayCatalog {

    /** 工具目录不允许实例化。 */
    private AuditDisplayCatalog() {
    }

    /** @param actionNameSnapshot 事件发生时固化的动作名称 @return 中文动作名称 */
    public static String actionName(String actionNameSnapshot) {
        return actionNameSnapshot == null || actionNameSnapshot.isBlank()
                ? "未登记操作"
                : actionNameSnapshot;
    }

    /** @param identityType 身份类型编码 @return 中文身份类型 */
    public static String identityTypeName(String identityType) {
        if (identityType == null || identityType.isBlank()) return "未知身份";
        return switch (identityType) {
            case "PLATFORM_IDENTITY" -> "平台身份";
            case "TENANT_ACCOUNT" -> "租户账号";
            case "SYSTEM" -> "系统";
            case "ANONYMOUS" -> "匿名请求";
            default -> "未知身份";
        };
    }

    /** @param targetType 目标类型编码 @return 中文目标类型 */
    public static String targetTypeName(String targetType) {
        if (targetType == null || targetType.isBlank()) return "未知对象";
        return switch (targetType) {
            case "PLATFORM_IDENTITY" -> "平台身份";
            case "TENANT_ACCOUNT" -> "租户账号";
            case "TENANT" -> "租户";
            case "ORGANIZATION" -> "机构";
            case "ORG_TYPE" -> "机构类型";
            case "ROLE" -> "角色";
            case "PERMISSION_RESOURCE" -> "权限资源";
            default -> "未知对象";
        };
    }

    /** @param loginMethod 登录方式编码 @return 可选中文登录方式 */
    public static String loginMethodName(String loginMethod) {
        if (loginMethod == null || loginMethod.isBlank()) return null;
        return switch (loginMethod) {
            case "PASSWORD" -> "用户名密码";
            case "SMS" -> "手机验证码";
            case "PASSWORD_SMS" -> "密码和手机验证码";
            case "RECOVERY_CODE" -> "一次性恢复码";
            default -> "未登记登录方式";
        };
    }

    /** @param failureReasonCode 失败原因编码 @return 可选中文失败说明 */
    public static String failureReasonName(String failureReasonCode) {
        if (failureReasonCode == null || failureReasonCode.isBlank()) return null;
        return switch (failureReasonCode) {
            case "INVALID_CREDENTIALS" -> "用户名或密码错误";
            case "ACCOUNT_LOCKED" -> "账号暂时锁定";
            case "ACCOUNT_DISABLED" -> "账号已停用";
            case "ACCOUNT_CANCELLED" -> "账号已注销";
            case "TENANT_DISABLED" -> "租户已停用";
            case "CHALLENGE_INVALID" -> "安全挑战无效或已过期";
            default -> "认证未通过";
        };
    }

}
