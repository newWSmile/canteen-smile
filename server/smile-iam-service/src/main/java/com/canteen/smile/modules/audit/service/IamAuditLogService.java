package com.canteen.smile.modules.audit.service;

import com.canteen.smile.modules.audit.entity.IamAuditLogEntity;
import com.canteen.smile.modules.audit.mapper.IamAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM 只追加管理审计日志服务。 */
@Service
@RequiredArgsConstructor
public class IamAuditLogService {

    /** 审计日志数据访问接口。 */
    private final IamAuditLogMapper mapper;

    /**
     * 记录平台身份对租户账号执行的敏感操作结果。
     *
     * @param tenantId 租户 ID
     * @param operatorId 平台身份 ID
     * @param actionCode 动作编码
     * @param actionNameFallback 权限资源不存在时使用的中文动作名称
     * @param targetId 目标账号 ID
     * @param reason 操作原因
     * @param result SUCCESS 或 FAILURE
     */
    @Transactional
    public void recordPlatformAccountAction(
            long tenantId,
            long operatorId,
            String actionCode,
            String actionNameFallback,
            long targetId,
            String reason,
            String result
    ) {
        IamAuditLogEntity entity = new IamAuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setOperatorType("PLATFORM_IDENTITY");
        entity.setOperatorId(operatorId);
        entity.setActionCode(actionCode);
        entity.setActionNameSnapshot(requiredActionName(actionNameFallback));
        entity.setTargetType("TENANT_ACCOUNT");
        entity.setTargetId(Long.toString(targetId));
        entity.setReason(reason);
        entity.setResult(result);
        entity.setTraceId(MDC.get("traceId"));
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("IAM audit log was not inserted");
        }
    }

    /**
     * 记录平台身份执行的平台治理操作。
     *
     * @param operatorId 平台身份 ID
     * @param actionCode 动作编码
     * @param actionNameFallback 权限资源不存在时使用的中文动作名称
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param result SUCCESS 或 FAILURE
     */
    @Transactional
    public void recordPlatformAction(
            long operatorId,
            String actionCode,
            String actionNameFallback,
            String targetType,
            String targetId,
            String result
    ) {
        IamAuditLogEntity entity = new IamAuditLogEntity();
        entity.setOperatorType("PLATFORM_IDENTITY");
        entity.setOperatorId(operatorId);
        entity.setActionCode(actionCode);
        entity.setActionNameSnapshot(requiredActionName(actionNameFallback));
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setResult(result);
        entity.setTraceId(MDC.get("traceId"));
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("IAM platform audit log was not inserted");
        }
    }

    /**
     * 记录要求填写原因的平台敏感治理操作。
     *
     * @param operatorId 平台身份 ID
     * @param actionCode 动作编码
     * @param actionNameFallback 中文动作名称
     * @param targetType 目标类型
     * @param targetId 目标标识
     * @param reason 必填操作原因
     * @param result SUCCESS 或 FAILURE
     */
    @Transactional
    public void recordPlatformActionWithReason(
            long operatorId,
            String actionCode,
            String actionNameFallback,
            String targetType,
            String targetId,
            String reason,
            String result
    ) {
        IamAuditLogEntity entity = new IamAuditLogEntity();
        entity.setOperatorType("PLATFORM_IDENTITY");
        entity.setOperatorId(operatorId);
        entity.setActionCode(actionCode);
        entity.setActionNameSnapshot(requiredActionName(actionNameFallback));
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setReason(reason);
        entity.setResult(result);
        entity.setTraceId(MDC.get("traceId"));
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("IAM platform sensitive audit log was not inserted");
        }
    }

    /**
     * 记录租户账号执行的机构治理操作。
     *
     * @param tenantId 租户 ID
     * @param operatorOrganizationId 操作者所属机构 ID
     * @param operatorId 租户账号 ID
     * @param actionCode 动作编码
     * @param actionNameFallback 权限资源不存在时使用的中文动作名称
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param reason 可选操作原因
     * @param result SUCCESS 或 FAILURE
     */
    @Transactional
    public void recordTenantOrganizationAction(
            long tenantId,
            long operatorOrganizationId,
            long operatorId,
            String actionCode,
            String actionNameFallback,
            String targetType,
            String targetId,
            String reason,
            String result
    ) {
        IamAuditLogEntity entity = new IamAuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setOperatorType("TENANT_ACCOUNT");
        entity.setOperatorId(operatorId);
        entity.setOperatorOrganizationId(operatorOrganizationId);
        entity.setActionCode(actionCode);
        entity.setActionNameSnapshot(requiredActionName(actionNameFallback));
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setReason(reason);
        entity.setResult(result);
        entity.setTraceId(MDC.get("traceId"));
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("IAM tenant audit log was not inserted");
        }
    }

    /** @param actionName 中文动作名称 @return 去除首尾空白后的动作名称 */
    private String requiredActionName(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            throw new IllegalArgumentException("Audit action name must not be blank");
        }
        return actionName.strip();
    }
}
