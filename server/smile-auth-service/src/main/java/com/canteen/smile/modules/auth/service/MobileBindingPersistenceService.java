package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.MobileBindingEntity;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import com.canteen.smile.modules.auth.mapper.MobileBindingMapper;
import com.canteen.smile.modules.auth.mapper.ReauthTicketMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.ReauthAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/** 手机号绑定查询、唯一写入和安全审计的本地事务边界。 */
@Service
@RequiredArgsConstructor
public class MobileBindingPersistenceService {

    /** 当前账号已经绑定手机号的错误码。 */
    private static final String ALREADY_BOUND_CODE = "AUTH_1301";

    /** 当前手机号或再认证状态已经变化的错误码。 */
    private static final String MOBILE_STATE_CONFLICT_CODE = "AUTH_1304";

    /** 手机号绑定数据访问接口。 */
    private final MobileBindingMapper mobileBindingMapper;

    /** 再认证票据数据访问接口。 */
    private final ReauthTicketMapper reauthTicketMapper;

    /** 设备会话数据访问接口。 */
    private final DeviceSessionMapper deviceSessionMapper;

    /** @param subjectType 主体类型 @param subjectId 主体 ID @return 当前有效绑定 */
    @Transactional(readOnly = true)
    public MobileBindingEntity findVerified(String subjectType, long subjectId) {
        return mobileBindingMapper.selectVerifiedBySubject(subjectType, subjectId);
    }

    /**
     * 原子写入首次绑定；成功审计由上层注解在提交后异步追加。
     *
     * @param entity 已验证并加密的手机号绑定
     * @param subject 当前可信租户账号
     */
    @Transactional
    public void bind(
            MobileBindingEntity entity,
            CurrentAuthSubjectService.CurrentTenantSubject subject
    ) {
        if (mobileBindingMapper.insertVerified(entity) != 1) {
            throw new BusinessException(ALREADY_BOUND_CODE, "当前账号已经绑定手机号", 409);
        }
    }

    /**
     * 只读校验当前账号的换绑或解绑再认证票据，避免无效票据消耗后续验证码。
     *
     * @param rawTicket 原始再认证票据
     * @param subject 当前可信租户账号
     * @param action 当前敏感动作
     */
    @Transactional(readOnly = true)
    public void validateReauth(
            String rawTicket,
            CurrentAuthSubjectService.CurrentTenantSubject subject,
            ReauthAction action
    ) {
        requireActiveReauth(rawTicket, subject, action);
    }

    /**
     * 原子消费再认证票据、替换手机号绑定并失效全部设备会话。
     *
     * @param currentBinding 当前有效手机号绑定
     * @param newBinding 已验证并加密的新手机号绑定
     * @param rawTicket 原始再认证票据
     * @param subject 当前可信租户账号
     */
    @Transactional
    public void change(
            MobileBindingEntity currentBinding,
            MobileBindingEntity newBinding,
            String rawTicket,
            CurrentAuthSubjectService.CurrentTenantSubject subject
    ) {
        /** 当前账号仍有效且仅允许换绑操作的再认证票据。 */
        ReauthTicketEntity reauthTicket = requireActiveReauth(
                rawTicket, subject, ReauthAction.MOBILE_CHANGE
        );
        if (mobileBindingMapper.replaceVerified(
                currentBinding.getId(), subject.accountId(), currentBinding.getVersion()
        ) != 1) {
            throw stateConflict();
        }
        if (mobileBindingMapper.insertVerified(newBinding) != 1) {
            throw stateConflict();
        }
        consumeReauth(reauthTicket, subject, ReauthAction.MOBILE_CHANGE);
        invalidateSessions(subject.accountId());
    }

    /**
     * 原子消费再认证票据、撤销手机号绑定并失效全部设备会话。
     *
     * @param currentBinding 当前有效手机号绑定
     * @param rawTicket 原始再认证票据
     * @param subject 当前可信租户账号
     */
    @Transactional
    public void unbind(
            MobileBindingEntity currentBinding,
            String rawTicket,
            CurrentAuthSubjectService.CurrentTenantSubject subject
    ) {
        /** 当前账号仍有效且仅允许解绑操作的再认证票据。 */
        ReauthTicketEntity reauthTicket = requireActiveReauth(
                rawTicket, subject, ReauthAction.MOBILE_UNBIND
        );
        if (mobileBindingMapper.revokeVerified(
                currentBinding.getId(), subject.accountId(), currentBinding.getVersion()
        ) != 1) {
            throw stateConflict();
        }
        consumeReauth(reauthTicket, subject, ReauthAction.MOBILE_UNBIND);
        invalidateSessions(subject.accountId());
    }

    /** 校验原始票据、当前主体、唯一动作、状态和有效期。 */
    private ReauthTicketEntity requireActiveReauth(
            String rawTicket,
            CurrentAuthSubjectService.CurrentTenantSubject subject,
            ReauthAction action
    ) {
        /** 原始再认证票据的不可逆摘要。 */
        String ticketHash = HmacRequestSigner.sha256Hex(
                rawTicket.getBytes(StandardCharsets.UTF_8)
        );
        /** 数据库中与摘要匹配的再认证票据。 */
        ReauthTicketEntity ticket = reauthTicketMapper.selectByHash(ticketHash);
        if (ticket == null
                || !AuthConstants.TENANT_ACCOUNT_SUBJECT.equals(ticket.getSubjectType())
                || ticket.getSubjectId() == null
                || ticket.getSubjectId() != subject.accountId()
                || !action.name().equals(ticket.getAllowedAction())
                || !AuthConstants.ACTIVE_STATUS.equals(ticket.getStatus())
                || ticket.getExpiresAt() == null
                || !ticket.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw new BusinessException("AUTH_1203", "再认证票据无效、已过期或用途不匹配", 401);
        }
        return ticket;
    }

    /** 原子消费已经完成全部业务校验的再认证票据。 */
    private void consumeReauth(
            ReauthTicketEntity ticket,
            CurrentAuthSubjectService.CurrentTenantSubject subject,
            ReauthAction action
    ) {
        if (reauthTicketMapper.consume(
                ticket.getId(),
                ticket.getVersion(),
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                subject.accountId(),
                action.name()
        ) != 1) {
            throw stateConflict();
        }
    }

    /** 失效当前账号在 Auth 数据库中的全部活动设备会话。 */
    private void invalidateSessions(long accountId) {
        deviceSessionMapper.invalidateActiveBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                accountId
        );
    }

    /** @return 不泄露并发内部状态的手机号安全操作冲突 */
    private BusinessException stateConflict() {
        return new BusinessException(
                MOBILE_STATE_CONFLICT_CODE,
                "手机号安全状态已经变化，请重新发起操作",
                409
        );
    }
}
