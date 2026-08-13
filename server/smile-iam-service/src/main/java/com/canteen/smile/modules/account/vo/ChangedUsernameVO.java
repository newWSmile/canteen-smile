package com.canteen.smile.modules.account.vo;

/**
 * 用户名修改结果。
 *
 * @param accountId 账号 ID
 * @param username 新用户名
 */
public record ChangedUsernameVO(String accountId, String username) {
}
