package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.ActivationTicketEntity;
import org.apache.ibatis.annotations.Param;

/** 账号激活票据数据访问接口。 */
public interface ActivationTicketMapper {

    /** @param subjectId 租户账号 ID @return 废弃的有效票据数量 */
    int supersedeActiveTenantTickets(@Param("subjectId") long subjectId);

    /** @param entity 新激活票据 @return 新增行数 */
    int insertActivationTicket(ActivationTicketEntity entity);

    /** @param ticketHash 原始票据摘要 @return 匹配的票据，不存在时为空 */
    ActivationTicketEntity selectByHash(@Param("ticketHash") String ticketHash);

    /**
     * 原子消费有效且未过期的激活票据。
     *
     * @param ticketId 票据主键
     * @param version 乐观锁版本
     * @return 更新行数
     */
    int consume(@Param("ticketId") long ticketId, @Param("version") long version);
}
