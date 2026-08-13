package com.canteen.smile.modules.navigation.service;

import java.util.Arrays;

/** 平台已发布且允许具备管理权限的租户管理员启停的功能目录。 */
public enum TenantFeatureCatalog {
    ORGANIZATION_TYPE("IAM_ORGANIZATION_TYPE", "机构类型与关系", "维护租户独立的机构类型及允许关系"),
    ORGANIZATION("IAM_ORGANIZATION", "机构树", "维护机构层级、状态和迁移关系"),
    ROLE("IAM_ROLE", "角色与授权", "维护本机构角色、功能权限和数据范围"),
    USER("IAM_USER", "用户管理", "维护本机构账号、角色和账号状态"),
    AUDIT("IAM_AUDIT", "审计日志", "查询授权变化和认证安全审计记录"),
    TENANT_SECURITY("IAM_TENANT_SECURITY", "租户安全", "维护会话、设备、密码和审计保留策略");

    /** 稳定功能编码。 */
    private final String code;
    /** 中文名称。 */
    private final String displayName;
    /** 中文说明。 */
    private final String description;

    TenantFeatureCatalog(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    /** @return 稳定功能编码。 */
    public String code() { return code; }
    /** @return 中文名称。 */
    public String displayName() { return displayName; }
    /** @return 中文说明。 */
    public String description() { return description; }

    /** @return 对应功能目录，不存在时返回空。 */
    public static TenantFeatureCatalog find(String code) {
        return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst().orElse(null);
    }
}
