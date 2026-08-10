package com.canteen.smile.modules.outbox.service;

import com.canteen.smile.modules.outbox.config.OutboxDeliveryProperties;
import com.canteen.smile.modules.outbox.entity.OutboxEventEntity;
import com.canteen.smile.modules.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

/** Outbox 事件短事务领取边界，不在事务中执行远程调用。 */
@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    /** Outbox 数据访问接口。 */
    private final OutboxEventMapper mapper;

    /** 投递批次与租约配置。 */
    private final OutboxDeliveryProperties properties;

    /** 返回已原子转为 PROCESSING 的有界事件批次。 */
    @Transactional
    public List<OutboxEventEntity> claim() {
        OffsetDateTime expiredBefore = OffsetDateTime.now().minusSeconds(properties.getProcessingLeaseSeconds());
        return mapper.claimDeliverable(properties.getBatchSize(), expiredBefore);
    }
}
