package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.platform.service.UsernameNormalizer;
import com.canteen.smile.modules.tenant.dto.CreateTenantRequest;
import com.canteen.smile.modules.tenant.dto.TenantSecurityPolicyRequest;
import com.canteen.smile.modules.tenant.mapper.TenantProvisionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/** 租户本地初始化和跨服务状态更新的事务边界。 */
@Service
@RequiredArgsConstructor
public class TenantProvisionTransactionService {

    /** 非法初始化参数错误码。 */
    private static final String INVALID_REQUEST_CODE = "IAM_2201";

    /** 幂等键冲突错误码。 */
    private static final String IDEMPOTENCY_CONFLICT_CODE = "IAM_2202";

    /** 已发布模板不存在错误码。 */
    private static final String TEMPLATE_NOT_FOUND_CODE = "IAM_2203";

    /** 结果引用固定前缀。 */
    private static final String TENANT_REFERENCE_PREFIX = "tenant:";

    /** 租户初始化数据访问接口。 */
    private final TenantProvisionMapper mapper;

    /** Jackson 请求摘要序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 在单个 IAM 本地事务中完成租户、根机构、所有者、权限和可靠事件初始化。
     *
     * @param request 创建命令
     * @param idempotencyKey 外部幂等键
     * @param operatorId 平台身份 ID
     * @return 跨 Auth 编排上下文
     */
    @Transactional
    public TenantProvisionContext initialize(CreateTenantRequest request, String idempotencyKey, long operatorId) {
        validatePolicy(request.securityPolicy());
        /** 幂等键摘要。 */
        String keyHash = hash(idempotencyKey.getBytes(StandardCharsets.UTF_8));
        /** 规范请求 JSON 摘要。 */
        String requestHash = hash(serialize(request));
        /** 是否成功占用新的幂等键。 */
        boolean reserved = mapper.insertIdempotency(operatorId, keyHash, requestHash) == 1;
        if (!reserved) {
            return resolveExisting(operatorId, keyHash, requestHash);
        }
        validateTemplateAndRegion(request);

        /** 新租户 ID。 */
        long tenantId = mapper.nextTenantId();
        /** 新根机构 ID。 */
        long organizationId = mapper.nextOrganizationId();
        /** 新所有者账号 ID。 */
        long accountId = mapper.nextAccountId();
        /** 新所有者保护角色 ID。 */
        long roleId = mapper.nextRoleId();
        /** 新 Outbox 主键。 */
        long outboxId = mapper.nextOutboxId();
        /** 去除首尾空白后的租户编码。 */
        String tenantCode = request.tenantCode().strip().toUpperCase(Locale.ROOT);
        /** 去除首尾空白后的租户名称。 */
        String tenantName = request.name().strip();
        /** 去除首尾空白后的机构编码。 */
        String organizationCode = request.rootOrganization().businessCode().strip();
        /** 机构编码永久唯一比较值。 */
        String normalizedOrganizationCode = normalize(organizationCode);
        /** 去除首尾空白后的机构名称。 */
        String organizationName = request.rootOrganization().name().strip();
        /** 机构同级名称唯一比较值。 */
        String normalizedOrganizationName = normalize(organizationName);
        /** 去除首尾空白后的原始用户名。 */
        String username = request.owner().username().strip();
        /** 全平台唯一用户名比较值。 */
        String normalizedUsername = UsernameNormalizer.normalize(username);
        /** 可选显示名称。 */
        String displayName = blankToNull(request.owner().displayName());
        /** 可选工号。 */
        String employeeNumber = blankToNull(request.owner().employeeNumber());
        /** 可选行政区域 ID。 */
        Long adminRegionId = request.rootOrganization().adminRegionId() == null
                ? null : parsePositiveLong(request.rootOrganization().adminRegionId(), "行政区域 ID 不合法");
        /** 永久唯一的系统所有者角色编码。 */
        String roleCode = "OWNER_T" + tenantId + "_O" + organizationId;

        mapper.insertTenant(tenantId, tenantCode, tenantName, request.templateVersion(), operatorId);
        mapper.insertTenantCodeRegistry(tenantId, tenantCode, operatorId);
        mapper.copyOrganizationTypes(tenantId, request.templateVersion(), operatorId);
        mapper.copyOrganizationTypeRelations(tenantId, request.templateVersion(), operatorId);
        /** 根机构对应的租户机构类型 ID。 */
        Long organizationTypeId = mapper.selectOrganizationTypeId(tenantId, request.rootOrganization().typeCode());
        if (organizationTypeId == null) {
            throw new BusinessException(TEMPLATE_NOT_FOUND_CODE, "根机构类型不属于所选模板", 400);
        }
        mapper.insertRootOrganization(organizationId, tenantId, organizationTypeId, organizationCode,
                organizationName, normalizedOrganizationName, adminRegionId, operatorId);
        mapper.insertRootOrganizationClosure(tenantId, organizationId, operatorId);
        mapper.insertOrganizationCodeRegistry(tenantId, organizationId, normalizedOrganizationCode, operatorId);
        mapper.bindRootOrganization(tenantId, organizationId, operatorId);
        mapper.insertSecurityPolicy(tenantId, request.securityPolicy(), operatorId);
        mapper.insertOwnerAccount(accountId, tenantId, organizationId, username, normalizedUsername,
                displayName, employeeNumber, operatorId);
        mapper.insertUsernameRegistry(accountId, username, normalizedUsername, operatorId);
        if (employeeNumber != null) {
            mapper.insertEmployeeNumberRegistry(tenantId, organizationId, accountId, employeeNumber,
                    normalize(employeeNumber), operatorId);
        }
        mapper.insertOwnerRole(roleId, tenantId, organizationId, roleCode, operatorId);
        mapper.insertOwnerDataPolicy(tenantId, organizationId, roleId, accountId);
        mapper.insertAccountRole(tenantId, organizationId, accountId, roleId);
        mapper.insertOrganizationOwner(tenantId, organizationId, accountId, roleId);
        mapper.initializeTenantFeatures(tenantId, operatorId);
        mapper.initializeTenantMenus(tenantId, operatorId);
        mapper.insertCreateAudit(tenantId, operatorId, tenantCode);
        mapper.insertProvisionOutbox(outboxId, UUID.randomUUID().toString(), tenantId, accountId,
                organizationId, operatorId);
        mapper.completeIdempotency(operatorId, keyHash, TENANT_REFERENCE_PREFIX + tenantId);
        return new TenantProvisionContext(tenantId, accountId, organizationId, outboxId);
    }

