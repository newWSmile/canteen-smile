package com.canteen.smile.modules.auth.vo;

import java.util.List;

/**
 * 手机号验证码找回密码的分步结果。
 *
 * @param nextStep 下一步骤
 * @param passwordResetTicket 可直接设置新密码时的一次性票据
 * @param accountSelectorTicket 多账号场景的一次性选择票据
 * @param accountCandidates 可安全展示的账号候选
 */
public record SmsPasswordResetResultVO(
        String nextStep,
        String passwordResetTicket,
        String accountSelectorTicket,
        List<MobileLoginCandidateVO> accountCandidates
) {

    /** @param ticket 密码重置票据 @return 可以进入设置新密码页面的结果 */
    public static SmsPasswordResetResultVO resetPassword(String ticket) {
        return new SmsPasswordResetResultVO("RESET_PASSWORD", ticket, null, List.of());
    }

    /** @param ticket 账号选择票据 @param candidates 当前可找回账号候选 @return 账号选择结果 */
    public static SmsPasswordResetResultVO accountSelectionRequired(
            String ticket,
            List<MobileLoginCandidateVO> candidates
    ) {
        return new SmsPasswordResetResultVO(
                "ACCOUNT_SELECTION_REQUIRED", null, ticket, candidates
        );
    }
}
