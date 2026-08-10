package com.canteen.smile.modules.outbox.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** IAM 可靠事件投递状态实体，仅映射 iam_outbox_event 表。 */
@Getter
@Setter
public class OutboxEventEntity {

    /** 事件记录主键。 */
    private long id;

    /** 跨服务全局事件 ID。 */
    private String eventId;

    /** 聚合根类型。 */
    private String aggregateType;

    /** 聚合根业务 ID。 */
    private String aggregateId;

    /** 所属租户 ID，可为空。 */
    private Long tenantId;

    /** 事件类型编码。 */
    private String eventType;

    /** 事件契约版本。 */
    private int schemaVersion;

    /** JSON 文本形式的最小事件载荷。 */
    private String payloadJson;

    /** 已失败重试次数。 */
    private int retryCount;

    /** 原始链路追踪 ID。 */
    private String traceId;

    /** 事件发生时间。 */
    private OffsetDateTime occurredTime;

    /** 当前乐观锁版本。 */
    private long version;
}
