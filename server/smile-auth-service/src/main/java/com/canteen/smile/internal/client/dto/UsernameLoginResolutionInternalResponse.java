package com.canteen.smile.internal.client.dto;

/**
 * IAM 用户名登录解析内部响应。
 *
 * @param resolved 是否解析到可登录主体
 * @param subjectType 认证主体类型
 * @param subjectId 认证主体十进制字符串 ID
 * @param username 当前用户名
 * @param displayName 展示名称
 * @param status IAM 主体状态
 * @param authzVersion 授权版本
 */
public record UsernameLoginResolutionInternalResponse(
        boolean resolved,
        String subjectType,
        String subjectId,
        String username,
        String displayName,
        String status,
        Long authzVersion
) {
}
