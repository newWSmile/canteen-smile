package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.BootstrapProperties;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.BootstrapPlatformIdentityInternalRequest;
import com.canteen.smile.internal.client.dto.PlatformIdentityInternalResponse;
import com.canteen.smile.modules.auth.dto.PlatformBootstrapRequest;
import com.canteen.smile.modules.auth.vo.PlatformBootstrapVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/** 首位平台管理员跨 IAM/Auth 一次性引导编排服务。 */
@Service
@RequiredArgsConstructor
public class PlatformBootstrapService {

    /** 首位平台管理员引导已关闭错误码。 */
    private static final String BOOTSTRAP_CLOSED_CODE = "AUTH_1010";

    /** 引导密钥校验组件。 */
    private final BootstrapSecretVerifier bootstrapSecretVerifier;

    /** 默认密码策略服务。 */
    private final PasswordPolicyService passwordPolicyService;

    /** Argon2id 密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    /** IAM 平台身份 Client。 */
    private final IamPlatformIdentityClient iamPlatformIdentityClient;

    /** Auth 本地凭证事务服务。 */
    private final CredentialProvisionService credentialProvisionService;

    /** 引导配置。 */
    private final BootstrapProperties bootstrapProperties;

    /**
     * 完成首位平台身份、Argon2id 凭证和恢复码引导。
     *
     * @param bootstrapSecret 请求携带的一次性引导密钥
     * @param request 首位平台管理员资料和初始密码
     * @param password 已从一次性信封中解密且仅在当前调用链使用的初始密码
     * @return 只展示一次恢复码的引导结果
     */
    public PlatformBootstrapVO bootstrap(
            String bootstrapSecret,
            PlatformBootstrapRequest request,
            String password
    ) {
        bootstrapSecretVerifier.verify(bootstrapSecret);
        passwordPolicyService.validate(password, request.getUsername());
        validateRecoveryCodeCount();

        /** IAM 创建或返回的首位平台身份。 */
        PlatformIdentityInternalResponse identity = iamPlatformIdentityClient.bootstrap(
                new BootstrapPlatformIdentityInternalRequest(request.getUsername(), request.getDisplayName())
        );
        if ("ACTIVE".equals(identity.status())) {
            throw new BusinessException(BOOTSTRAP_CLOSED_CODE, "首位平台管理员引导已经关闭", 409);
        }
        /** 平台身份 ID 数值。 */
        long identityId = Long.parseLong(identity.id());
        /** 在事务外计算的 Argon2id 密码摘要。 */
        String encodedPassword = passwordEncoder.encode(password);
        /** 新批次恢复码明文。 */
        List<String> recoveryCodes = credentialProvisionService.provision(
                identityId,
                password,
                encodedPassword,
                bootstrapProperties.getRecoveryCodeCount()
        );
        /** IAM 激活后的平台身份。 */
        PlatformIdentityInternalResponse activatedIdentity = iamPlatformIdentityClient.activate(identityId);
        return new PlatformBootstrapVO(
                activatedIdentity.id(),
                activatedIdentity.username(),
                recoveryCodes,
                "立即离线保存全部恢复码，然后使用用户名和密码登录平台管理端"
        );
    }

    /** 校验恢复码数量处于安全且可运营的范围。 */
    private void validateRecoveryCodeCount() {
        if (bootstrapProperties.getRecoveryCodeCount() < 5 || bootstrapProperties.getRecoveryCodeCount() > 20) {
            throw new IllegalStateException("Platform bootstrap recovery code count must be between 5 and 20");
        }
    }
}
