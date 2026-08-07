package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.entity.PasswordResetTicketEntity;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import com.canteen.smile.modules.auth.mapper.PasswordHistoryMapper;
import com.canteen.smile.modules.auth.mapper.PasswordResetTicketMapper;
import com.canteen.smile.modules.auth.mapper.ReauthTicketMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 密码恢复票据、凭证和设备会话的本地事务边界。 */
@Service
@RequiredArgsConstructor
public class PasswordResetPersistenceService {

    /** 密码恢复并发冲突错误码。 */
    private static final String RESET_CONFLICT_CODE = "AUTH_1205";

    /** 再认证票据数据访问接口。 */
    private final ReauthTicketMapper reauthTicketMapper;

    /** 密码重置票据数据访问接口。 */
    private final PasswordResetTicketMapper resetTicketMapper;

    /** 认证凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** 设备会话数据访问接口。 */
    private final DeviceSessionMapper deviceSessionMapper;

    /** 密码历史数据访问接口。 */
    private final PasswordHistoryMapper passwordHistoryMapper;

    /**
     * 消费平台再认证票据、使账号进入待重置并替换恢复票据。
     *
     * @param reauthTicket 已校验再认证票据
     * @param resetTicket 新密码恢复票据
     */
    @Transactional
    public void initiate(ReauthTicketEntity reauthTicket, PasswordResetTicketEntity resetTicket) {
        int reauthRows = reauthTicketMapper.consume(
                reauthTicket.getId(),
                reauthTicket.getVersion(),
                reauthTicket.getSubjectId(),
                reauthTicket.getAllowedAction()
        );
        int credentialRows = credentialMapper.markTenantAccountResetRequired(resetTicket.getSubjectId());
        if (reauthRows != 1 || credentialRows != 1) {
            throw conflict("密码恢复发起状态已变化，请重新验证当前密码");
        }
        resetTicketMapper.supersedeActiveTenantTickets(resetTicket.getSubjectId());
        if (resetTicketMapper.insert(resetTicket) != 1) {
            throw new IllegalStateException("Password reset ticket was not inserted");
        }
        deviceSessionMapper.invalidateActiveBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                resetTicket.getSubjectId()
        );
    }

    /**
     * 保存旧密码历史、写入新密码并消费恢复票据。
     *
     * @param ticket 已校验密码恢复票据
     * @param passwordHash 新 Argon2id 密码摘要
     */
    @Transactional
    public void complete(PasswordResetTicketEntity ticket, String passwordHash) {
        if (passwordHistoryMapper.insertCurrentCredential(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                ticket.getSubjectId()
        ) != 1) {
            throw conflict("账号密码状态已变化，请重新打开恢复链接");
        }
        int credentialRows = credentialMapper.completeTenantAccountPasswordReset(
                ticket.getSubjectId(),
                passwordHash
        );
        int ticketRows = resetTicketMapper.consume(ticket.getId(), ticket.getVersion());
        if (credentialRows != 1 || ticketRows != 1) {
            throw conflict("密码恢复状态已变化，请重新打开恢复链接");
        }
    }

    /** @param message 对外稳定消息 @return 密码恢复冲突异常 */
    private BusinessException conflict(String message) {
        return new BusinessException(RESET_CONFLICT_CODE, message, 409);
    }
}
