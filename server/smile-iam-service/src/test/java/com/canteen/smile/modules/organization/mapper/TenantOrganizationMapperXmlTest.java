package com.canteen.smile.modules.organization.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户机构治理 MyBatis XML 契约测试。 */
class TenantOrganizationMapperXmlTest {

    /** 验证机构类型、关系、闭包树关键语句均可被 MyBatis 解析。 */
    @Test
    void shouldParseOrganizationMapperXml() throws IOException {
        /** 不连接数据库的独立 MyBatis 配置。 */
        Configuration configuration = new Configuration();
        /** 机构治理 Mapper XML 资源。 */
        String resource = "mapper/organization/TenantOrganizationMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream, configuration, resource, configuration.getSqlFragments()
            );
            builder.parse();
        }

        String namespace = TenantOrganizationMapper.class.getName() + ".";
        assertThat(configuration.hasStatement(namespace + "selectOrganizationTypes")).isTrue();
        assertThat(configuration.hasStatement(namespace + "upsertOrganizationTypeRelations")).isTrue();
        assertThat(configuration.hasStatement(namespace + "insertOrganizationClosure")).isTrue();
        assertThat(configuration.hasStatement(namespace + "insertMovedOrganizationPaths")).isTrue();
        assertThat(configuration.hasStatement(namespace + "countOrganizationDeleteDependencies")).isTrue();
    }
}
