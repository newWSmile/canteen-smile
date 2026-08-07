package com.canteen.smile.modules.platform.service;

import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.platform.dto.UsernameLoginResolutionRequest;
import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.mapper.PlatformIdentityMapper;
import com.canteen.smile.modules.platform.vo.UsernameLoginResolutionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 平台用户名登录主体解析测试。 */
@ExtendWith(MockitoExtension.class)
class PlatformLoginResolutionServiceTest {

    /** 模拟平台身份数据访问接口。 */
    @Mock
    private PlatformIdentityMapper platformIdentityMapper;

    /** 模拟租户账号登录上下文数据访问接口。 */
    @Mock
    private AccountLifecycleMapper accountLifecycleMapper;

    /** 被测试登录解析服务。 */
    private PlatformLoginResolutionService service;

    /** 创建测试服务。 */
    @BeforeEach
    void setUp() {
        service = new PlatformLoginResolutionService(platformIdentityMapper, accountLifecycleMapper);
    }

    /** 验证用户名不区分大小写并只解析有效平台身份。 */
    @Test
    void shouldResolveActivePlatformIdentity() {
        /** IAM 中的有效平台身份。 */
        PlatformIdentityEntity entity = new PlatformIdentityEntity();
        entity.setId(1001L);
        entity.setUsername("PlatformRoot");
        entity.setDisplayName(null);
        entity.setStatus("ACTIVE");
        entity.setAuthzVersion(3L);
        entity.setDeleted(false);
        when(platformIdentityMapper.selectByNormalizedUsername("platformroot")).thenReturn(entity);

        /** 登录主体解析结果。 */
        UsernameLoginResolutionVO result = service.resolve(
                new UsernameLoginResolutionRequest("PLATFORM_ADMIN", "  PLATFORMROOT  ")
        );

        assertThat(result.resolved()).isTrue();
        assertThat(result.subjectId()).isEqualTo("1001");
        assertThat(result.displayName()).isEqualTo("PlatformRoot");
        assertThat(result.authzVersion()).isEqualTo(3L);
    }

    /** 验证租户入口不会错误解析为平台身份。 */
    @Test
    void shouldNotResolvePlatformIdentityForTenantApp() {
        /** 非平台入口解析结果。 */
        UsernameLoginResolutionVO result = service.resolve(
                new UsernameLoginResolutionRequest("TENANT_ADMIN", "platformroot")
        );

        assertThat(result.resolved()).isFalse();
    }
}
