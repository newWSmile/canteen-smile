package com.canteen.smile.modules.outbox.service;

import com.canteen.smile.modules.outbox.entity.OutboxEventEntity;
import com.canteen.smile.modules.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

/** Outbox 单事件完成、重试和死亡状态的短事务边界。 */
@Service
@RequiredArgsConstructor
public class OutboxStateService {

    /** Outbox 数据访问接口。 */
    private final OutboxEventMapper mapper;

    /** 将已获 Auth 确认的事件标记为 PUBLISHED。 */
    @Transactional
    public void published(OutboxEventEntity event) {
        requireOne(mapper.markPublished(event.getId(), event.getVersion()), event.getEventId());
    }

    /** 将投递失败事件标记为 RETRY 或 DEAD。 */
    @Transactional
    public void failed(OutboxEventEntity event, int retryCount, OffsetDateTime nextRetryTime,
                       String errorCode, boolean dead) {
        requireOne(mapper.markFailed(event.getId(), event.getVersion(), retryCount,
                nextRetryTime, errorCode, dead), event.getEventId());
    }

    /** 校验乐观锁状态更新结果。 */
    private void requireOne(int rows, String eventId) {
        if (rows != 1) throw new IllegalStateException("Outbox state changed concurrently: " + eventId);
    }
}
