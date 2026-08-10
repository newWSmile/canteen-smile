package com.canteen.smile.modules.sms.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.internal.client.AuthSmsDeliveryClient;
import com.canteen.smile.internal.client.dto.SmsDeliveryInternalResponse;
import com.canteen.smile.internal.client.dto.SmsDeliverySearchInternalRequest;
import com.canteen.smile.modules.sms.dto.SmsDeliveryPageQuery;
import com.canteen.smile.modules.sms.vo.SmsDeliveryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 平台短信记录查询编排服务，IAM 只做权限收敛而不读取 Auth 数据库。 */
@Service
@RequiredArgsConstructor
public class PlatformSmsDeliveryService {

    /** Auth 短信记录内部 Client。 */
    private final AuthSmsDeliveryClient authSmsDeliveryClient;

    /** @param query 平台筛选条件 @return 短信记录分页 */
    public PageResult<SmsDeliveryVO> page(SmsDeliveryPageQuery query) {
        PageResult<SmsDeliveryInternalResponse> page =
                authSmsDeliveryClient.page(new SmsDeliverySearchInternalRequest(
                query.getPageNo(),
                query.getPageSize(),
                query.getMobile(),
                query.getStartTime(),
                query.getEndTime()
        ));
        List<SmsDeliveryVO> items = page.items().stream().map(row -> new SmsDeliveryVO(
                row.id(),
                row.requestId(),
                row.challengeId(),
                row.providerCode(),
                row.purpose(),
                row.maskedMobile(),
                row.templateCode(),
                row.content(),
                row.sensitiveContentRetained(),
                row.status(),
                row.providerMessageId(),
                row.failureCode(),
                row.failureMessage(),
                row.acceptedTime(),
                row.createdTime()
        )).toList();
        return new PageResult<>(items, page.pageNo(), page.pageSize(), page.total());
    }
}
