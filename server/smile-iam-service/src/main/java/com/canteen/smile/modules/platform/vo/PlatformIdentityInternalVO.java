package com.canteen.smile.modules.platform.vo;

import com.canteen.smile.modules.platform.model.PlatformIdentityStatus;

/**
 * Auth 使用的平台身份内部契约。
 *
 * @param id 平台身份 bigint ID 的十进制字符串
 * @param username 原始用户名
 * @param displayName 显示名称
 * @param status 平台身份状态
 * @param authzVersion 授权版本
 * @param version 乐观锁版本
 */
public record PlatformIdentityInternalVO(
        String id,
        String username,
        String displayName,
        PlatformIdentityStatus status,
        long authzVersion,
        long version
) {
}
