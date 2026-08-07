package com.canteen.smile.infrastructure.async;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 统一业务线程池配置。 */
@ConfigurationProperties(prefix = "application.async")
public record AsyncProperties(
        int corePoolSize,
        int maxPoolSize,
        int queueCapacity,
        int keepAliveSeconds,
        int taskTimeoutSeconds
) {
}
