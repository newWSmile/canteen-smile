package com.canteen.smile.modules.permission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 平台权限资源 MyBatis XML 契约测试。 */
class PermissionResourceMapperXmlTest {

    /** 验证权限发布和既有租户同步 SQL 均可被 MyBatis 解析。 */
    @Test
    void shouldParsePermissionMapperXml() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/permission/PermissionResourceMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream, configuration, resource, configuration.getSqlFragments()
            );
            builder.parse();
        }
        String namespace = PermissionResourceMapper.class.getName() + ".";
        assertThat(configuration.hasStatement(namespace + "publishResource")).isTrue();
        assertThat(configuration.hasStatement(namespace + "initializeFeatureForExistingTenants")).isTrue();
        assertThat(configuration.hasStatement(namespace + "initializeMenuForExistingTenants")).isTrue();
    }
}
