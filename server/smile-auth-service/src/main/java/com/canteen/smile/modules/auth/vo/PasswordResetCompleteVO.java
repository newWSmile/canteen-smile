package com.canteen.smile.modules.auth.vo;

/**
 * 密码恢复完成结果。
 *
 * @param username 用户名
 * @param nextStep 后续固定进入登录页
 */
public record PasswordResetCompleteVO(String username, String nextStep) {
}
