package com.canteen.smile.internal.client.dto;

import java.util.List;

/**
 * Auth 请求 IAM 批量解析手机号登录候选的内部契约。
 *
 * @param appCode 应用入口编码
 * @param accountIds Auth 已验证手机号摘要命中的账号 ID
 */
public record MobileAccountLoginResolutionInternalRequest(String appCode, List<Long> accountIds) {
}
