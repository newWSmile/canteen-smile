package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.dto.TenantAccountProvisionResponse;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM 驱动的租户账号凭证容器初始化服务。 */
@Service
@RequiredArgsConstructor
public class TenantAccountCredentialService {

    /** 租户账号认证主体类型。 */
    private static final String TENANT_ACCOUNT = "TENANT_ACCOUNT";

    /** 凭证契约冲突错误码。 */
    private static final String CREDENTIAL_CONFLICT_CODE = "AUTH_1101";

    /** 凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /**
     * 幂等创建待激活凭证容器；本阶段不生成激活链接，也不保存初始密码。
     *
     * @param accountId IAM 租户账号 ID
     * @return 已存在或新建的凭证状态
     */
    @Transactional
    public TenantAccountProvisionResponse provision(long accountId) {
        credentialMapper.insertPendingTenantAccountCredential(accountId);
        /** 幂等创建后的租户账号凭证。 */
        CredentialEntity credential = credentialMapper.selectBySubject(TENANT_ACCOUNT, accountId);
        if (credential == null || !TENANT_ACCOUNT.equals(credential.getSubjectType())) {
            throw new BusinessException(CREDENTIAL_CONFLICT_CODE, "租户账号凭证初始化结果异常", 409);
        }
        return new TenantAccountProvisionResponse(accountId + "", credential.getStatus());
    }
}
