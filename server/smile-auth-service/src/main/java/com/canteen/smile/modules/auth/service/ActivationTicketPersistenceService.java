package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.entity.ActivationTicketEntity;
import com.canteen.smile.modules.auth.mapper.ActivationTicketMapper;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 激活票据和待激活凭证的本地事务边界。 */
@Service
@RequiredArgsConstructor
public class ActivationTicketPersistenceService {

    /** 激活状态竞争错误码。 */
    private static final String ACTIVATION_CONFLICT_CODE = "AUTH_1022";

    /** 激活票据数据访问接口。 */
    private final ActivationTicketMapper activationTicketMapper;

    /** 密码凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** @param ticket 新票据实体 */
    @Transactional
    public void replaceActiveTicket(ActivationTicketEntity ticket) {
        activationTicketMapper.supersedeActiveTenantTickets(ticket.getSubjectId());
        if (activationTicketMapper.insertActivationTicket(ticket) != 1) {
            throw new IllegalStateException("Activation ticket was not inserted");
        }
    }

    /**
     * 同一事务内激活凭证并消费票据，避免出现只完成其中一步的状态。
     *
     * @param ticket 已校验票据
     * @param passwordHash Argon2id 摘要
     */
    @Transactional
    public void activateCredential(ActivationTicketEntity ticket, String passwordHash) {
        int credentialRows = credentialMapper.activatePendingTenantAccountCredential(
                ticket.getSubjectId(),
                passwordHash
        );
        int ticketRows = activationTicketMapper.consume(ticket.getId(), ticket.getVersion());
        if (credentialRows != 1 || ticketRows != 1) {
            throw new BusinessException(ACTIVATION_CONFLICT_CODE, "账号激活状态已变化，请重新打开激活链接", 409);
        }
    }
}
