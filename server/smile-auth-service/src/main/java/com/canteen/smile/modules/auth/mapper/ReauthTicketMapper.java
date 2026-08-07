package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import org.apache.ibatis.annotations.Param;

/** 敏感操作再认证票据数据访问接口。 */
public interface ReauthTicketMapper {

    /** @param entity 新再认证票据 @return 新增行数 */
    int insert(ReauthTicketEntity entity);

    /** @param ticketHash 原始票据摘要 @return 匹配票据，不存在时为空 */
    ReauthTicketEntity selectByHash(@Param("ticketHash") String ticketHash);

    /**
     * 原子消费指定主体和操作的有效再认证票据。
     *
     * @param ticketId 票据主键
     * @param version 乐观锁版本
     * @param subjectId 发起平台身份 ID
     * @param allowedAction 允许执行的敏感操作
     * @return 更新行数
     */
    int consume(
            @Param("ticketId") long ticketId,
            @Param("version") long version,
            @Param("subjectId") long subjectId,
            @Param("allowedAction") String allowedAction
    );
}
