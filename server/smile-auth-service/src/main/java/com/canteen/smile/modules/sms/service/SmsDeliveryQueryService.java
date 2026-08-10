package com.canteen.smile.modules.sms.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.dto.SmsDeliveryInternalResponse;
import com.canteen.smile.internal.dto.SmsDeliverySearchRequest;
import com.canteen.smile.modules.sms.mapper.SmsDeliveryRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/** 平台短信投递记录只读分页查询服务。 */
@Service
@RequiredArgsConstructor
public class SmsDeliveryQueryService {

    /** 单次查询允许的最大时间跨度。 */
    private static final Duration MAX_QUERY_RANGE = Duration.ofDays(90);

    /** 投递记录 Mapper。 */
    private final SmsDeliveryRecordMapper mapper;

    /** 手机号安全摘要服务。 */
    private final MobileProtectionService mobileProtectionService;

    /**
     * 使用手机号精确摘要和时间范围分页查询。
     *
     * @param query IAM 已签名查询条件
     * @return 短信投递分页
     */
    @Transactional(readOnly = true)
    public PageResult<SmsDeliveryInternalResponse> page(SmsDeliverySearchRequest query) {
        validateTimeRange(query);
        String mobileHash = query.mobile() == null || query.mobile().isBlank()
                ? null
                : mobileProtectionService.hashForSearch(query.mobile());
        long total = mapper.countPage(mobileHash, query.startTime(), query.endTime());
        if (total == 0) {
            return new PageResult<>(List.of(), query.pageNo(), query.pageSize(), 0);
        }
        long offset = (long) (query.pageNo() - 1) * query.pageSize();
        List<SmsDeliveryInternalResponse> items = mapper.selectPage(
                        mobileHash,
                        query.startTime(),
                        query.endTime(),
                        query.pageSize(),
                        offset
                ).stream()
                .map(this::toResponse)
                .toList();
        return new PageResult<>(items, query.pageNo(), query.pageSize(), total);
    }

    /** 校验时间先后关系及最大跨度。 */
    private void validateTimeRange(SmsDeliverySearchRequest query) {
        if (query.startTime() == null || query.endTime() == null) return;
        if (query.startTime().isAfter(query.endTime())
                || Duration.between(query.startTime(), query.endTime()).compareTo(MAX_QUERY_RANGE) > 0) {
            throw new BusinessException("AUTH_1401", "短信记录查询时间范围必须有效且不能超过 90 天", 400);
        }
    }

    /** 将数据库投影转换为不包含手机号摘要的内部响应。 */
    private SmsDeliveryInternalResponse toResponse(SmsDeliveryRecordMapper.SmsDeliveryRow row) {
        return new SmsDeliveryInternalResponse(
                Long.toString(row.id()),
                row.requestId(),
                row.challengeId(),
                row.providerCode(),
                row.purpose(),
                row.maskedMobile(),
                row.templateCode(),
                row.contentSnapshot(),
                row.sensitiveContentRetained(),
                row.status(),
                row.providerMessageId(),
                row.failureCode(),
                row.failureMessage(),
                row.acceptedTime(),
                row.createdTime()
        );
    }
}
