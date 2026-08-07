package com.canteen.smile.infrastructure.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Redis Key 命名空间配置。 */
@ConfigurationProperties(prefix = "application.redis")
public record RedisKeyProperties(String keyPrefix) {
}
