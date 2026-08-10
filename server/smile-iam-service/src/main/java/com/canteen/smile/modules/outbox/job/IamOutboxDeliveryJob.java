package com.canteen.smile.modules.outbox.job;

import com.canteen.smile.modules.outbox.service.OutboxDeliveryService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** XXL-JOB 调度的 IAM Outbox 有界投递处理器。 */
@Component
@RequiredArgsConstructor
public class IamOutboxDeliveryJob {

    /** Outbox 投递服务。 */
    private final OutboxDeliveryService deliveryService;

    /** 领取并投递一批到期事件。 */
    @XxlJob("iamOutboxDeliveryJob")
    public void deliver() {
        int processed = deliveryService.deliverBatch();
        XxlJobHelper.log("IAM Outbox delivery completed, claimed events: {}", processed);
    }
}
