package com.canteen.smile.modules.sms.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.redis.RedisKeyBuilder;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import java.util.List;

/** 原子执行短信重发、手机号、IP 和设备小时及每日频率限制。 */
@Service
@RequiredArgsConstructor
public class SmsChallengeRateLimitService {

    /** 发送过于频繁错误码。 */
    private static final String RATE_LIMITED_CODE = "AUTH_1005";

    /** Redis 运行态不可用错误码。 */
    private static final String RUNTIME_UNAVAILABLE_CODE = "AUTH_1013";

    /** Redis Cluster 多 Key Lua 脚本使用的固定哈希槽标签。 */
    private static final String REDIS_HASH_TAG = "{sms-rate-limit}";

    /** 小时窗口 Key 的冗余有效秒数。 */
    private static final int HOURLY_WINDOW_TTL_SECONDS = 7_200;

    /** 每日窗口 Key 的冗余有效秒数。 */
    private static final int DAILY_WINDOW_TTL_SECONDS = 172_800;

    /**
     * 在同一 Redis Cluster 哈希槽内检查全部限制后统一递增，避免部分维度被计数。
     * 返回 1 表示重发间隔未到，返回 2 及以上表示某个窗口达到上限。
     */
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 1
            end
            for i = 2, #KEYS do
                local argumentIndex = 2 + ((i - 2) * 2)
                local current = tonumber(redis.call('GET', KEYS[i]) or '0')
                local limit = tonumber(ARGV[argumentIndex])
                if current >= limit then
                    return i
                end
            end
            redis.call('SET', KEYS[1], '1', 'EX', tonumber(ARGV[1]))
            for i = 2, #KEYS do
                local argumentIndex = 2 + ((i - 2) * 2)
                local current = redis.call('INCR', KEYS[i])
                if current == 1 then
                    redis.call('EXPIRE', KEYS[i], tonumber(ARGV[argumentIndex + 1]))
                end
            end
            return 0
            """, Long.class);

    /** Redis 字符串客户端。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 统一 Redis Key 构建器。 */
    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * 原子取得一次短信发送额度。
     *
     * @param mobileHash 手机号安全摘要
     * @param clientIp 服务端取得的来源 IP
     * @param deviceId 客户端稳定设备标识
     * @param purpose 短信业务用途
     */
    public void acquire(
            String mobileHash,
            String clientIp,
            String deviceId,
            SmsPurpose purpose,
            SmsRuntimePolicy policy
    ) {
        validateConfiguration(policy);
        if (purpose == null) {
            throw new IllegalArgumentException("purpose must not be null");
        }
        /** Redis Key 使用的小写稳定业务用途。 */
        String purposeCode = purpose.name().toLowerCase(java.util.Locale.ROOT);
        /** 当前固定小时窗口编号。 */
        String hourBucket = Long.toString(Instant.now().getEpochSecond() / 3_600);
        /** 当前上海时区自然日窗口编号。 */
        String dayBucket = Long.toString(LocalDate.now(ZoneId.of("Asia/Shanghai")).toEpochDay());
        /** 来源 IP 摘要。 */
        String ipHash = digest(clientIp, "clientIp");
        /** 设备标识摘要。 */
        String deviceHash = digest(deviceId, "deviceId");
        /** 同一哈希槽内的重发与六个窗口 Key。 */
        List<String> keys = List.of(
                key("resend", purposeCode, mobileHash),
                key("mobile-hour", purposeCode, mobileHash, hourBucket),
                key("mobile-day", purposeCode, mobileHash, dayBucket),
                key("ip-hour", ipHash, hourBucket),
                key("ip-day", ipHash, dayBucket),
                key("device-hour", deviceHash, hourBucket),
                key("device-day", deviceHash, dayBucket)
        );
        /** Lua 脚本 TTL 与窗口阈值参数。 */
        Object[] arguments = {
                Integer.toString(policy.resendIntervalSeconds()),
                Integer.toString(policy.mobileHourlyLimit()),
                Integer.toString(HOURLY_WINDOW_TTL_SECONDS),
                Integer.toString(policy.mobileDailyLimit()),
                Integer.toString(DAILY_WINDOW_TTL_SECONDS),
                Integer.toString(policy.ipHourlyLimit()),
                Integer.toString(HOURLY_WINDOW_TTL_SECONDS),
                Integer.toString(policy.ipDailyLimit()),
                Integer.toString(DAILY_WINDOW_TTL_SECONDS),
                Integer.toString(policy.deviceHourlyLimit()),
                Integer.toString(HOURLY_WINDOW_TTL_SECONDS),
                Integer.toString(policy.deviceDailyLimit()),
                Integer.toString(DAILY_WINDOW_TTL_SECONDS)
        };
        try {
            /** 原子获取额度结果。 */
            Long result = stringRedisTemplate.execute(ACQUIRE_SCRIPT, keys, arguments);
            if (result == null) {
                throw runtimeUnavailable();
            }
            if (result == 1L) {
                throw new BusinessException(RATE_LIMITED_CODE, "验证码已发送，请稍后再试", 429);
            }
            if (result > 1L) {
                throw new BusinessException(RATE_LIMITED_CODE, "验证码发送过于频繁，请稍后再试", 429);
            }
        } catch (DataAccessException exception) {
            throw runtimeUnavailable();
        }
    }

    /** @param segments 业务 Key 片段 @return 位于短信限流固定哈希槽的标准 Key */
    private String key(String... segments) {
        /** 固定模块、业务和 Cluster 哈希槽前缀。 */
        String[] fullSegments = new String[segments.length + 3];
        fullSegments[0] = "auth";
        fullSegments[1] = "sms-rate";
        fullSegments[2] = REDIS_HASH_TAG;
        System.arraycopy(segments, 0, fullSegments, 3, segments.length);
        return redisKeyBuilder.build(fullSegments);
    }

    /** @param value 原始限流标识 @param fieldName 字段名称 @return SHA-256 摘要 */
    private String digest(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return HmacRequestSigner.sha256Hex(value.trim().getBytes(StandardCharsets.UTF_8));
    }

    /** 校验全部时间和次数限制均为正数且错误次数符合数据库约束。 */
    private void validateConfiguration(SmsRuntimePolicy policy) {
        if (policy == null
                || policy.challengeTtlSeconds() <= 0
                || policy.resendIntervalSeconds() <= 0
                || policy.maxVerificationAttempts() <= 0
                || policy.maxVerificationAttempts() > 5
                || policy.mobileHourlyLimit() <= 0
                || policy.mobileDailyLimit() <= 0
                || policy.ipHourlyLimit() <= 0
                || policy.ipDailyLimit() <= 0
                || policy.deviceHourlyLimit() <= 0
                || policy.deviceDailyLimit() <= 0) {
            throw new IllegalStateException("SMS challenge and rate limit configuration is invalid");
        }
    }

    /** @return Redis 运行态不可用业务异常 */
    private BusinessException runtimeUnavailable() {
        return new BusinessException(RUNTIME_UNAVAILABLE_CODE, "认证运行状态暂时不可用", 503);
    }
}
