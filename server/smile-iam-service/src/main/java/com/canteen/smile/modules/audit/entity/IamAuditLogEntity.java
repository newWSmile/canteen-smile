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

    /** 审计日志主键。 */
    private Long id;

    /** 租户 ID。 */
    private Long tenantId;

    /** 操作身份类型。 */
    private String operatorType;

    /** 操作身份 ID。 */
    private Long operatorId;

    /** 租户操作者执行操作时所属的机构 ID。 */
    private Long operatorOrganizationId;

    /** 审计动作编码。 */
    private String actionCode;

    /** 被操作目标类型。 */
    private String targetType;

    /** 被操作目标 ID。 */
    private String targetId;

    /** 敏感操作原因。 */
    private String reason;

    /** 操作结果。 */
    private String result;

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
