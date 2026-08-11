package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.mapper.AccountSelectorTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 手机号登录账号选择票据的本地事务边界。 */
@Service
@RequiredArgsConstructor
public class AccountSelectorTicketPersistenceService {

    /** 账号选择票据无效错误码。 */
    private static final String INVALID_SELECTOR_TICKET_CODE = "AUTH_1014";

    /** 账号选择票据数据访问接口。 */
    private final AccountSelectorTicketMapper mapper;

    /** @param entity 待持久化的票据摘要 */
    @Transactional
    public void create(AccountSelectorTicketEntity entity) {
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("Account selector ticket was not inserted");
        }
    }

    /** @param ticketHash 原始票据摘要 @return 当前有效票据 */
    @Transactional(readOnly = true)
    public AccountSelectorTicketEntity requireActive(String ticketHash) {
        AccountSelectorTicketEntity entity = mapper.selectActiveByHash(ticketHash);
        if (entity == null) throw invalidTicket();
        return entity;
    }

    /** @param entity 已完成全部业务校验的票据 */
    @Transactional
    public void consume(AccountSelectorTicketEntity entity) {
        if (mapper.consume(entity.getId(), entity.getVersion()) != 1) throw invalidTicket();
    }

    /** @return 不泄露票据状态细节的统一异常 */
    private BusinessException invalidTicket() {
        return new BusinessException(INVALID_SELECTOR_TICKET_CODE, "账号选择凭证无效或已过期", 400);
    }
}
