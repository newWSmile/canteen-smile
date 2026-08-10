package com.canteen.smile.modules.outbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** IAM Outbox 的 XXL-JOB 执行器配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.outbox.xxl-job")
public class OutboxXxlJobProperties {

    /** 是否启用 XXL-JOB 执行器注册。 */
    private boolean enabled;

    /** XXL-JOB 管理端地址，多个地址使用逗号分隔。 */
    private String adminAddresses;

    /** 执行器应用名。 */
    private String appName = "smile-iam-service";

    /** 执行器对外注册地址，可为空。 */
    private String address;

    /** 执行器绑定 IP，可为空。 */
    private String ip;

    /** 执行器通信端口。 */
    private int port = 9998;

    /** 执行器日志目录。 */
    private String logPath = "./logs/xxl-job/iam";

    /** 执行器日志保留天数。 */
    private int logRetentionDays = 30;

    /** XXL-JOB 调度通信令牌，只能通过安全配置提供。 */
    private String accessToken;
}
