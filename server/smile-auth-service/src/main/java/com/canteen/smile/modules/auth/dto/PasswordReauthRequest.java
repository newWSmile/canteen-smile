package com.canteen.smile.modules.auth.dto;

import com.canteen.smile.modules.auth.model.ReauthAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 当前平台身份使用密码完成敏感操作再认证。
 *
 * @param passwordEnvelope 当前密码加密信封
 * @param allowedAction 本次票据唯一允许的敏感操作
 */
public record PasswordReauthRequest(
        @Valid @NotNull PasswordEnvelopeRequest passwordEnvelope,
        @NotNull ReauthAction allowedAction
) {
}
