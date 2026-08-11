package com.canteen.smile.modules.auth.vo;

import java.time.OffsetDateTime;

/**
 * 手机号验证成功后可安全展示给用户选择的租户账号候选。
 *
 * @param accountId 账号 ID
 * @param tenantName 租户名称
 * @param organizationName 所属机构名称
 * @param username 用户名
 * @param displayName 显示名称
 * @param latestLoginTime 最近一次登录时间，无历史时为空
 */
public record MobileLoginCandidateVO(
        String accountId,
        String tenantName,
        String organizationName,
        String username,
        String displayName,
        OffsetDateTime latestLoginTime
) {
}
