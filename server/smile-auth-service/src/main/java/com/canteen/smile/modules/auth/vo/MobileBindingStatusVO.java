package com.canteen.smile.modules.auth.vo;

import java.time.OffsetDateTime;

/**
 * 当前账号手机号安全状态，永不返回完整手机号或密文。
 *
 * @param bound 是否已经存在有效绑定
 * @param maskedMobile 脱敏手机号，未绑定时为空
 * @param verifiedTime 验证完成时间，未绑定时为空
 */
public record MobileBindingStatusVO(
        boolean bound,
        String maskedMobile,
        OffsetDateTime verifiedTime
) {

    /** @return 尚未绑定手机号的稳定响应 */
    public static MobileBindingStatusVO unbound() {
        return new MobileBindingStatusVO(false, null, null);
    }
}
