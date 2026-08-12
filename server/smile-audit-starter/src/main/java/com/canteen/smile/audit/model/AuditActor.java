package com.canteen.smile.audit.model;

/**
 * 业务线程内解析并传递给异步审计线程的登录人不可变快照。
 *
 * @param tenantId 可选租户 ID
 * @param operatorType 操作者类型
 * @param operatorId 操作者 ID；系统或匿名操作使用零
 * @param organizationId 可选所属机构 ID
 * @param username 可选用户名快照
 * @param displayName 可选显示名称快照
 * @param appCode 当前登录入口或调用方应用编码
 */
public record AuditActor(
        Long tenantId,
        String operatorType,
        long operatorId,
        Long organizationId,
        String username,
        String displayName,
        String appCode
) {

    /** @return 无登录主体时使用的明确系统身份，禁止冒用普通用户 */
    public static AuditActor system() {
        return new AuditActor(null, "SYSTEM", 0L, null, null, "系统", "SERVICE");
    }

    /** @return 未通过身份校验前用于登录失败记录的明确匿名主体 */
    public static AuditActor anonymous(String appCode) {
        return new AuditActor(null, "ANONYMOUS", 0L, null, null, "匿名访问者", appCode);
    }
}
