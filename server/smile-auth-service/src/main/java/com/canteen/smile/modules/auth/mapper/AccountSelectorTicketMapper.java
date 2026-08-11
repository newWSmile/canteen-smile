package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 手机号登录账号选择票据数据访问接口。 */
@Mapper
public interface AccountSelectorTicketMapper {

    /** @param entity 新签发的票据摘要 @return 新增行数 */
    int insert(AccountSelectorTicketEntity entity);

    /** @param ticketHash 原始票据摘要 @return 当前有效票据，不存在时为空 */
    AccountSelectorTicketEntity selectActiveByHash(@Param("ticketHash") String ticketHash);

    /**
     * 使用乐观锁原子消费一次性票据。
     *
     * @param id 票据 ID
     * @param version 当前版本
     * @return 更新行数
     */
    int consume(@Param("id") long id, @Param("version") long version);
}
