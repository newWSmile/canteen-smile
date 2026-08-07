package com.canteen.smile.modules.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 使用一次性票据设置初始密码的请求。
 *
 * @param passwordEnvelope RSA-OAEP 与 AES-GCM 混合加密的密码信封
 */
public record CompleteActivationRequest(@Valid @NotNull PasswordEnvelopeRequest passwordEnvelope) {
}
