package com.canteen.smile.modules.outbox.service;

import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.internal.client.dto.SecurityEventInternalRequest;
import com.canteen.smile.modules.outbox.client.AuthSecurityEventClient;
import com.canteen.smile.modules.outbox.client.OutboxDeliveryException;
import com.canteen.smile.modules.outbox.config.OutboxDeliveryProperties;
import com.canteen.smile.modules.outbox.entity.OutboxEventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** 有界投递 IAM Outbox 事件，并按幂等性执行指数退避重试。 */
@Service
@RequiredArgsConstructor
public class OutboxDeliveryService {

    /** 当前投递服务日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(OutboxDeliveryService.class);

    /** 会导致账号全部设备会话失效的已实现事件类型。 */
    private static final Set<String> SECURITY_EVENT_TYPES = Set.of(
            "ACCOUNT_ROLES_CHANGED", "ACCOUNT_VALIDITY_CHANGED", "ACCOUNT_DISABLED",
            "ACCOUNT_ENABLED", "ACCOUNT_CANCELLED", "ROLE_AUTHORIZATION_CHANGED",
            "ACCOUNT_STATUS_CHANGED", "ACCOUNT_USERNAME_CHANGED", "MOBILE_BINDING_CHANGED",
            "PASSWORD_RESET_REQUESTED", "SESSION_INVALIDATION_REQUESTED",
            "TENANT_SECURITY_POLICY_CHANGED", "TENANT_STATUS_CHANGED"
    );

    /** Outbox 事件领取服务。 */
    private final OutboxClaimService claimService;

    /** Outbox 状态落库服务。 */
    private final OutboxStateService stateService;

    /** Auth 安全事件 Client。 */
    private final AuthSecurityEventClient securityEventClient;

    /** Auth 租户账号凭证 Client。 */
    private final AuthTenantAccountClient tenantAccountClient;

    /** Jackson JSON 解析器。 */
    private final ObjectMapper objectMapper;

    /** 投递重试配置。 */
    private final OutboxDeliveryProperties properties;

    /** 返回本次领取并完成处理的事件数量。 */
    public int deliverBatch() {
        List<OutboxEventEntity> events = claimService.claim();
        for (OutboxEventEntity event : events) {
            deliverOne(event);
        }
        return events.size();
    }

    /** 投递单个事件并独立推进状态，避免一个失败事件阻塞整批。 */
    private void deliverOne(OutboxEventEntity event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayloadJson());
            if ("ACCOUNT_PROVISION_REQUESTED".equals(event.getEventType())) {
                tenantAccountClient.provision(
                        event.getEventId(), positiveLong(payload, "accountId"),
                        positiveLong(payload, "tenantId"), positiveLong(payload, "organizationId")
                );
            } else if (SECURITY_EVENT_TYPES.contains(event.getEventType())) {
                securityEventClient.deliver(toSecurityEvent(event, payload));
            } else {
                throw new OutboxDeliveryException("UNSUPPORTED_EVENT_TYPE", true);
            }
            stateService.published(event);
        } catch (OutboxDeliveryException exception) {
            recordFailure(event, exception.getErrorCode(), exception.isPermanent());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            recordFailure(event, "INVALID_EVENT_PAYLOAD", true);
        } catch (RuntimeException exception) {
            log.warn("Outbox event delivery failed unexpectedly: eventId={}, type={}, error={}",
                    event.getEventId(), event.getEventType(), exception.getClass().getSimpleName());
            recordFailure(event, "UNEXPECTED_DELIVERY_ERROR", false);
        }
    }

    /** 将数据库事件转换为 Auth v1 安全事件信封。 */
    private SecurityEventInternalRequest toSecurityEvent(OutboxEventEntity event, JsonNode payload) {
        return new SecurityEventInternalRequest(
                event.getEventId(), event.getEventType(), event.getAggregateType(), event.getAggregateId(),
                event.getTenantId() == null ? null : event.getTenantId().toString(),
                event.getOccurredTime(), event.getSchemaVersion(), event.getTraceId(), payload
        );
    }

    /** 解析并校验载荷中的正整数 ID。 */
    private long positiveLong(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || !value.isTextual()) throw new IllegalArgumentException("Missing event identifier");
        long parsed = Long.parseLong(value.textValue());
        if (parsed <= 0) throw new IllegalArgumentException("Invalid event identifier");
        return parsed;
    }

    /** 计算退避并持久化失败状态，错误详情不写入数据库。 */
    private void recordFailure(OutboxEventEntity event, String errorCode, boolean permanent) {
        int retryCount = event.getRetryCount() + 1;
        boolean dead = permanent || retryCount >= properties.getMaxAttempts();
        stateService.failed(event, retryCount, OffsetDateTime.now().plusSeconds(backoffSeconds(retryCount)),
                truncate(errorCode), dead);
        log.warn("Outbox event delivery not completed: eventId={}, type={}, retryCount={}, dead={}, code={}",
                event.getEventId(), event.getEventType(), retryCount, dead, truncate(errorCode));
    }

    /** 计算带少量抖动且有最大值的指数退避秒数。 */
    private long backoffSeconds(int retryCount) {
        int exponent = Math.min(Math.max(retryCount - 1, 0), 30);
        long exponential = properties.getBaseBackoffSeconds() * (1L << exponent);
        long bounded = Math.min(exponential, properties.getMaxBackoffSeconds());
        long jitterBound = Math.max(1, bounded / 4 + 1);
        return Math.min(properties.getMaxBackoffSeconds(),
                bounded + ThreadLocalRandom.current().nextLong(jitterBound));
    }

    /** 将错误码限制在数据库字段长度内。 */
    private String truncate(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) return "UNKNOWN_DELIVERY_ERROR";
        return errorCode.length() <= 128 ? errorCode : errorCode.substring(0, 128);
    }
}
