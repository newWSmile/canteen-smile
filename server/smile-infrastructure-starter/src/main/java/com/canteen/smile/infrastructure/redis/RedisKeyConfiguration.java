package com.canteen.smile.infrastructure.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 启用 Redis Key 命名配置。 */
@Configuration
@EnableConfigurationProperties(RedisKeyProperties.class)
public class RedisKeyConfiguration {
}
