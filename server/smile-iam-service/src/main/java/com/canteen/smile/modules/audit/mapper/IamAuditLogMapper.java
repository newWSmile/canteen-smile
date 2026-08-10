package com.canteen.smile.modules.audit.mapper;

import com.canteen.smile.modules.audit.entity.IamAuditLogEntity;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** IAM 管理操作审计数据访问接口。 */
public interface IamAuditLogMapper {

    /** @param entity 已脱敏审计实体 @return 新增行数 */
    int insert(IamAuditLogEntity entity);

    /**
     * 统计经过平台或租户数据边界过滤后的 IAM 审计记录。
     *
     * @param platformScope 是否平台身份范围
     * @param tenantId 租户 ID
     * @param organizationId 当前机构 ID
     * @param accountId 当前账号 ID
     * @param tenantWide 是否允许查看租户全部记录
     * @param actionCode 可选动作编码
     * @param result 可选结果
     * @param operatorId 可选操作者 ID
     * @param startTime 可选开始时间
     * @param endTime 可选结束时间
     * @return 符合条件的记录数
     */
    long countPage(
            @Param("platformScope") boolean platformScope,
            @Param("tenantId") Long tenantId,
            @Param("organizationId") Long organizationId,
            @Param("accountId") Long accountId,
            @Param("tenantWide") boolean tenantWide,
            @Param("actionCode") String actionCode,
            @Param("result") String result,
            @Param("operatorId") Long operatorId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /** 按与统计相同的 SQL 数据边界分页查询 IAM 审计记录。 */
    List<IamAuditLogEntity> selectPage(
            @Param("platformScope") boolean platformScope,
            @Param("tenantId") Long tenantId,
            @Param("organizationId") Long organizationId,
            @Param("accountId") Long accountId,
            @Param("tenantWide") boolean tenantWide,
            @Param("actionCode") String actionCode,
            @Param("result") String result,
            @Param("operatorId") Long operatorId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("limit") int limit,
            @Param("offset") long offset
    );
}
