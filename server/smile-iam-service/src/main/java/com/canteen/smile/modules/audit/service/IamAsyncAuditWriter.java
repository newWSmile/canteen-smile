package com.canteen.smile.modules.audit.service;

import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditEvent;
import com.canteen.smile.modules.audit.entity.IamAuditLogEntity;
import com.canteen.smile.modules.audit.mapper.IamAuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 将 IAM 通用审计事件幂等写入独立新事务。 */
@Service
@RequiredArgsConstructor
public class IamAsyncAuditWriter {

    /** IAM 审计数据访问接口。 */
    private final IamAuditLogMapper mapper;

    /** Jackson JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** @param event 业务线程已经固化操作人快照的通用审计事件 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEvent event) {
        mapper.insertAsync(toEntity(event));
    }

    /** @return IAM 服务自有审计实体 */
    private IamAuditLogEntity toEntity(AuditEvent event) {
        AuditActor actor = event.actor();
        IamAuditLogEntity entity = new IamAuditLogEntity();
        entity.setEventId(event.eventId());
        entity.setSchemaVersion(event.schemaVersion());
        entity.setSourceCode(event.source());
        entity.setCategoryPathJson(categoryPathJson(event));
        entity.setAppCodeSnapshot(actor.appCode());
        entity.setTenantId(actor.tenantId());
        entity.setOperatorType(actor.operatorType());
        entity.setOperatorId(actor.operatorId());
        entity.setOperatorUsernameSnapshot(actor.username());
        entity.setOperatorDisplayNameSnapshot(actor.displayName());
        entity.setOperatorOrganizationId(actor.organizationId());
        entity.setActionCode(event.actionCode());
        entity.setActionNameSnapshot(event.actionName());
        entity.setTargetType(event.targetType());
        entity.setTargetId(event.targetId());
        entity.setTargetNameSnapshot(event.targetName());
        entity.setTargetCodeSnapshot(event.targetCode());
        entity.setReason(event.reason());
        entity.setResult(event.result());
        entity.setFailureReasonCode(event.failureReasonCode());
        entity.setIpAddress(event.ipAddress());
        entity.setIpHash(event.ipHash());
        entity.setTraceId(event.traceId());
        entity.setOccurredTime(event.occurredTime());
        entity.setDurationMs(event.durationMs());
        entity.setCreatedBy(actor.operatorId());
        entity.setUpdatedBy(actor.operatorId());
        return entity;
    }

    /** @return PostgreSQL jsonb 字段使用的分类路径 JSON 数组 */
    private String categoryPathJson(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event.categoryPath());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Audit category path serialization failed", exception);
        }
    }
}
