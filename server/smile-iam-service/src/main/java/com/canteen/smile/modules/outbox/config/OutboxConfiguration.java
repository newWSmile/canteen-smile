package com.canteen.smile.modules.outbox.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** IAM 可靠事件投递及 XXL-JOB 执行器装配。 */
@Configuration
@EnableConfigurationProperties({OutboxDeliveryProperties.class, OutboxXxlJobProperties.class})
public class OutboxConfiguration {

    /**
     * 按配置创建 XXL-JOB 执行器；未启用时不启动注册线程和端口。
     *
     * @param properties 执行器配置
     * @return XXL-JOB Spring 执行器
     */
    @Bean
    @ConditionalOnProperty(prefix = "application.outbox.xxl-job", name = "enabled", havingValue = "true")
    public XxlJobSpringExecutor xxlJobExecutor(OutboxXxlJobProperties properties) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAppname(properties.getAppName());
        executor.setAddress(properties.getAddress());
        executor.setIp(properties.getIp());
        executor.setPort(properties.getPort());
        executor.setLogPath(properties.getLogPath());
        executor.setLogRetentionDays(properties.getLogRetentionDays());
        executor.setAccessToken(properties.getAccessToken());
        return executor;
    }
}
