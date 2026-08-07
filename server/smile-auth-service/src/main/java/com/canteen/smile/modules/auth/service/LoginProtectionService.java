package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.dto.DeviceRequest;
import com.canteen.smile.modules.auth.entity.LoginFailureEntity;
import com.canteen.smile.modules.auth.mapper.LoginFailureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Locale;

/** 密码失败计数、图形验证码门槛和十五分钟锁定服务。 */
@Service
@RequiredArgsConstructor
public class LoginProtectionService {

    /** 密码登录临时锁定错误码。 */
    private static final String PASSWORD_LOCKED_CODE = "AUTH_1002";

    /** 需要图形验证码错误码。 */
    private static final String CAPTCHA_REQUIRED_CODE = "AUTH_1003";

    /** 登录失败数据访问接口。 */
    private final LoginFailureMapper loginFailureMapper;

    /**
     * 在密码哈希计算前检查现有锁定和验证码门槛。
     *
     * @param appCode 应用编码
     * @param username 用户输入用户名
     * @param captchaTicket 图形验证码票据
     */
    @Transactional(readOnly = true)
    public void requirePasswordAttemptAllowed(String appCode, String username, String captchaTicket) {
        /** 当前登录主体失败状态。 */
        LoginFailureEntity failure = loginFailureMapper.selectBySubjectKeyHash(subjectHash(appCode, username));
        if (failure == null) {
            return;
        }
        if (failure.getLockedUntil() != null && failure.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(PASSWORD_LOCKED_CODE, "密码登录已临时锁定，请稍后重试", 423);
        }
        if (Boolean.TRUE.equals(failure.getCaptchaRequired())) {
            /* TODO(待确认): 图形验证码供应商与无障碍替代策略确认后校验 captchaTicket。 */
            throw new BusinessException(CAPTCHA_REQUIRED_CODE, "需要先完成图形验证码");
        }
    }

    /**
     * 原子记录一次密码失败。
     *
     * @param appCode 应用编码
     * @param username 用户名
     * @param ipAddress 服务端解析的来源 IP
     * @param device 登录设备
     */
    @Transactional
    public void recordPasswordFailure(
            String appCode,
            String username,
            String ipAddress,
            DeviceRequest device
    ) {
        loginFailureMapper.recordPasswordFailure(
                subjectHash(appCode, username),
                sha256(ipAddress == null ? "unknown" : ipAddress),
                sha256(device.getDeviceId())
        );
    }

    /** 成功完成密码验证后清除连续失败状态。 */
    @Transactional
    public void resetAfterSuccess(String appCode, String username) {
        loginFailureMapper.resetAfterSuccess(subjectHash(appCode, username));
    }

    /** @return 不含明文用户名的登录主体组合摘要 */
    private String subjectHash(String appCode, String username) {
        /** 仅用于风控聚合、不能代替 IAM 身份解析的用户名规范值。 */
        String normalizedUsername = Normalizer.normalize(username, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT);
        return sha256(appCode + "\n" + normalizedUsername);
    }

    /** @param value 原始值 @return SHA-256 小写十六进制摘要 */
    private String sha256(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }
}
