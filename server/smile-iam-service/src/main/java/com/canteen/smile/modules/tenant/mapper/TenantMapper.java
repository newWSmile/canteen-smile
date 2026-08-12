package com.canteen.smile.modules.tenant.mapper;

import com.canteen.smile.modules.tenant.entity.TenantEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** `iam_tenant` 表数据访问接口。 */
public interface TenantMapper {

    /**
     * 按主键查询有效租户。
     *
     * @param tenantId 租户主键
     * @return 租户实体，不存在时为空
     */
    TenantEntity selectById(@Param("tenantId") long tenantId);

    /**
     * 统计符合状态条件的有效租户数量。
     *
     * @param status 可选租户状态
     * @return 有效租户数量
     */
    long countActiveTenants(@Param("status") String status);

    /**
     * 直接在 PostgreSQL 中分页查询有效租户。
     *
     * @param status 可选租户状态
     * @param limit 本页最大记录数
     * @param offset 从零开始的记录偏移量
     * @return 当前页租户实体
     */
    List<TenantEntity> selectActiveTenantPage(
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /** @return 按乐观锁修改租户名称成功的行数。 */
    int updateTenantName(@Param("tenantId") long tenantId,
                         @Param("name") String name,
                         @Param("version") long version,
                         @Param("operatorId") long operatorId);

    /** @return 按允许的原状态和乐观锁变更租户状态成功的行数。 */
    int changeTenantStatus(@Param("tenantId") long tenantId,
                           @Param("sourceStatuses") List<String> sourceStatuses,
                           @Param("targetStatus") String targetStatus,
                           @Param("version") long version,
                           @Param("operatorId") long operatorId);

    /** @return 指定租户变更后的安全版本。 */
    Long selectSecurityVersion(@Param("tenantId") long tenantId);

    /** @return 为租户所有未注销账号生成会话失效事件的行数。 */
    int insertTenantStatusChangedEvents(@Param("tenantId") long tenantId,
                                        @Param("securityVersion") long securityVersion,
                                        @Param("targetStatus") String targetStatus,
                                        @Param("actionName") String actionName,
                                        @Param("operatorId") long operatorId,
                                        @Param("ipAddress") String ipAddress);
}
