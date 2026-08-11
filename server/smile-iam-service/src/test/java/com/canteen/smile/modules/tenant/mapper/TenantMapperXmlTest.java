package com.canteen.smile.modules.tenant.mapper;

import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.audit.mapper.IamAuditLogMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户 MyBatis XML 映射契约测试。 */
class TenantMapperXmlTest {

    /** 验证租户初始化相关 Mapper XML 可以解析并注册关键语句。 */
    @Test
    void shouldParseTenantMapperXml() throws IOException {
        /** 独立 MyBatis 测试配置，不连接数据库。 */
        Configuration configuration = new Configuration();
        parse(configuration, "mapper/tenant/TenantMapper.xml");
        parse(configuration, "mapper/tenant/TenantProvisionMapper.xml");
        parse(configuration, "mapper/organization/OrgTypeTemplateMapper.xml");
        parse(configuration, "mapper/account/AccountLifecycleMapper.xml");
        parse(configuration, "mapper/audit/IamAuditLogMapper.xml");

        assertThat(configuration.hasStatement(TenantMapper.class.getName() + ".countActiveTenants")).isTrue();
        assertThat(configuration.hasStatement(TenantMapper.class.getName() + ".selectById")).isTrue();
        assertThat(configuration.hasStatement(TenantProvisionMapper.class.getName() + ".insertTenant")).isTrue();
        assertThat(configuration.hasStatement(TenantProvisionMapper.class.getName() + ".insertProvisionOutbox")).isTrue();
        assertThat(configuration.hasStatement(AccountLifecycleMapper.class.getName()
                + ".markPasswordResetRequired")).isTrue();
        assertThat(configuration.hasStatement(AccountLifecycleMapper.class.getName()
                + ".completePasswordReset")).isTrue();
        assertThat(configuration.hasStatement(AccountLifecycleMapper.class.getName()
                + ".selectTenantPermissionContext")).isTrue();
        assertThat(configuration.hasStatement(IamAuditLogMapper.class.getName() + ".insert")).isTrue();
        assertThat(configuration.hasStatement(IamAuditLogMapper.class.getName() + ".insertAsync")).isTrue();
        assertThat(configuration.hasStatement(IamAuditLogMapper.class.getName() + ".selectPage")).isTrue();
    }

    /** 解析一个 Mapper XML 资源。 */
    private void parse(Configuration configuration, String resource) throws IOException {
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            /** MyBatis XML 映射解析器。 */
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            );
            builder.parse();
        }
    }
}
