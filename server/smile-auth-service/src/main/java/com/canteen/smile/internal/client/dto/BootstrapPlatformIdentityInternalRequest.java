package com.canteen.smile.internal.client.dto;

/**
 * Auth → IAM 首位平台身份创建契约。
 *
 * @param username 原始用户名
 * @param displayName 可选显示名称
 */
public record BootstrapPlatformIdentityInternalRequest(String username, String displayName) {
}
