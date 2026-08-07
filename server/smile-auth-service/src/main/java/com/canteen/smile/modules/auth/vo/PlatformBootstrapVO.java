package com.canteen.smile.modules.auth.vo;

import java.util.List;

/**
 * 首位平台管理员一次性引导结果。
 *
 * @param platformIdentityId 平台身份 bigint ID 十进制字符串
 * @param username 平台用户名
 * @param recoveryCodes 仅本次响应展示的一次性恢复码
 * @param nextStep 下一步操作提示
 */
public record PlatformBootstrapVO(
        String platformIdentityId,
        String username,
        List<String> recoveryCodes,
        String nextStep
) {

    /** 创建不可变恢复码副本。 */
    public PlatformBootstrapVO {
        recoveryCodes = List.copyOf(recoveryCodes);
    }
}
