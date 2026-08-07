package com.canteen.smile.modules.auth.service;

import com.canteen.smile.internal.dto.TenantAccountProvisionResponse;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 租户账号待激活凭证幂等初始化测试。 */
class TenantAccountCredentialServiceTest {

    /** 验证服务使用固定主体类型创建并返回待激活凭证。 */
    @Test
    void shouldProvisionPendingTenantCredential() {
        /** 凭证 Mapper 替身。 */
        CredentialMapper mapper = mock(CredentialMapper.class);
        /** 已创建的待激活凭证。 */
        CredentialEntity credential = new CredentialEntity();
        credential.setSubjectType("TENANT_ACCOUNT");
        credential.setSubjectId(99L);
        credential.setStatus("PENDING");
        when(mapper.selectBySubject("TENANT_ACCOUNT", 99L)).thenReturn(credential);
        /** 被测服务。 */
        TenantAccountCredentialService service = new TenantAccountCredentialService(mapper);

        TenantAccountProvisionResponse result = service.provision(99L);

        verify(mapper).insertPendingTenantAccountCredential(99L);
        assertThat(result.accountId()).isEqualTo("99");
        assertThat(result.credentialStatus()).isEqualTo("PENDING");
    }
}
