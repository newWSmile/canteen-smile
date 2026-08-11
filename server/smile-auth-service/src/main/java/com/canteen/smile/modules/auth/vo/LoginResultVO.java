package com.canteen.smile.modules.auth.vo;

/**
 * 分步骤登录结果。
 *
 * @param nextStep 下一认证步骤
 * @param session 已认证时的设备会话
 * @param secondFactorTicket 平台二次验证一次性票据
 */
public record LoginResultVO(
        String nextStep,
        SessionVO session,
        String secondFactorTicket,
        String accountSelectorTicket,
        java.util.List<MobileLoginCandidateVO> accountCandidates
) {

    /** @param session 已建立的设备会话 @return 已完成认证的登录结果 */
    public static LoginResultVO authenticated(SessionVO session) {
        return new LoginResultVO("AUTHENTICATED", session, null, null, java.util.List.of());
    }

    /** @return 需要平台二次验证的登录结果 */
    public static LoginResultVO secondFactorRequired(String ticket) {
        return new LoginResultVO("SECOND_FACTOR_REQUIRED", null, ticket, null, java.util.List.of());
    }

    /** @param ticket 短期一次性选择票据 @param candidates 可安全展示的账号候选 */
    public static LoginResultVO accountSelectionRequired(
            String ticket,
            java.util.List<MobileLoginCandidateVO> candidates
    ) {
        return new LoginResultVO("ACCOUNT_SELECTION_REQUIRED", null, null, ticket, candidates);
    }
}
