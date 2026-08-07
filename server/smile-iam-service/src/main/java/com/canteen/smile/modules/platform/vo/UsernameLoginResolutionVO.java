package com.canteen.smile.modules.platform.vo;

/**
 * Auth 使用的用户名登录解析结果，不向外部客户端直接返回。
 *
 * @param resolved 是否解析到与应用入口匹配且可用的主体
 * @param subjectType 认证主体类型
 * @param subjectId 认证主体 bigint ID 十进制字符串
 * @param username 当前原始用户名
 * @param displayName 显示名称
 * @param status 身份状态
 * @param authzVersion 授权版本
 */
public record UsernameLoginResolutionVO(
        boolean resolved,
        String subjectType,
        String subjectId,
        String username,
        String displayName,
        String status,
        Long authzVersion
) {

    /** @return 不泄露账号存在性细节的未解析结果 */
    public static UsernameLoginResolutionVO unresolved() {
        return new UsernameLoginResolutionVO(false, null, null, null, null, null, null);
    }
}
