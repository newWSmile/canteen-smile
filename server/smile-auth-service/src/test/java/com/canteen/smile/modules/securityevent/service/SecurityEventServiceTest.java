package com.canteen.smile.modules.securityevent.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.dto.SecurityEventRequest;
import com.canteen.smile.internal.dto.SecurityEventResponse;
import com.canteen.smile.modules.securityevent.mapper.SecurityEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Auth 安全事件幂等消费与全设备失效服务测试。 */
class SecurityEventServiceTest {

    /** 验证首次消费会失效快照、数据库会话、Sa-Token 会话并记录审计。 */
    @Test
    void shouldInvalidateAllSessionsOnFirstConsumption() {
        SecurityEventMapper mapper = mock(SecurityEventMapper.class);
        TenantSessionInvalidator invalidator = mock(TenantSessionInvalidator.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SecurityEventRequest request = request(objectMapper);
        when(mapper.insertConsumedEvent(request.eventId(), request.eventType(), digest(objectMapper, request.payload())))
                .thenReturn(1);
        when(mapper.insertSecurityAudit(2L, 9L,
                "auth:session:invalidate:ACCOUNT_ROLES_CHANGED", request.traceId())).thenReturn(1);
        SecurityEventService service = new SecurityEventService(mapper, invalidator, objectMapper);

        SecurityEventResponse response = service.consume(request.eventId(), request);

        verify(mapper).invalidatePermissionSnapshots(2L, 9L);
        verify(mapper).invalidateDeviceSessions(2L, 9L);
        verify(invalidator).invalidateAll(9L);
        assertThat(response.alreadyConsumed()).isFalse();
    }

    /** 验证同事件同载荷重复投递直接成功，不重复执行会话失效。 */
    @Test
    void shouldReturnSuccessForIdenticalDuplicate() {
        SecurityEventMapper mapper = mock(SecurityEventMapper.class);
        TenantSessionInvalidator invalidator = mock(TenantSessionInvalidator.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SecurityEventRequest request = request(objectMapper);
        String digest = digest(objectMapper, request.payload());
        when(mapper.insertConsumedEvent(request.eventId(), request.eventType(), digest)).thenReturn(0);
        when(mapper.selectConsumedEvent(request.eventId())).thenReturn(
                new SecurityEventMapper.ConsumedEventRow(request.eventId(), request.eventType(), digest, "SUCCESS")
        );
        SecurityEventService service = new SecurityEventService(mapper, invalidator, objectMapper);

        SecurityEventResponse response = service.consume(request.eventId(), request);

        verify(invalidator, never()).invalidateAll(9L);
        assertThat(response.alreadyConsumed()).isTrue();
    }

    /** 验证 eventId 被不同载荷复用时拒绝消费。 */
    @Test
    void shouldRejectEventIdReuseWithDifferentPayload() {
        SecurityEventMapper mapper = mock(SecurityEventMapper.class);
        TenantSessionInvalidator invalidator = mock(TenantSessionInvalidator.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SecurityEventRequest request = request(objectMapper);
        String digest = digest(objectMapper, request.payload());
        when(mapper.insertConsumedEvent(request.eventId(), request.eventType(), digest)).thenReturn(0);
        when(mapper.selectConsumedEvent(request.eventId())).thenReturn(
                new SecurityEventMapper.ConsumedEventRow(request.eventId(), request.eventType(), "0".repeat(64), "SUCCESS")
        );
        SecurityEventService service = new SecurityEventService(mapper, invalidator, objectMapper);

        assertThatThrownBy(() -> service.consume(request.eventId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo("INTERNAL_3102");
    }

    /** 创建真实契约字段的账号角色变更事件。 */
    private SecurityEventRequest request(ObjectMapper objectMapper) {
        JsonNode payload = objectMapper.createObjectNode().put("tenantId", "2").put("accountId", "9");
        return new SecurityEventRequest(
                "event-1", "ACCOUNT_ROLES_CHANGED", "TENANT_ACCOUNT", "9", "2",
                OffsetDateTime.now(), 1, "trace-1", payload
        );
    }

    /** 使用生产代码相同算法生成载荷摘要。 */
    private String digest(ObjectMapper objectMapper, JsonNode payload) {
        try {
            return com.canteen.smile.infrastructure.security.HmacRequestSigner.sha256Hex(
                    objectMapper.writeValueAsBytes(payload));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
