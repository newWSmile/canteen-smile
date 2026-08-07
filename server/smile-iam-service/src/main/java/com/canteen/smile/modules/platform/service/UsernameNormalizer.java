package com.canteen.smile.modules.platform.service;

import java.text.Normalizer;
import java.util.Locale;

/** 全局用户名服务端归一化规则。 */
public final class UsernameNormalizer {

    /** 禁止实例化用户名归一化工具。 */
    private UsernameNormalizer() {
    }

    /**
     * 去首尾空白、执行 Unicode NFKC 规范化并按 Locale.ROOT 转小写。
     *
     * @param username 原始用户名
     * @return 可用于全局唯一比较的用户名
     */
    public static String normalize(String username) {
        /** 去除首尾空白后的用户名。 */
        String trimmed = username.strip();
        return Normalizer.normalize(trimmed, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}
