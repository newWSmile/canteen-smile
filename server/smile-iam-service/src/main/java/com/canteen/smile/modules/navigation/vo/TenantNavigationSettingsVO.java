package com.canteen.smile.modules.navigation.vo;

import java.util.List;

/** 租户功能与统一菜单显示配置聚合展示对象。 */
public record TenantNavigationSettingsVO(List<TenantFeatureVO> features, List<TenantMenuSettingVO> menus) {
    /** 创建不可变列表副本。 */
    public TenantNavigationSettingsVO {
        features = List.copyOf(features);
        menus = List.copyOf(menus);
    }
}
