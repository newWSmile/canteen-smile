package com.canteen.smile.modules.role.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 角色、权限与数据范围 MyBatis XML 契约测试。 */
class RoleMapperXmlTest {

    /** 验证角色授权关键 SQL 均可被 MyBatis 完整解析。 */
    @Test
    void shouldParseRoleMapperXml() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/role/RoleMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream, configuration, resource, configuration.getSqlFragments()
            );
            builder.parse();
        }
        String namespace = RoleMapper.class.getName() + ".";
        assertThat(configuration.hasStatement(namespace + "selectGrantablePermissions")).isTrue();
        assertThat(configuration.hasStatement(namespace + "upsertRolePermissions")).isTrue();
        assertThat(configuration.hasStatement(namespace + "bumpAssignedAccountAuthzVersions")).isTrue();
        assertThat(configuration.hasStatement(namespace + "insertRoleAuthorizationOutbox")).isTrue();
    }
}
