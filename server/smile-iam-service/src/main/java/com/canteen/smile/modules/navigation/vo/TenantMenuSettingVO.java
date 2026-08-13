package com.canteen.smile.modules.navigation.vo;

/** 租户管理端菜单显示配置展示对象。 */
public record TenantMenuSettingVO(String permissionCode, String parentPermissionCode, String name,
                                  String routePath, String featureCode, boolean featureEnabled,
                                  boolean tenantHidden, long tenantVersion,
                                  boolean personallyHidden, long preferenceVersion, int sortOrder) { }
