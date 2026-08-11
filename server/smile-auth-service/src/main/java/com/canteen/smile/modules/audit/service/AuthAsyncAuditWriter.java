package com.canteen.smile.modules.audit.service;

import com.canteen.smile.audit.model.AuditActor;
import com.canteen.smile.audit.model.AuditEvent;
import com.canteen.smile.modules.audit.entity.AuthAsyncAuditEntity;
import com.canteen.smile.modules.audit.mapper.AuthAuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 将 Auth 通用审计事件幂等写入独立新事务。 */
@Service
@RequiredArgsConstructor
public class AuthAsyncAuditWriter {

    /** Auth 审计数据访问接口。 */
    private final AuthAuditLogMapper mapper;

    /** Jackson JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** @param event 业务线程已经固化操作人快照的通用审计事件 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEvent event) {
        mapper.insertAsyncAudit(toEntity(event));
    }

    /** @return 不共享跨服务 Entity 的 Auth 自有审计实体 */
    private AuthAsyncAuditEntity toEntity(AuditEvent event) {
        AuditActor actor = event.actor();
        AuthAsyncAuditEntity entity = new AuthAsyncAuditEntity();
        entity.setEventId(event.eventId());
        entity.setSchemaVersion(event.schemaVersion());
        entity.setSourceCode(event.source());
        entity.setCategoryPathJson(categoryPathJson(event));
        entity.setAppCodeSnapshot(actor.appCode());
        entity.setTenantId(actor.tenantId());
        if ("PLATFORM_IDENTITY".equals(actor.operatorType())
                || "TENANT_ACCOUNT".equals(actor.operatorType())) {
            entity.setSubjectType(actor.operatorType());
            entity.setSubjectId(actor.operatorId());
        }
        entity.setOperatorType(actor.operatorType());
        entity.setOperatorId(actor.operatorId());
        entity.setOperatorUsernameSnapshot(actor.username());
        entity.setOperatorDisplayNameSnapshot(actor.displayName());
        entity.setActionCode(event.actionCode());
        entity.setActionNameSnapshot(event.actionName());
        entity.setTargetType(event.targetType());
        entity.setTargetId(event.targetId());
        entity.setTargetNameSnapshot(event.targetName());
        entity.setTargetCodeSnapshot(event.targetCode());
        entity.setReason(event.reason());
        entity.setResult(event.result());
        entity.setFailureReasonCode(event.failureReasonCode());
        entity.setMaskedMobile(event.maskedMobile());
        entity.setTraceId(event.traceId());
        entity.setOccurredTime(event.occurredTime());
        entity.setDurationMs(event.durationMs());
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

