package com.canteen.smile.modules.audit.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthAuditLogClient;
import com.canteen.smile.internal.client.dto.AuthAuditLogInternalResponse;
import com.canteen.smile.internal.client.dto.AuthAuditLogSearchInternalRequest;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.audit.dto.AuditLogPageQuery;
import com.canteen.smile.modules.audit.entity.IamAuditLogEntity;
import com.canteen.smile.modules.audit.mapper.IamAuditLogMapper;
import com.canteen.smile.modules.audit.model.AuditDisplayCatalog;
import com.canteen.smile.modules.audit.vo.AuditLogVO;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** 聚合 IAM 自有管理审计和 Auth 自有认证审计的只读查询服务。 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    /** 单次查询允许的最大时间跨度。 */
    private static final Duration MAX_QUERY_RANGE = Duration.ofDays(90);

    /** IAM 审计数据访问接口。 */
    private final IamAuditLogMapper mapper;

    /** Auth 审计内部 Client。 */
    private final AuthAuditLogClient authAuditLogClient;

    /** 平台身份解析服务。 */
    private final PlatformActorService platformActorService;

    /** 租户身份解析服务。 */
    private final TenantActorService tenantActorService;

    /** Jackson JSON 解析器。 */
    private final ObjectMapper objectMapper;

    /** @param query 页面查询条件 @return 平台身份范围审计分页 */
    public PageResult<AuditLogVO> pagePlatform(AuditLogPageQuery query) {
        platformActorService.currentPlatformIdentityId();
        validateTimeRange(query);
        if ("AUTH".equals(query.getSource())) {
            return mapAuthPage(authAuditLogClient.page(toAuthRequest(query, "PLATFORM", null, null, false)));
        }
        return pageIam(query, true, null, null, null, false);
    }

    /** @param query 页面查询条件 @return 当前租户授权范围审计分页 */
    public PageResult<AuditLogVO> pageTenant(AuditLogPageQuery query) {
        /** 经 IAM 数据库最终校验的当前租户操作人。 */
        TenantActorContext actor = tenantActorService.current();
        validateTimeRange(query);
        if ("AUTH".equals(query.getSource())) {
            return mapAuthPage(authAuditLogClient.page(toAuthRequest(
                    query, "TENANT", actor.tenantId(), actor.accountId(), actor.rootOwner()
            )));
        }
        return pageIam(
                query, false, actor.tenantId(), actor.organizationId(), actor.accountId(), actor.rootOwner()
        );
    }

    /** 在 IAM 数据库中执行带作用域和分页的查询。 */
    private PageResult<AuditLogVO> pageIam(
            AuditLogPageQuery query,
            boolean platformScope,
            Long tenantId,
            Long organizationId,
            Long accountId,
            boolean tenantWide
    ) {
        /** 数据库作用域和筛选条件下的总记录数。 */
        long total = mapper.countPage(
                platformScope, tenantId, organizationId, accountId, tenantWide,
                query.getActionCode(), query.getResult(), query.getOperatorId(),
                query.getStartTime(), query.getEndTime()
        );
        if (total == 0) {
            return new PageResult<>(List.of(), query.getPageNo(), query.getPageSize(), 0);
        }
        /** 当前页数据库偏移量。 */
        long offset = (long) (query.getPageNo() - 1) * query.getPageSize();
        /** 当前页安全视图。 */
        List<AuditLogVO> items = mapper.selectPage(
                platformScope, tenantId, organizationId, accountId, tenantWide,
                query.getActionCode(), query.getResult(), query.getOperatorId(),
                query.getStartTime(), query.getEndTime(), query.getPageSize(), offset
        ).stream().map(this::toIamVO).toList();
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), total);
    }

    /** 构建已经按当前登录身份收敛作用域的 Auth 内部请求。 */
    private AuthAuditLogSearchInternalRequest toAuthRequest(
            AuditLogPageQuery query,
            String scopeType,
            Long tenantId,
            Long accountId,
            boolean tenantWide
    ) {
        return new AuthAuditLogSearchInternalRequest(
                query.getPageNo(), query.getPageSize(), scopeType, tenantId, accountId, tenantWide,
                query.getActionCode(), query.getResult(), query.getOperatorId(),
                query.getStartTime(), query.getEndTime()
        );
    }

    /** 将 IAM 审计实体转换为不泄露内部敏感字段的统一视图。 */
    private AuditLogVO toIamVO(IamAuditLogEntity row) {
        return new AuditLogVO(
                Long.toString(row.getId()), "IAM", nullableId(row.getTenantId()),
                row.getOperatorType(), AuditDisplayCatalog.identityTypeName(row.getOperatorType()),
                nullableId(row.getOperatorId()), row.getOperatorUsernameSnapshot(),
                row.getOperatorDisplayNameSnapshot(), row.getActionCode(),
                AuditDisplayCatalog.actionName(row.getActionNameSnapshot()), row.getTargetType(),
                AuditDisplayCatalog.targetTypeName(row.getTargetType()), row.getTargetId(),
                row.getTargetNameSnapshot(), row.getTargetCodeSnapshot(), row.getResult(), row.getReason(),
                null, null, row.getFailureReasonCode(),
                AuditDisplayCatalog.failureReasonName(row.getFailureReasonCode()),
                null, null, row.getIpAddress(), row.getTraceId(), row.getOccurredTime(),
                row.getAppCodeSnapshot(), categoryPath(row.getCategoryPathJson()), row.getDurationMs()
        );
    }

    /** 将 Auth 内部分页转换为对前端稳定的统一视图。 */
    private PageResult<AuditLogVO> mapAuthPage(PageResult<AuthAuditLogInternalResponse> page) {
        /** 转换后的统一 Auth 审计视图。 */
        List<AuditLogVO> items = page.items().stream().map(row -> new AuditLogVO(
                row.id(), "AUTH", row.tenantId(), row.operatorType(),
                AuditDisplayCatalog.identityTypeName(row.operatorType()), row.operatorId(),
                row.operatorUsernameSnapshot(), row.operatorDisplayNameSnapshot(), row.actionCode(),
                AuditDisplayCatalog.actionName(row.actionNameSnapshot()),
                firstText(row.targetType(), row.subjectType()),
                AuditDisplayCatalog.targetTypeName(firstText(row.targetType(), row.subjectType())),
                firstText(row.targetId(), row.subjectId()),
                firstText(row.targetNameSnapshot(), row.subjectDisplayNameSnapshot()),
                firstText(row.targetCodeSnapshot(), row.subjectUsernameSnapshot()),
                row.result(), row.reason(),
                row.loginMethod(), AuditDisplayCatalog.loginMethodName(row.loginMethod()),
                row.failureReasonCode(), AuditDisplayCatalog.failureReasonName(row.failureReasonCode()),
                row.maskedMobile(), row.deviceSummary(), row.ipAddress(), row.traceId(), row.occurredTime(),
                row.appCode(), row.categoryPath(), row.durationMs()
        )).toList();
        return new PageResult<>(items, page.pageNo(), page.pageSize(), page.total());
    }

    /** 校验查询时间先后关系及最大跨度。 */
    private void validateTimeRange(AuditLogPageQuery query) {
        if (query.getStartTime() == null || query.getEndTime() == null) return;
        if (query.getStartTime().isAfter(query.getEndTime())
                || Duration.between(query.getStartTime(), query.getEndTime()).compareTo(MAX_QUERY_RANGE) > 0) {
            throw new BusinessException("IAM_2901", "审计查询时间范围必须有效且不能超过 90 天", 400);
        }
    }

    /** @param value 可选 bigint @return JavaScript 安全的字符串 ID */
    private String nullableId(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /** @return 数据库 JSON 分类路径的安全投影，历史空值返回空列表 */
    private List<String> categoryPath(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            return List.of();
        }
    }

    /** @return 首个非空文本，用于兼容非通用注解产生的历史审计 */
    private String firstText(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
