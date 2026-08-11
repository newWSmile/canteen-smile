package com.canteen.smile.modules.platform.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Auth 按已验证手机号绑定结果批量解析可登录租户账号的内部请求。
 *
 * @param appCode 应用入口编码
 * @param accountIds Auth 数据库中由手机号摘要查得的租户账号 ID
 */
public record MobileAccountLoginResolutionRequest(
        @Pattern(regexp = "TENANT_ADMIN|TENANT_PORTAL") String appCode,
        @NotEmpty @Size(max = 100) List<@Positive Long> accountIds
) {
}
