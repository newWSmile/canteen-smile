package com.canteen.smile.modules.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 使用一次性密码恢复票据设置新密码。
 *
 * @param passwordEnvelope 新密码加密信封
 */
public record CompletePasswordResetRequest(@Valid @NotNull PasswordEnvelopeRequest passwordEnvelope) {
}
