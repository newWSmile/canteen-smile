package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.redis.RedisKeyBuilder;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/** 平台二次验证高熵一次性票据服务。 */
@Service
@RequiredArgsConstructor
public class PlatformSecondFactorTicketService {

    /** 一次性票据无效错误码。 */
    private static final String INVALID_TICKET_CODE = "AUTH_1007";

    /** 认证运行态暂不可用错误码。 */
    private static final String AUTH_RUNTIME_UNAVAILABLE_CODE = "AUTH_1013";

    /** 票据有效时间。 */
    private static final Duration TICKET_TTL = Duration.ofMinutes(5);

    /** 密码学安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** Redis 字符串客户端。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 统一 Redis Key 构建器。 */
    private final RedisKeyBuilder redisKeyBuilder;

    /** Jackson JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 签发只展示一次的平台二次验证票据。
     *
     * @param context 已通过密码校验的安全上下文
     * @return 票据明文
     */
    public String issue(PlatformSecondFactorContext context) {
        /** 256 位随机票据字节。 */
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        /** 只返回一次的 URL 安全票据。 */
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        try {
            stringRedisTemplate.opsForValue().set(
                    ticketKey(ticket),
                    objectMapper.writeValueAsString(context),
                    TICKET_TTL
            );
            return ticket;
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new BusinessException(AUTH_RUNTIME_UNAVAILABLE_CODE, "认证运行状态暂时不可用", 503);
        }
    }

    /**
     * 原子获取并删除票据，使并发和重放请求最多一个成功。
     *
     * @param ticket 票据明文
     * @return 二次验证上下文
     */
    public PlatformSecondFactorContext consume(String ticket) {
        try {
            /** 原子删除前取得的票据上下文 JSON。 */
            String contextJson = stringRedisTemplate.opsForValue().getAndDelete(ticketKey(ticket));
            if (contextJson == null) {
                throw new BusinessException(INVALID_TICKET_CODE, "一次性票据无效或已使用");
            }
            return objectMapper.readValue(contextJson, PlatformSecondFactorContext.class);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new BusinessException(AUTH_RUNTIME_UNAVAILABLE_CODE, "认证运行状态暂时不可用", 503);
        }
    }

    /** @param ticket 票据明文 @return 只含票据摘要的标准 Redis Key */
    private String ticketKey(String ticket) {
        /** 票据明文的 SHA-256 摘要。 */
        String ticketHash = HmacRequestSigner.sha256Hex(ticket.getBytes(StandardCharsets.UTF_8));
        return redisKeyBuilder.build("auth", "platform-second-factor", ticketHash);
    }
}
