package com.canteen.smile.modules.navigation.vo;

/** 租户功能开关展示对象。 */
public record TenantFeatureVO(String featureCode, String name, String description,
                              boolean enabled, long version) { }
