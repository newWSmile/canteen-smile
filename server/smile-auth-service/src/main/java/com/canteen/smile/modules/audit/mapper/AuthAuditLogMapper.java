package com.canteen.smile.modules.audit.mapper;

import com.canteen.smile.internal.dto.AuthAuditLogSearchRequest;
import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** Auth 自有认证安全审计数据访问接口。 */
public interface AuthAuditLogMapper {

    /**
     * 在设备会话创建事务内追加登录成功审计。
     *
     * @param entity 已持久化设备会话
     * @param actionCode 与登录方式对应的动作编码
     * @param traceId 当前链路 ID
     * @return 新增行数
     */
    int insertSessionCreatedAudit(
            @Param("entity") DeviceSessionEntity entity,
            @Param("actionCode") String actionCode,
            @Param("traceId") String traceId
    );

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
     * @param operatorType 操作者类型
     * @param operatorId 操作者 ID
     * @param actionCode 动作编码
     * @param result 结果
     * @param loginMethod 登录方式
     * @param failureReasonCode 失败原因码
     * @param maskedMobile 脱敏手机号
     * @param deviceSummary 脱敏设备摘要
     * @param traceId 链路 ID
     * @param occurredTime 事件发生时间
     */
    record AuthAuditLogRow(
            long id,
            Long tenantId,
            String subjectType,
            Long subjectId,
            String operatorType,
            long operatorId,
            String actionCode,
            String result,
            String loginMethod,
            String failureReasonCode,
            String maskedMobile,
            String deviceSummary,
            String traceId,
            OffsetDateTime occurredTime
    ) {
    }
}
