package com.canteen.smile.modules.auth.service;

import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.UsernameLoginResolutionInternalResponse;
import com.canteen.smile.modules.auth.dto.DeviceRequest;
import com.canteen.smile.modules.auth.dto.PasswordLoginRequest;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.LoginResultVO;
import com.canteen.smile.modules.auth.vo.SessionVO;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 平台用户名密码直接登录行为测试。 */
class PlatformPasswordLoginServiceTest {

    /** 验证密码成功后直接建立会话，不要求或消费恢复码。 */
    @Test
    void shouldCreateSessionImmediatelyAfterPasswordVerified() {
        /** IAM 平台身份 Client。 */
        IamPlatformIdentityClient iamClient = mock(IamPlatformIdentityClient.class);
        /** 凭证数据访问接口。 */
        CredentialMapper credentialMapper = mock(CredentialMapper.class);
        /** 密码验证服务。 */
        PasswordVerificationService passwordVerificationService = mock(PasswordVerificationService.class);
        /** 登录保护服务。 */
        LoginProtectionService loginProtectionService = mock(LoginProtectionService.class);
        /** 平台设备会话服务。 */
        PlatformSessionService platformSessionService = mock(PlatformSessionService.class);
        /** 被测试的平台密码登录服务。 */
        PlatformPasswordLoginService service = new PlatformPasswordLoginService(
                iamClient,
                credentialMapper,
                passwordVerificationService,
                loginProtectionService,
                platformSessionService
        );
        /** 当前登录请求。 */
        PasswordLoginRequest request = passwordLoginRequest();
        /** IAM 返回的有效平台身份。 */
        UsernameLoginResolutionInternalResponse resolution = new UsernameLoginResolutionInternalResponse(
                true,
                AuthConstants.PLATFORM_IDENTITY_SUBJECT,
                "1001",
                "platform-admin",
                "平台管理员",
                AuthConstants.ACTIVE_STATUS,
                3L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        /** Auth 本地密码凭证。 */
        CredentialEntity credential = new CredentialEntity();
        /** 登录成功后签发的设备会话。 */
        SessionVO session = new SessionVO(
                "satoken",
                "token-value",
                "session-id",
                AuthConstants.PLATFORM_ADMIN_APP,
                AuthConstants.PLATFORM_IDENTITY_SUBJECT,
                "1001",
                null,
                null,
                OffsetDateTime.now().plusHours(2),
                OffsetDateTime.now().plusDays(7)
        );
        when(iamClient.resolveUsername(any())).thenReturn(resolution);
        when(credentialMapper.selectBySubject(AuthConstants.PLATFORM_IDENTITY_SUBJECT, 1001L))
                .thenReturn(credential);
        when(passwordVerificationService.matches("correct-password", credential)).thenReturn(true);
        when(platformSessionService.createPasswordSession(any(), eq("127.0.0.1"))).thenReturn(session);

        /** 用户名密码登录结果。 */
        LoginResultVO result = service.login(request, "correct-password", "127.0.0.1");

        assertThat(result.nextStep()).isEqualTo("AUTHENTICATED");
        assertThat(result.session()).isSameAs(session);
        assertThat(result.secondFactorTicket()).isNull();
        verify(loginProtectionService).resetAfterSuccess(AuthConstants.PLATFORM_ADMIN_APP, "platform-admin");
        verify(platformSessionService).createPasswordSession(any(), eq("127.0.0.1"));
    }

    /** @return 具备真实设备字段的平台密码登录请求 */
    private PasswordLoginRequest passwordLoginRequest() {
        /** 当前浏览器设备描述。 */
        DeviceRequest device = new DeviceRequest();
        device.setDeviceId("device-id");
        device.setDeviceType("WEB");
        device.setDeviceName("平台管理端浏览器");
        /** 当前平台登录请求。 */
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setAppCode(AuthConstants.PLATFORM_ADMIN_APP);
        request.setUsername("platform-admin");
        request.setRememberMe(false);
        request.setDevice(device);
        return request;
    }
}
