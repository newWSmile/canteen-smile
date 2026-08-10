package com.canteen.smile.modules.outbox.mapper;

import com.canteen.smile.modules.outbox.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** IAM Outbox 领取、完成和失败状态的数据访问接口。 */
public interface OutboxEventMapper {

    /**
     * 通过 PostgreSQL SKIP LOCKED 原子领取到期事件，并恢复超时的处理中事件。
     *
     * @param batchSize 单批最大事件数
     * @param processingExpiredBefore 处理中租约过期边界
     * @return 已转为处理中状态的事件
     */
    List<OutboxEventEntity> claimDeliverable(
            @Param("batchSize") int batchSize,
            @Param("processingExpiredBefore") OffsetDateTime processingExpiredBefore
    );

    /** @return 成功标记为已发布的行数。 */
    int markPublished(@Param("id") long id, @Param("version") long version);

    /**
     * 标记可重试或死亡事件。
     *
     * @return 成功更新的行数
     */
    int markFailed(@Param("id") long id, @Param("version") long version,
                   @Param("retryCount") int retryCount,
                   @Param("nextRetryTime") OffsetDateTime nextRetryTime,
                   @Param("errorCode") String errorCode,
                   @Param("dead") boolean dead);
}
