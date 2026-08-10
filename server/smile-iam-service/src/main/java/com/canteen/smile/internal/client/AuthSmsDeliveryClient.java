package com.canteen.smile.internal.client;

import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.dto.SmsDeliveryInternalResponse;
import com.canteen.smile.internal.client.dto.SmsDeliverySearchInternalRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** IAM 查询 Auth 自有短信投递记录使用的 HMAC RestClient。 */
@Component
@RequiredArgsConstructor
public class AuthSmsDeliveryClient {

    /** 当前 Client 日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(AuthSmsDeliveryClient.class);

    /** Auth 短信投递内部查询路径。 */
    private static final String SEARCH_PATH = "/internal/auth/v1/sms-deliveries/search";

    /** 已配置静态地址、超时和 HMAC 的 Auth Client。 */
    private final RestClient authRestClient;

    /** @param request 已收敛查询条件 @return 短信投递分页 */
    public PageResult<SmsDeliveryInternalResponse> page(SmsDeliverySearchInternalRequest request) {
        try {
            ApiResponse<PageResult<SmsDeliveryInternalResponse>> response = authRestClient.post()
                    .uri(SEARCH_PATH)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (response == null || !"0".equals(response.code()) || response.data() == null) {
                throw unavailable();
            }
            return response.data();
        } catch (RestClientException exception) {
            log.warn("Auth SMS delivery search failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    /** @return Auth 短信记录服务不可用异常 */
    private BusinessException unavailable() {
        return new BusinessException("IAM_2951", "短信发送记录服务暂时不可用，请稍后重试", 502);
    }
}
