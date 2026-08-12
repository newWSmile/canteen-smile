package com.canteen.smile.gateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/** 清洗外部来源 IP 请求头，并向下游传递由网关确认的客户端地址。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrustedClientIpWebFilter implements WebFilter {

    /** 仅供网关向内部服务传递的可信客户端 IP 请求头。 */
    public static final String TRUSTED_CLIENT_IP_HEADER = "X-Smile-Client-IP";

    /** 标准代理链请求头。 */
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** {@inheritDoc} */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        InetAddress peerAddress = peerAddress(exchange.getRequest().getRemoteAddress());
        String clientIp = peerAddress == null ? null : peerAddress.getHostAddress();
        if (peerAddress != null && peerAddress.isLoopbackAddress()) {
            String forwardedIp = firstForwardedIp(exchange.getRequest().getHeaders().getFirst(FORWARDED_FOR_HEADER));
            if (forwardedIp != null) {
                clientIp = forwardedIp;
            }
        }
        String resolvedClientIp = clientIp;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(TRUSTED_CLIENT_IP_HEADER);
                    if (resolvedClientIp != null) {
                        headers.set(TRUSTED_CLIENT_IP_HEADER, resolvedClientIp);
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    /** @return 当前 TCP 上一跳地址。 */
    private InetAddress peerAddress(InetSocketAddress remoteAddress) {
        return remoteAddress == null ? null : remoteAddress.getAddress();
    }

    /** @return 代理链中的第一个合法 IP；格式异常时忽略。 */
    private String firstForwardedIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String candidate = headerValue.split(",", 2)[0].strip();
        try {
            return InetAddress.getByName(candidate).getHostAddress();
        } catch (Exception ignored) {
            return null;
        }
    }
}
