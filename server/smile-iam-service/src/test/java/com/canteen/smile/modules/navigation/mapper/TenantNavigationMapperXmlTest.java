package com.canteen.smile.modules.navigation.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.MappedStatement;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户功能、菜单和个人偏好 MyBatis XML 契约测试。 */
class TenantNavigationMapperXmlTest {

    /** 验证租户导航治理关键 SQL 均可被 MyBatis 完整解析。 */
    @Test
    void shouldParseTenantNavigationMapperXml() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/navigation/TenantNavigationMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = TenantNavigationMapper.class.getName() + ".";
        assertThat(configuration.hasStatement(namespace + "selectFeatures")).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectMenus")).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectPreferenceMenus")).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectEffectiveHiddenMenuCodes")).isTrue();
        assertThat(configuration.hasStatement(namespace + "updateFeature")).isTrue();
        assertThat(configuration.hasStatement(namespace + "updateTenantMenu")).isTrue();
        assertThat(configuration.hasStatement(namespace + "updatePreference")).isTrue();
        assertThat(configuration.hasStatement(namespace + "insertTenantSessionInvalidationOutbox")).isTrue();

        MappedStatement preferenceMenus = configuration.getMappedStatement(namespace + "selectPreferenceMenus");
        String sql = preferenceMenus.getBoundSql(java.util.Map.of(
                "tenantId", 1L,
                "accountId", 2L,
                "permissionCodes", java.util.List.of("iam:user:view")
        )).getSql();
        assertThat(sql).contains("permission.permission_code IN");
        assertThat(sql).contains("tenant_hidden_menu");
        assertThat(sql).contains("COALESCE(feature.enabled, false)=true");
        assertThat(sql).contains("permission.permission_code='iam:tenant-navigation:view'");

        MappedStatement hiddenMenus = configuration.getMappedStatement(
                namespace + "selectEffectiveHiddenMenuCodes"
        );
        String hiddenSql = hiddenMenus.getBoundSql(java.util.Map.of(
                "tenantId", 1L,
                "accountId", 2L,
                "permissionCodes", java.util.List.of("iam:user:view")
        )).getSql();
        assertThat(hiddenSql).contains("permission.permission_code IN");
        assertThat(hiddenSql).contains("permission.permission_code<>'iam:tenant-navigation:view'");
    }
}
