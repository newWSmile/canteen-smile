package com.canteen.smile.modules.audit.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `iam_audit_log` IAM 管理操作审计表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class IamAuditLogEntity {

    /** 通用审计事件全局唯一 ID。 */
    private String eventId;

    /** 通用审计事件契约版本。 */
    private Integer schemaVersion;

    /** 来源服务或业务域编码。 */
    private String sourceCode;

    /** 任意层级中文分类路径 JSON 数组。 */
    private String categoryPathJson;

    /** 操作人所在应用端编码快照。 */
    private String appCodeSnapshot;

    /** 审计日志主键。 */
    private Long id;

    /** 租户 ID。 */
    private Long tenantId;

    /** 操作身份类型。 */
    private String operatorType;

    /** 操作身份 ID。 */
    private Long operatorId;

    /** 事件发生时操作人的用户名快照。 */
    private String operatorUsernameSnapshot;

    /** 事件发生时操作人的显示名称快照。 */
    private String operatorDisplayNameSnapshot;

    /** 租户操作者执行操作时所属的机构 ID。 */
    private Long operatorOrganizationId;

    /** 审计动作编码。 */
    private String actionCode;

    /** 事件发生时的中文动作名称快照。 */
    private String actionNameSnapshot;

    /** 被操作目标类型。 */
    private String targetType;

    /** 被操作目标 ID。 */
    private String targetId;

    /** 事件发生时被操作目标的名称快照。 */
    private String targetNameSnapshot;

    /** 事件发生时被操作目标的业务编码快照。 */
    private String targetCodeSnapshot;

    /** 敏感操作原因。 */
    private String reason;

    /** 操作结果。 */
    private String result;

    /** 可选稳定失败原因码。 */
    private String failureReasonCode;

    /** 被审计业务方法执行耗时毫秒数。 */
    private Long durationMs;

    /** 请求链路追踪 ID。 */
    private String traceId;

    /** 审计事件发生并落库的时间。 */
    private OffsetDateTime createdTime;

    /** 业务审计事件实际发生时间。 */
    private OffsetDateTime occurredTime;

    /** 创建身份 ID。 */
    private Long createdBy;

    /** 最后更新身份 ID。 */
    private Long updatedBy;
}
