package com.canteen.smile.internal.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 启用 IAM 内部 HMAC 配置绑定。 */
@Configuration
@EnableConfigurationProperties(IamInternalHmacProperties.class)
public class IamInternalHmacConfiguration {
}
