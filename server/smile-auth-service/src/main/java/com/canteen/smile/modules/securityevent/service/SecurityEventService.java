package com.canteen.smile.modules.securityevent.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.internal.dto.SecurityEventRequest;
import com.canteen.smile.internal.dto.SecurityEventResponse;
import com.canteen.smile.modules.securityevent.mapper.SecurityEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** 幂等消费 IAM 安全事件并立即失效账号全部设备会话。 */
@Service
@RequiredArgsConstructor
public class SecurityEventService {

    /** 当前服务已经实现并承诺消费的事件类型。 */
    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "ACCOUNT_ROLES_CHANGED", "ACCOUNT_VALIDITY_CHANGED", "ACCOUNT_DISABLED",
            "ACCOUNT_ENABLED", "ACCOUNT_CANCELLED", "ROLE_AUTHORIZATION_CHANGED",
            "ACCOUNT_STATUS_CHANGED", "ACCOUNT_USERNAME_CHANGED", "MOBILE_BINDING_CHANGED",
            "PASSWORD_RESET_REQUESTED", "SESSION_INVALIDATION_REQUESTED",
            "TENANT_SECURITY_POLICY_CHANGED", "TENANT_STATUS_CHANGED"
    );

    /** 幂等、会话、快照及审计数据访问接口。 */
    private final SecurityEventMapper mapper;

    /** Sa-Token 全设备会话失效器。 */
    private final TenantSessionInvalidator sessionInvalidator;

    /** Jackson JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 消费单个安全事件；数据库状态与幂等记录处于同一本地事务。
     *
     * @param request 已通过 HMAC 的事件信封
     * @return 首次或重复消费结果
     */
    @Transactional
    public SecurityEventResponse consume(String signedEventId, SecurityEventRequest request) {
        if (!request.eventId().equals(signedEventId)) {
            throw invalid("签名事件标识与请求载荷不一致");
        }
        EventContext context = validate(request);
        String payloadDigest = payloadDigest(request.payload());
        int inserted = mapper.insertConsumedEvent(request.eventId(), request.eventType(), payloadDigest);
        if (inserted == 0) {
            verifyDuplicate(request, payloadDigest);
            return new SecurityEventResponse(request.eventId(), "SUCCESS", true);
        }
        if (inserted != 1) throw new IllegalStateException("Security event idempotency record was not inserted");

        mapper.invalidatePermissionSnapshots(context.tenantId(), context.accountId());
        mapper.invalidateDeviceSessions(context.tenantId(), context.accountId());
        sessionInvalidator.invalidateAll(context.accountId());
        if (mapper.insertSecurityAudit(
                context.tenantId(), context.accountId(), context.usernameSnapshot(), context.displayNameSnapshot(),
                "auth:session:invalidate:" + request.eventType(), context.actionNameSnapshot(),
                context.ipAddress(), hashIp(context.ipAddress()), request.traceId()) != 1) {
            throw new IllegalStateException("Security event audit was not inserted");
        }
        return new SecurityEventResponse(request.eventId(), "SUCCESS", false);
    }

    /** 校验事件边界与冗余身份字段完全一致。 */
    private EventContext validate(SecurityEventRequest request) {
        if (!SUPPORTED_EVENT_TYPES.contains(request.eventType())) {
            throw invalid("不支持的安全事件类型");
        }
        if (!"TENANT_ACCOUNT".equals(request.aggregateType()) || request.tenantId() == null) {
            throw invalid("安全事件聚合类型或租户上下文无效");
        }
        long accountId = positiveLong(request.aggregateId());
        long tenantId = positiveLong(request.tenantId());
        if (!request.aggregateId().equals(textField(request.payload(), "accountId"))
                || !request.tenantId().equals(textField(request.payload(), "tenantId"))) {
            throw invalid("安全事件身份字段不一致");
        }
        return new EventContext(
                tenantId,
                accountId,
                optionalTextField(request.payload(), "usernameSnapshot", 128),
                optionalTextField(request.payload(), "displayNameSnapshot", 128),
                requiredTextField(request.payload(), "actionNameSnapshot", 200),
                optionalTextField(request.payload(), "ipAddress", 128)
        );
    }

    /** 验证重复事件的类型和载荷摘要，禁止复用 eventId 表达其它命令。 */
    private void verifyDuplicate(SecurityEventRequest request, String payloadDigest) {
        SecurityEventMapper.ConsumedEventRow existing = mapper.selectConsumedEvent(request.eventId());
        if (existing == null || !request.eventType().equals(existing.eventType())
                || !payloadDigest.equals(existing.payloadDigest()) || !"SUCCESS".equals(existing.result())) {
            throw new BusinessException("INTERNAL_3102", "事件标识已被其它载荷占用", 409);
        }
    }

    /** 对 JSON 载荷生成稳定 SHA-256 摘要。 */
    private String payloadDigest(JsonNode payload) {
        try {
            return HmacRequestSigner.sha256Hex(objectMapper.writeValueAsBytes(payload));
        } catch (JsonProcessingException exception) {
            throw invalid("安全事件载荷无法序列化");
        }
    }

    /** @param ipAddress 原始客户端 IP @return 用于关联检索的 SHA-256 摘要。 */
    private String hashIp(String ipAddress) {
        return ipAddress == null ? null : HmacRequestSigner.sha256Hex(ipAddress.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** 读取必需的文本身份字段。 */
    private String textField(JsonNode payload, String name) {
        JsonNode value = payload.get(name);
        if (value == null || !value.isTextual()) throw invalid("安全事件缺少身份字段");
        return value.textValue();
    }

    /** 读取 IAM 必需的中文动作名称快照并限制数据库字段长度。 */
    private String requiredTextField(JsonNode payload, String name, int maxLength) {
        JsonNode value = payload.get(name);
        if (value == null || !value.isTextual()
                || value.textValue().isBlank() || value.textValue().length() > maxLength) {
            throw invalid("安全事件动作名称快照格式无效");
        }
        return value.textValue();
    }

    /** 读取 IAM 可选身份快照并限制数据库字段长度。 */
    private String optionalTextField(JsonNode payload, String name, int maxLength) {
        JsonNode value = payload.get(name);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().length() > maxLength) {
            throw invalid("安全事件身份快照格式无效");
        }
        return value.textValue();
    }

    /** 解析正整数业务 ID。 */
    private long positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw invalid("安全事件业务标识无效");
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid("安全事件业务标识无效");
        }
    }

    /** 创建稳定的内部契约错误。 */
    private BusinessException invalid(String message) {
        return new BusinessException("INTERNAL_3101", message, 400);
    }

    /** 已校验的租户账号上下文。 */
    private record EventContext(
            long tenantId,
            long accountId,
            String usernameSnapshot,
            String displayNameSnapshot,
            String actionNameSnapshot,
            String ipAddress
    ) {
    }
}
