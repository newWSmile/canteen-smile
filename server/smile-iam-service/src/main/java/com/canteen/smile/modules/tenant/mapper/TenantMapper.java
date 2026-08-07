package com.canteen.smile.modules.tenant.mapper;

import com.canteen.smile.modules.tenant.entity.TenantEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** `iam_tenant` 表数据访问接口。 */
public interface TenantMapper {

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
}
