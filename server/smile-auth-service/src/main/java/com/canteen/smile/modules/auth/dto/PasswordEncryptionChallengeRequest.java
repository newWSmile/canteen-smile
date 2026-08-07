package com.canteen.smile.modules.auth.dto;

import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import jakarta.validation.constraints.NotNull;

/**
 * 创建一次性密码加密挑战的请求。
 *
 * @param purpose 挑战绑定的唯一业务用途
 */
public record PasswordEncryptionChallengeRequest(
        @NotNull PasswordEnvelopePurpose purpose
) {
}
