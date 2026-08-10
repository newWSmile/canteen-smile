package com.canteen.smile.modules.auth.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Auth MyBatis XML 映射契约测试。 */
class AuthMapperXmlTest {

    /** 验证新增 Auth Mapper XML 可以解析并注册关键语句。 */
    @Test
    void shouldParseAllAuthMapperXml() throws IOException {
        /** 独立 MyBatis 测试配置，不连接数据库。 */
        Configuration configuration = new Configuration();
        /** 当前 Auth Mapper XML 资源集合。 */
        List<String> resources = List.of(
                "mapper/auth/CredentialMapper.xml",
                "mapper/auth/PlatformRecoveryCodeMapper.xml",
                "mapper/auth/LoginFailureMapper.xml",
                "mapper/auth/DeviceSessionMapper.xml",
                "mapper/auth/ReauthTicketMapper.xml",
                "mapper/auth/PasswordResetTicketMapper.xml",
                "mapper/auth/PasswordHistoryMapper.xml",
                "mapper/securityevent/SecurityEventMapper.xml",
                "mapper/audit/AuthAuditLogMapper.xml"
        );
        for (String resource : resources) {
            parse(configuration, resource);
        }

        assertThat(configuration.hasStatement(CredentialMapper.class.getName() + ".selectBySubject")).isTrue();
        assertThat(configuration.hasStatement(CredentialMapper.class.getName()
                + ".insertPendingTenantAccountCredential")).isTrue();
        assertThat(configuration.hasStatement(PlatformRecoveryCodeMapper.class.getName()
                + ".consumeRecoveryCode")).isTrue();
        assertThat(configuration.hasStatement(LoginFailureMapper.class.getName()
                + ".recordPasswordFailure")).isTrue();
        assertThat(configuration.hasStatement(DeviceSessionMapper.class.getName()
                + ".insertDeviceSession")).isTrue();
        assertThat(configuration.hasStatement(ReauthTicketMapper.class.getName() + ".consume")).isTrue();
        assertThat(configuration.hasStatement(PasswordResetTicketMapper.class.getName() + ".insert")).isTrue();
        assertThat(configuration.hasStatement(PasswordHistoryMapper.class.getName()
                + ".selectRecentHashes")).isTrue();
        assertThat(configuration.hasStatement("com.canteen.smile.modules.securityevent.mapper.SecurityEventMapper"
                + ".insertConsumedEvent")).isTrue();
        assertThat(configuration.hasStatement("com.canteen.smile.modules.securityevent.mapper.SecurityEventMapper"
                + ".invalidateDeviceSessions")).isTrue();
        assertThat(configuration.hasStatement("com.canteen.smile.modules.audit.mapper.AuthAuditLogMapper"
                + ".selectPage")).isTrue();
        assertThat(configuration.hasStatement("com.canteen.smile.modules.audit.mapper.AuthAuditLogMapper"
                + ".insertSessionCreatedAudit")).isTrue();
    }

    /** 将一个 Mapper XML 注册到测试配置。 */
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
