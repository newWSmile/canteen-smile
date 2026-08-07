package com.canteen.smile.modules.tenant.converter;

import com.canteen.smile.modules.account.model.AccountStatus;
import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.model.TenantProvisionStatus;
import com.canteen.smile.modules.tenant.model.TenantStatus;
import com.canteen.smile.modules.tenant.vo.TenantSummaryVO;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户显式对象转换测试。 */
class TenantConverterTest {

    /** 验证 bigint ID 被安全转换为前端十进制字符串。 */
    @Test
    void shouldConvertTenantEntityToSummary() {
        /** 待转换的租户数据库实体。 */
        TenantEntity entity = tenantEntity();
        /** 被测试的手动转换器。 */
        TenantConverter converter = new TenantConverter();

        /** 转换后的租户分页摘要。 */
        TenantSummaryVO result = converter.toSummary(entity, "tenantOwner", AccountStatus.ACTIVE);

        assertThat(result.id()).isEqualTo("9007199254740993");
        assertThat(result.rootOrganizationId()).isEqualTo("9007199254740995");
        assertThat(result.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(result.provisionStatus()).isEqualTo(TenantProvisionStatus.ACTIVE);
        assertThat(result.ownerUsername()).isEqualTo("tenantOwner");
        assertThat(result.ownerAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.version()).isEqualTo(7L);
    }

    /** @return 包含超过 JavaScript 安全整数范围 ID 的租户实体 */
    private TenantEntity tenantEntity() {
        /** 测试使用的租户实体。 */
        TenantEntity entity = new TenantEntity();
        entity.setId(9_007_199_254_740_993L);
        entity.setTenantCode("TENANT-001");
        entity.setName("测试租户");
        entity.setStatus("ACTIVE");
        entity.setRootOrganizationId(9_007_199_254_740_995L);
        entity.setSecurityVersion(2L);
        entity.setTemplateVersion(3L);
        entity.setProvisionStatus("ACTIVE");
        entity.setCreatedTime(OffsetDateTime.parse("2026-08-06T10:00:00+08:00"));
        entity.setVersion(7L);
        return entity;
    }
}
