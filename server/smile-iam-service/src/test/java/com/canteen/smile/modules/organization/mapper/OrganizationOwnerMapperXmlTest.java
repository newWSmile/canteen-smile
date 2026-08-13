package com.canteen.smile.modules.organization.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 机构所有权 MyBatis XML 契约测试。 */
class OrganizationOwnerMapperXmlTest {

    /** 验证所有权查询、转让和历史记录 SQL 均可被 MyBatis 完整解析。 */
    @Test
    void shouldParseOrganizationOwnerMapperXml() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/organization/OrganizationOwnerMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = OrganizationOwnerMapper.class.getName() + ".";
        assertThat(configuration.hasStatement(namespace + "selectOwner")).isTrue();
        assertThat(configuration.hasStatement(namespace + "transferOwner")).isTrue();
        assertThat(configuration.hasStatement(namespace + "insertHistory")).isTrue();
        assertThat(configuration.hasStatement(namespace + "bumpAccountVersion")).isTrue();
    }
}
