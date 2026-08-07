package com.canteen.smile.internal.client.dto;

/**
 * IAM 返回的平台身份内部快照。
 *
 * @param id 平台身份 ID 十进制字符串
 * @param username 原始用户名
 * @param displayName 显示名称
 * @param status 身份状态
 * @param authzVersion 授权版本
 * @param version 乐观锁版本
 */
public record PlatformIdentityInternalResponse(
        String id,
        String username,
        String displayName,
        String status,
        long authzVersion,
        long version
) {
}
