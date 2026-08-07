package com.canteen.smile.internal.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** IAM → Auth 静态地址 RestClient 配置。 */
@Configuration
@EnableConfigurationProperties({AuthClientProperties.class, AuthHmacClientProperties.class})
public class AuthRestClientConfiguration {

    /**
     * 创建具有明确超时和 HMAC 拦截器的 Auth RestClient。
     *
     * @param builder Spring 管理的构建器
     * @param clientProperties Auth 地址和超时
     * @param hmacProperties HMAC 配置
     * @return Auth RestClient
     */
    @Bean
    public RestClient authRestClient(RestClient.Builder builder, AuthClientProperties clientProperties,
                                     AuthHmacClientProperties hmacProperties) {
        /** 有限连接和读取超时的请求工厂。 */
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(clientProperties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(clientProperties.getReadTimeoutMillis());
        return builder.baseUrl(clientProperties.getBaseUrl()).requestFactory(requestFactory)
                .requestInterceptor(new AuthHmacRequestInterceptor(hmacProperties)).build();
    }
}
