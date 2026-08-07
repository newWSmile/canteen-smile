package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.modules.account.model.AccountStatus;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import com.canteen.smile.modules.tenant.converter.TenantConverter;
import com.canteen.smile.modules.tenant.dto.CreateTenantRequest;
import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.mapper.TenantMapper;
import com.canteen.smile.modules.tenant.vo.TenantCreationVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** 平台创建租户并协调 IAM 本地事务与 Auth 幂等初始化。 */
@Service
@RequiredArgsConstructor
public class TenantCreationService {

    /** 当前服务日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(TenantCreationService.class);

    /** 永久标识或唯一字段冲突错误码。 */
    private static final String UNIQUE_CONFLICT_CODE = "IAM_2204";

    /** Auth 初始化失败错误码。 */
    private static final String AUTH_PROVISION_FAILED_CODE = "IAM_2205";

    /** IAM 本地事务服务。 */
    private final TenantProvisionTransactionService transactionService;

    /** Auth 租户账号凭证 Client。 */
    private final AuthTenantAccountClient authClient;

    /** 当前平台操作者解析服务。 */
    private final PlatformActorService platformActorService;

    /** 租户查询 Mapper。 */
    private final TenantMapper tenantMapper;

    /** 租户显式转换器。 */
    private final TenantConverter tenantConverter;

    /**
     * 创建完整 IAM 数据后同步 Auth 凭证容器；失败时保留可重试状态。
     *
     * @param request 创建租户命令
     * @param idempotencyKey 外部幂等键
     * @return 最新租户与所有者状态
     */
    public TenantCreationVO create(CreateTenantRequest request, String idempotencyKey) {
        /** 当前平台身份 ID。 */
        long operatorId = platformActorService.currentPlatformIdentityId();
        /** IAM 本地初始化上下文。 */
        TenantProvisionContext context;
        try {
            context = transactionService.initialize(request, idempotencyKey, operatorId);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(UNIQUE_CONFLICT_CODE,
                    "租户编码、机构编码、用户名、工号或其它永久唯一字段已被占用", 409);
        }
        try {
            authClient.provision(context.accountId(), context.tenantId(), context.organizationId());
            transactionService.markSucceeded(context, operatorId);
        } catch (BusinessException exception) {
            transactionService.markFailed(context, operatorId, AUTH_PROVISION_FAILED_CODE);
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Auth tenant provision failed before completion: {}", exception.getClass().getSimpleName());
            transactionService.markFailed(context, operatorId, AUTH_PROVISION_FAILED_CODE);
            throw new BusinessException(AUTH_PROVISION_FAILED_CODE,
                    "认证服务暂时不可用，租户已保留并等待重试", 502);
        }
        /** 激活后的租户实体。 */
        TenantEntity tenant = tenantMapper.selectById(context.tenantId());
        if (tenant == null) {
            throw new IllegalStateException("Tenant disappeared after successful provisioning");
        }
        return new TenantCreationVO(tenantConverter.toSummary(
                tenant,
                request.owner().username(),
                AccountStatus.PENDING_ACTIVATION
        ),
                Long.toString(context.accountId()), "PENDING_ACTIVATION");
    }
}
