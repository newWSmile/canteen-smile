package com.canteen.smile.gateway.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.AuthPublicApiPaths;
import com.canteen.smile.common.api.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/** 在网关执行粗粒度登录认证，业务服务仍负责最终权限与数据范围校验。 */
@Configuration
@RequiredArgsConstructor
public class SaTokenGatewayConfiguration {

    /** Jackson 响应序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建使用 Spring WebFlux 原生响应 API 的 Sa-Token 网关过滤器。
     * Sa-Token 1.45 Reactor 响应适配器与 Spring 6.2 的状态码方法签名不兼容，
     * 因此这里只复用 Sa-Token 登录判断和 Reactor 上下文，错误响应由本类安全输出。
     *
     * @return 网关登录认证过滤器
     */
    @Bean
    public WebFilter saTokenGatewayWebFilter() {
        return (exchange, chain) -> {
            /** 当前请求路径。 */
            String path = exchange.getRequest().getPath().value();
            if (!requiresLogin(path)) {
                return chain.filter(exchange);
            }
            SaReactorSyncHolder.setContext(exchange);
            try {
                StpUtil.checkLogin();
            } catch (NotLoginException exception) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
            } catch (NotPermissionException | NotRoleException exception) {
                return writeError(exchange, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
            } finally {
                SaReactorSyncHolder.clearContext();
            }
            return chain.filter(exchange);
        };
    }

    /** @param path 请求路径 @return 是否必须完成登录 */
    private boolean requiresLogin(String path) {
        return path.startsWith("/api/") && !AuthPublicApiPaths.ANONYMOUS_PATHS.contains(path);
    }

    /**
     * 使用 Spring 6.2 原生 ServerHttpResponse 写入统一错误响应。
     *
     * @param exchange 当前请求交换对象
     * @param status HTTP 状态
     * @param errorCode 公共稳定错误码
     * @return 完成响应的异步结果
     */
    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            ErrorCode errorCode
    ) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            /** 统一错误响应 JSON 字节。 */
            byte[] body = objectMapper.writeValueAsBytes(
                    ApiResponse.failure(errorCode, errorCode.getMessage())
            );
            /** WebFlux 响应数据缓冲区。 */
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException exception) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }
}
