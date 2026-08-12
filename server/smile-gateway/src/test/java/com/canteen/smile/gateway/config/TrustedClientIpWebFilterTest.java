package com.canteen.smile.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** 网关客户端 IP 清洗与可信代理边界测试。 */
class TrustedClientIpWebFilterTest {

    /** 验证本机 Vite 代理可以传递浏览器的真实局域网地址。 */
    @Test
    void shouldTrustForwardedAddressFromLoopbackDevelopmentProxy() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/v1/login/password")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 5174))
                .header("X-Forwarded-For", "192.168.0.66")
                .header(TrustedClientIpWebFilter.TRUSTED_CLIENT_IP_HEADER, "198.51.100.10")
                .build();

        assertThat(filteredClientIp(request)).isEqualTo("192.168.0.66");
    }

    /** 验证非受信任远端不能伪造内部 IP 头或代理链头。 */
    @Test
    void shouldUseTcpPeerForUntrustedRemoteRequest() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/v1/login/password")
                .remoteAddress(new InetSocketAddress("192.168.0.88", 55000))
                .header("X-Forwarded-For", "198.51.100.20")
                .header(TrustedClientIpWebFilter.TRUSTED_CLIENT_IP_HEADER, "198.51.100.10")
                .build();

        assertThat(filteredClientIp(request)).isEqualTo("192.168.0.88");
    }

    /** @return 经过过滤器清洗后传给下游的客户端 IP */
    private String filteredClientIp(MockServerHttpRequest request) {
        AtomicReference<String> clientIp = new AtomicReference<>();
        new TrustedClientIpWebFilter().filter(
                MockServerWebExchange.from(request),
                exchange -> {
                    clientIp.set(exchange.getRequest().getHeaders().getFirst(
                            TrustedClientIpWebFilter.TRUSTED_CLIENT_IP_HEADER
                    ));
                    return reactor.core.publisher.Mono.empty();
                }
        ).block();
        return clientIp.get();
    }
}
