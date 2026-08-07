package com.canteen.smile.infrastructure.redis;

import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Redis Key 构建器，格式为 `项目:模块:业务:标识`。
 * 业务 Key 段必须来自已确认设计，调用方写入非永久缓存时仍必须显式提供 TTL。
 */
@Component
public class RedisKeyBuilder {

    /** Redis Key 项目前缀。 */
    private final String prefix;

    /**
     * 创建 Key 构建器。
     *
     * @param properties Redis Key 配置
     */
    public RedisKeyBuilder(RedisKeyProperties properties) {
        this.prefix = requireSegment(properties.keyPrefix());
    }

    /**
     * 使用已确认的非空片段生成 Key。
     *
     * @param segments 模块、业务和标识片段
     * @return 标准 Redis Key
     */
    public String build(String... segments) {
        if (segments.length < 2) {
            throw new IllegalArgumentException("Redis key requires at least module and business segments");
        }
        String suffix = Arrays.stream(segments)
                .map(RedisKeyBuilder::requireSegment)
                .reduce((left, right) -> left + ":" + right)
                .orElseThrow();
        return prefix + ":" + suffix;
    }

    /**
     * 校验 Key 片段，避免空段或分隔符破坏命名空间。
     *
     * @param value Key 片段
     * @return 合法片段
     */
    private static String requireSegment(String value) {
        if (value == null || value.isBlank() || value.contains(":")) {
            throw new IllegalArgumentException("Redis key segment must be non-blank and cannot contain ':'");
        }
        return value;
    }
}
