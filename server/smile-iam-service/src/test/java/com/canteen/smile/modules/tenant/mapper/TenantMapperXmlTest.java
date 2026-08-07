package com.canteen.smile.modules.tenant.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户 MyBatis XML 映射契约测试。 */
class TenantMapperXmlTest {

    /** Mapper XML 的类路径。 */
    private static final String MAPPER_RESOURCE = "mapper/tenant/TenantMapper.xml";

    /** 验证 Mapper XML 可以解析并注册全部查询语句。 */
    @Test
    void shouldParseTenantMapperXml() throws IOException {
        /** 独立 MyBatis 测试配置，不连接数据库。 */
        Configuration configuration = new Configuration();
        try (InputStream inputStream = Resources.getResourceAsStream(MAPPER_RESOURCE)) {
            /** MyBatis XML 映射解析器。 */
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    MAPPER_RESOURCE,
                    configuration.getSqlFragments()
            );
            builder.parse();
        }

        assertThat(configuration.hasStatement(TenantMapper.class.getName() + ".countActiveTenants")).isTrue();
        assertThat(configuration.hasStatement(TenantMapper.class.getName() + ".selectActiveTenantPage")).isTrue();
    }
}
