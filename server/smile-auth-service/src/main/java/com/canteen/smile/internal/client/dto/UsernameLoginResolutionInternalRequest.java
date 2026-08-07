package com.canteen.smile.internal.client.dto;

/**
 * IAM 用户名登录解析内部请求。
 *
 * @param appCode 发起登录的前端应用编码
 * @param username 用户输入的用户名
 */
public record UsernameLoginResolutionInternalRequest(String appCode, String username) {
}
