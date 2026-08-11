package com.canteen.smile.modules.audit.mapper;

import com.canteen.smile.internal.dto.AuthAuditLogSearchRequest;
import com.canteen.smile.modules.audit.entity.AuthAsyncAuditEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** Auth 自有认证安全审计数据访问接口。 */
@Mapper
public interface AuthAuditLogMapper {

    /** @param entity 通用注解生成的不可变异步审计实体 @return 新增行数；幂等重复时为零 */
    int insertAsyncAudit(AuthAsyncAuditEntity entity);

    /** @param query 已验证查询条件 @return 符合权限边界的记录数 */
    long countPage(@Param("query") AuthAuditLogSearchRequest query);

    /** @param query 已验证查询条件 @param limit 页大小 @param offset 偏移量 @return 当前页投影 */
    List<AuthAuditLogRow> selectPage(
            @Param("query") AuthAuditLogSearchRequest query,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * Auth 审计表安全查询投影。
     *
     * @param id 日志 ID
     * @param tenantId 租户 ID
     * @param subjectType 主体类型
     * @param subjectId 主体 ID
     * @param subjectUsernameSnapshot 主体用户名快照
     * @param subjectDisplayNameSnapshot 主体显示名称快照
     * @param operatorType 操作者类型
     * @param operatorId 操作者 ID
     * @param operatorUsernameSnapshot 操作者用户名快照
     * @param operatorDisplayNameSnapshot 操作者显示名称快照
     * @param actionCode 动作编码
     * @param actionNameSnapshot 中文动作名称快照
     * @param result 结果
     * @param loginMethod 登录方式
     * @param failureReasonCode 失败原因码
     * @param maskedMobile 脱敏手机号
     * @param deviceSummary 脱敏设备摘要
     * @param traceId 链路 ID
     * @param occurredTime 事件发生时间
     * @param appCodeSnapshot 操作人所在应用端编码快照
     * @param categoryPathJson 任意层级中文分类路径 JSON
     * @param targetType 被操作目标类型
     * @param targetId 被操作目标 ID
     * @param targetNameSnapshot 被操作目标名称快照
     * @param targetCodeSnapshot 被操作目标业务编码快照
     * @param reason 可选操作原因
     * @param durationMs 业务方法执行耗时毫秒数
     */
    record AuthAuditLogRow(
            long id,
            Long tenantId,
            String subjectType,
            Long subjectId,
            String subjectUsernameSnapshot,
            String subjectDisplayNameSnapshot,
            String operatorType,
            long operatorId,
            String operatorUsernameSnapshot,
            String operatorDisplayNameSnapshot,
            String actionCode,
            String actionNameSnapshot,
            String result,
            String loginMethod,
            String failureReasonCode,
            String maskedMobile,
            String deviceSummary,
            String traceId,
            OffsetDateTime occurredTime,
            String appCodeSnapshot,
            String categoryPathJson,
            String targetType,
            String targetId,
            String targetNameSnapshot,
            String targetCodeSnapshot,
            String reason,
            Long durationMs
    ) {
    }
}
