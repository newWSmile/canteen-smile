package com.canteen.smile.modules.account.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户用户 MyBatis XML 契约测试。 */
class TenantUserMapperXmlTest {

    /** 验证分页、角色整版替换和生命周期命令 SQL 均可完整解析。 */
    @Test
    void shouldParseTenantUserMapperXml() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/account/TenantUserMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = TenantUserMapper.class.getName() + ".";
        assertThat(configuration.hasStatement(namespace + "selectUsers")).isTrue();
        assertThat(configuration.hasStatement(namespace + "countAssignableRoles")).isTrue();
        assertThat(configuration.hasStatement(namespace + "updateUserProfile")).isTrue();
        assertThat(configuration.hasStatement(namespace + "changeUserStatus")).isTrue();
        assertThat(configuration.hasStatement(namespace + "insertAccountChangedOutbox")).isTrue();
    }
}
