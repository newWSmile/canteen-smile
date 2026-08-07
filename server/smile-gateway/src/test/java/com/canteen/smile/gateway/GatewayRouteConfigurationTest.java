package com.canteen.smile.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 网关 Auth 与 IAM 静态服务路由配置测试。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test"
)
class GatewayRouteConfigurationTest {

    /** 聚合配置文件中声明的网关路由。 */
    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    /** 随机端口网关测试客户端。 */
    @Autowired
    private WebTestClient webTestClient;

    /** 验证 Auth 和 IAM API 使用互不重叠的服务前缀。 */
    @Test
    void shouldRouteAuthAndIamRequestsToIndependentServices() {
        /** 配置文件声明的全部路由。 */
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();
        /** 认证服务路由。 */
        RouteDefinition authRoute = routes.stream()
                .filter(route -> "auth-service".equals(route.getId()))
                .findFirst()
                .orElseThrow();
        /** 身份与访问管理服务路由。 */
        RouteDefinition iamRoute = routes.stream()
                .filter(route -> "iam-service".equals(route.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(authRoute.getUri()).isEqualTo(URI.create("http://127.0.0.1:18081"));
        assertThat(authRoute.getPredicates())
                .anySatisfy(predicate -> {
                    assertThat(predicate.getName()).isEqualTo("Path");
                    assertThat(predicate.getArgs()).containsValue("/api/auth/v1/**");
                });
        assertThat(iamRoute.getUri()).isEqualTo(URI.create("http://127.0.0.1:18082"));
        assertThat(iamRoute.getPredicates())
                .anySatisfy(predicate -> {
                    assertThat(predicate.getName()).isEqualTo("Path");
                    assertThat(predicate.getArgs()).containsValue("/api/iam/v1/**");
                });
        assertThat(routes)
                .flatExtracting(RouteDefinition::getPredicates)
                .allSatisfy(predicate -> assertThat(predicate.getArgs()).doesNotContainValue("/api/**"));
    }

    /** 验证未登录请求由 Gateway 原生返回统一 401，而不是触发 Reactor 适配错误。 */
    @Test
    void shouldReturnUnifiedUnauthorizedResponse() {
        webTestClient.get()
                .uri("/api/iam/v1/platform/tenants?pageNo=1&pageSize=20")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTH_401")
                .jsonPath("$.message").isEqualTo("请先登录");
    }

    /** 验证密码加密挑战属于匿名接口，不会在转发到 Auth 前被 Gateway 返回 401。 */
    @Test
    void shouldAllowAnonymousPasswordEncryptionChallenge() {
        webTestClient.post()
                .uri("/api/auth/v1/password-encryption/challenges")
                .header("Content-Type", "application/json")
                .bodyValue("{\"purpose\":\"PLATFORM_PASSWORD_LOGIN\"}")
                .exchange()
                .expectStatus()
                .value(status -> assertThat(status).isNotEqualTo(401));
    }
}
