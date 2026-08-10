package com.canteen.smile.modules.sms.service;

import com.canteen.smile.modules.sms.client.LocalDatabaseLogSmsClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 短信策略路由测试。 */
class SmsClientRouterTest {

    /** 验证策略编码按大小写无关方式解析。 */
    @Test
    void shouldResolveConfiguredProviderCode() {
        LocalDatabaseLogSmsClient client = new LocalDatabaseLogSmsClient();
        SmsClientRouter router = new SmsClientRouter(List.of(client));

        assertThat(router.resolve("local_database_log")).isSameAs(client);
    }
}
