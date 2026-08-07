package com.canteen.smile.modules.auth.vo;

/**
 * 账号激活完成结果。
 *
 * @param username 已激活用户名
 * @param nextStep 下一步动作
 */
public record ActivationCompleteVO(String username, String nextStep) {
}
