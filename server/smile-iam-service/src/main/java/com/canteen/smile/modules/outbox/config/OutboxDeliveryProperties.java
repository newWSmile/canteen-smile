package com.canteen.smile.modules.outbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** IAM Outbox 有界批次、租约与指数退避配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.outbox.delivery")
public class OutboxDeliveryProperties {

    /** 单次任务领取上限。 */
    private int batchSize = 50;

    /** 最大失败次数，达到后进入 DEAD。 */
    private int maxAttempts = 10;

    /** 指数退避基础秒数。 */
    private long baseBackoffSeconds = 5;

    /** 单次退避最大秒数。 */
    private long maxBackoffSeconds = 300;

    /** PROCESSING 状态的租约秒数。 */
    private long processingLeaseSeconds = 60;
}
