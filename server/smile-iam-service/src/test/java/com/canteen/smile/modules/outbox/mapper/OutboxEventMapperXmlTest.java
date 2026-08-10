package com.canteen.smile.modules.outbox.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** IAM Outbox PostgreSQL 领取与状态更新 XML 契约测试。 */
class OutboxEventMapperXmlTest {

    /** 验证 Outbox Mapper XML 可解析并注册关键语句。 */
    @Test
    void shouldParseOutboxMapperXml() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/outbox/OutboxEventMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = OutboxEventMapper.class.getName() + ".";
        assertThat(configuration.hasStatement(namespace + "claimDeliverable")).isTrue();
        assertThat(configuration.hasStatement(namespace + "markPublished")).isTrue();
        assertThat(configuration.hasStatement(namespace + "markFailed")).isTrue();
    }
}
