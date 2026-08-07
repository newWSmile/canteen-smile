package com.canteen.smile.internal.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Auth → IAM 静态地址 RestClient 配置。 */
@Configuration
public class IamRestClientConfiguration {

    /**
     * 创建具有明确超时和 HMAC 拦截器的 IAM RestClient。
     *
     * @param builder Spring 管理的 RestClient 构建器
     * @param iamProperties IAM 地址和超时
     * @param hmacProperties HMAC 客户端配置
     * @return IAM RestClient
     */
    @Bean
    public RestClient iamRestClient(
            RestClient.Builder builder,
            IamClientProperties iamProperties,
            InternalHmacClientProperties hmacProperties
    ) {
        /** 使用有限连接和读取超时的请求工厂。 */
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(iamProperties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(iamProperties.getReadTimeoutMillis());
        return builder
                .baseUrl(iamProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(new InternalHmacRequestInterceptor(hmacProperties))
                .build();
    }
}