    /** 标记 Auth 凭证初始化成功。 */
    @Transactional
    public void markSucceeded(TenantProvisionContext context, long operatorId) {
        mapper.markProvisionSucceeded(context.tenantId(), context.outboxId(), operatorId);
    }

    /** 标记 Auth 凭证初始化失败并保留重试信息。 */
    @Transactional
    public void markFailed(TenantProvisionContext context, long operatorId, String errorCode) {
        mapper.markProvisionFailed(context.tenantId(), context.outboxId(), operatorId, errorCode);
    }

    /** 解析已完成幂等命令的初始化上下文。 */
    private TenantProvisionContext resolveExisting(long operatorId, String keyHash, String requestHash) {
        /** 加锁读取的已有幂等记录。 */
        TenantProvisionMapper.IdempotencyRow record = mapper.selectIdempotencyForUpdate(operatorId, keyHash);
        if (record == null || !requestHash.equals(record.requestHash())) {
            throw new BusinessException(IDEMPOTENCY_CONFLICT_CODE, "幂等键已用于不同的创建请求", 409);
        }
        if (!"COMPLETED".equals(record.status()) || record.responseReference() == null
                || !record.responseReference().startsWith(TENANT_REFERENCE_PREFIX)) {
            throw new BusinessException(IDEMPOTENCY_CONFLICT_CODE, "相同创建请求正在处理中", 409);
        }
        /** 结果引用中的租户 ID。 */
        long tenantId = Long.parseLong(record.responseReference().substring(TENANT_REFERENCE_PREFIX.length()));
        /** 已创建的所有者账号 ID。 */
        Long accountId = mapper.selectOwnerAccountId(tenantId);
        /** 已创建的根机构 ID。 */
        Long organizationId = mapper.selectRootOrganizationId(tenantId);
        /** 已创建的凭证初始化事件主键。 */
        Long outboxId = mapper.selectProvisionOutboxId(tenantId);
        if (accountId == null || organizationId == null || outboxId == null) {
            throw new BusinessException(IDEMPOTENCY_CONFLICT_CODE, "租户初始化结果不完整", 409);
        }
        return new TenantProvisionContext(tenantId, accountId, organizationId, outboxId);
    }

    /** 校验模板版本、根类型和可选行政区域引用。 */
    private void validateTemplateAndRegion(CreateTenantRequest request) {
        if (mapper.countPublishedTemplateTypes(request.templateVersion()) == 0
                || mapper.countPublishedRootType(request.templateVersion(), request.rootOrganization().typeCode()) == 0) {
            throw new BusinessException(TEMPLATE_NOT_FOUND_CODE, "所选机构类型模板或根机构类型不存在", 400);
        }
        if (request.rootOrganization().adminRegionId() != null
                && mapper.countActiveAdminRegion(parsePositiveLong(
                        request.rootOrganization().adminRegionId(), "行政区域 ID 不合法")) == 0) {
            throw new BusinessException(INVALID_REQUEST_CODE, "所选行政区域不存在或已停用", 400);
        }
    }

    /** 校验会话时长和密码到期字段间约束。 */
    private void validatePolicy(TenantSecurityPolicyRequest policy) {
        if (policy.idleSeconds() > policy.absoluteSeconds()
                || policy.rememberIdleSeconds() > policy.rememberAbsoluteSeconds()
                || (policy.passwordExpiryEnabled() && policy.passwordExpiryDays() == null)
                || (!policy.passwordExpiryEnabled() && policy.passwordExpiryDays() != null)) {
            throw new BusinessException(INVALID_REQUEST_CODE, "租户安全策略字段组合不合法", 400);
        }
    }

    /** @return Unicode NFKC、去首尾空白并转小写后的稳定比较值 */
    private String normalize(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    /** @return 空白字符串转换为空后的可选值 */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** 将外部 bigint 字符串安全解析为正数。 */
    private long parsePositiveLong(String value, String message) {
        try {
            /** 已解析的 bigint 值。 */
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new BusinessException(INVALID_REQUEST_CODE, message, 400);
        }
    }

    /** @return Jackson 序列化字节 */
    private byte[] serialize(CreateTenantRequest request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize tenant creation request", exception);
        }
    }

    /** @return SHA-256 小写十六进制摘要 */
    private String hash(byte[] content) {
        return HmacRequestSigner.sha256Hex(content);
    }
}
