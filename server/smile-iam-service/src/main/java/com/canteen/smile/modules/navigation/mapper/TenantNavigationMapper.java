package com.canteen.smile.modules.navigation.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 租户功能、菜单显示配置和账号菜单偏好数据访问接口。 */
public interface TenantNavigationMapper {

    /** @return 当前租户全部功能开关。 */
    List<FeatureRow> selectFeatures(@Param("tenantId") long tenantId);

    /** @return 当前租户管理端全部已发布菜单及显示配置。 */
    List<MenuRow> selectMenus(@Param("tenantId") long tenantId, @Param("accountId") long accountId);

    /** @return 当前账号确实拥有且未被租户统一隐藏的有效菜单。 */
    List<MenuRow> selectPreferenceMenus(@Param("tenantId") long tenantId,
                                        @Param("accountId") long accountId,
                                        @Param("permissionCodes") List<String> permissionCodes);

    /** @return 租户隐藏与个人隐藏合并并级联到子节点后的菜单权限码。 */
    List<String> selectEffectiveHiddenMenuCodes(@Param("tenantId") long tenantId,
                                                @Param("accountId") long accountId,
                                                @Param("permissionCodes") List<String> permissionCodes);

    /** @return 乐观锁修改功能状态的行数。 */
    int updateFeature(@Param("tenantId") long tenantId, @Param("featureCode") String featureCode,
                      @Param("enabled") boolean enabled, @Param("version") long version,
                      @Param("operatorId") long operatorId);

    /** @return 乐观锁修改租户菜单隐藏状态的行数。 */
    int updateTenantMenu(@Param("tenantId") long tenantId, @Param("permissionCode") String permissionCode,
                         @Param("hidden") boolean hidden, @Param("version") long version,
                         @Param("operatorId") long operatorId);

    /** @return 乐观锁修改现有个人菜单偏好的行数。 */
    int updatePreference(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                         @Param("accountId") long accountId, @Param("permissionCode") String permissionCode,
                         @Param("hidden") boolean hidden, @Param("version") long version);

    /** @return 新增个人菜单偏好行数。 */
    int insertPreference(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                         @Param("accountId") long accountId, @Param("permissionCode") String permissionCode,
                         @Param("hidden") boolean hidden);

    /** 提升当前租户全部有效账号授权版本。 */
    int bumpTenantAccountAuthzVersions(@Param("tenantId") long tenantId, @Param("operatorId") long operatorId);

    /** 为租户全部有效账号批量写入会话失效 Outbox，不在 Java 中全量加载账号。 */
    int insertTenantSessionInvalidationOutbox(@Param("tenantId") long tenantId,
                                              @Param("operatorId") long operatorId,
                                              @Param("actionName") String actionName,
                                              @Param("ipAddress") String ipAddress);

    /** 租户功能开关投影。 */
    record FeatureRow(String featureCode, boolean enabled, long version) { }

    /** 租户菜单与当前账号偏好投影。 */
    record MenuRow(long permissionId, Long parentId, String permissionCode, String name, String routePath,
                   String featureCode, boolean featureEnabled, boolean tenantHidden, long tenantVersion,
                   boolean personallyHidden, Long preferenceVersion, int sortOrder) { }
}
