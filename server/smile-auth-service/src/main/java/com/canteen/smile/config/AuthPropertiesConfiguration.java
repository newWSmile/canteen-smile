package com.canteen.smile.config;

import com.canteen.smile.internal.client.IamClientProperties;
import com.canteen.smile.internal.client.InternalHmacClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 启用 Auth 引导、IAM Client 和内部 HMAC 配置绑定。 */
@Configuration
@EnableConfigurationProperties({
        BootstrapProperties.class,
        PasswordEnvelopeProperties.class,
        IamClientProperties.class,
        InternalHmacClientProperties.class
})
public class AuthPropertiesConfiguration {
}
