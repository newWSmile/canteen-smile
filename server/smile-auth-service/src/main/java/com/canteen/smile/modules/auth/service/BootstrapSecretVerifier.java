package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.BootstrapProperties;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** 一次性平台引导密钥校验组件。 */
@Component
@RequiredArgsConstructor
public class BootstrapSecretVerifier {

    /** 引导密钥无效错误码。 */
    private static final String INVALID_BOOTSTRAP_SECRET_CODE = "AUTH_1012";

    /** 引导密钥安全配置。 */
    private final BootstrapProperties properties;

    /**
     * 对环境变量密钥与请求密钥的摘要执行常量时间比较。
     *
     * @param providedSecret 请求携带的一次性引导密钥
     */
    public void verify(String providedSecret) {
        if (properties.getSecret() == null || properties.getSecret().isBlank() || providedSecret == null) {
            throw invalidSecret();
        }
        /** 环境变量引导密钥摘要。 */
        String expectedHash = HmacRequestSigner.sha256Hex(
                properties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        /** 请求引导密钥摘要。 */
        String providedHash = HmacRequestSigner.sha256Hex(providedSecret.getBytes(StandardCharsets.UTF_8));
        if (!HmacRequestSigner.constantTimeEquals(expectedHash, providedHash)) {
            throw invalidSecret();
        }
    }

    /** @return 不泄露配置状态的统一引导密钥异常 */
    private BusinessException invalidSecret() {
        return new BusinessException(INVALID_BOOTSTRAP_SECRET_CODE, "平台引导凭证无效", 401);
    }
}
