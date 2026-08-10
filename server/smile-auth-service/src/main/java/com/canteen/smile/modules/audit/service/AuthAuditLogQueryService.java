package com.canteen.smile.modules.audit.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.dto.AuthAuditLogInternalResponse;
import com.canteen.smile.internal.dto.AuthAuditLogSearchRequest;
import com.canteen.smile.modules.audit.mapper.AuthAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/** Auth 审计日志只读分页查询服务。 */
@Service
@RequiredArgsConstructor
public class AuthAuditLogQueryService {

    /** 单次查询允许的最大时间跨度。 */
    private static final Duration MAX_QUERY_RANGE = Duration.ofDays(90);

    /** Auth 审计数据访问接口。 */
    private final AuthAuditLogMapper mapper;

    /**
     * 在数据库中应用平台或租户数据边界并分页查询。
     *
     * @param query IAM 已签名的查询条件
     * @return Auth 审计分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<AuthAuditLogInternalResponse> page(AuthAuditLogSearchRequest query) {
        validateScope(query);
        validateTimeRange(query);
        /** 数据库作用域和筛选条件下的总记录数。 */
        long total = mapper.countPage(query);
        if (total == 0) {
            return new PageResult<>(List.of(), query.pageNo(), query.pageSize(), 0);
        }
        /** 当前页数据库偏移量。 */
        long offset = (long) (query.pageNo() - 1) * query.pageSize();
        /** 当前页安全响应投影。 */
        List<AuthAuditLogInternalResponse> items = mapper.selectPage(query, query.pageSize(), offset)
                .stream().map(this::toResponse).toList();
        return new PageResult<>(items, query.pageNo(), query.pageSize(), total);
    }

    /** 校验内部调用方不能构造越界作用域。 */
    private void validateScope(AuthAuditLogSearchRequest query) {
        if ("PLATFORM".equals(query.scopeType())) {
            if (query.tenantId() != null || query.accountId() != null || query.tenantWide()) {
                throw invalidQuery("平台审计查询不能携带租户范围");
            }
            return;
        }
        if (!"TENANT".equals(query.scopeType()) || query.tenantId() == null
                || (!query.tenantWide() && query.accountId() == null)) {
            throw invalidQuery("租户审计查询缺少有效的数据边界");
        }
    }

    /** 校验时间先后关系及最大跨度。 */
    private void validateTimeRange(AuthAuditLogSearchRequest query) {
        if (query.startTime() == null || query.endTime() == null) return;
        if (query.startTime().isAfter(query.endTime())
                || Duration.between(query.startTime(), query.endTime()).compareTo(MAX_QUERY_RANGE) > 0) {
            throw invalidQuery("审计查询时间范围必须有效且不能超过 90 天");
        }
    }

    /** 将数据库投影转换为内部契约，并将 bigint 作为字符串传输。 */
    private AuthAuditLogInternalResponse toResponse(AuthAuditLogMapper.AuthAuditLogRow row) {
        return new AuthAuditLogInternalResponse(
                Long.toString(row.id()),
                row.tenantId() == null ? null : Long.toString(row.tenantId()),
                row.subjectType(),
                row.subjectId() == null ? null : Long.toString(row.subjectId()),
                row.operatorType(),
                Long.toString(row.operatorId()),
                row.actionCode(),
                row.result(),
                row.loginMethod(),
                row.failureReasonCode(),
                row.maskedMobile(),
                row.deviceSummary(),
                row.traceId(),
                row.occurredTime()
        );
    }

    /** @param message 明确错误说明 @return 内部查询参数异常 */
    private BusinessException invalidQuery(String message) {
        return new BusinessException("AUTH_1301", message, 400);
    }
}
