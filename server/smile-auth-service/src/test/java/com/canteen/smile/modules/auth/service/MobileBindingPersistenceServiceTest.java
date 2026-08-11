package com.canteen.smile.modules.auth.service;

import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.MobileBindingEntity;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import com.canteen.smile.modules.auth.mapper.MobileBindingMapper;
import com.canteen.smile.modules.auth.mapper.ReauthTicketMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.ReauthAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 手机号换绑与解绑本地事务行为测试。 */
class MobileBindingPersistenceServiceTest {

    /** 手机号绑定数据访问替身。 */
    private MobileBindingMapper mobileBindingMapper;

    /** 再认证票据数据访问替身。 */
    private ReauthTicketMapper reauthTicketMapper;

    /** 设备会话数据访问替身。 */
    private DeviceSessionMapper deviceSessionMapper;

    /** 被测试事务服务。 */
    private MobileBindingPersistenceService persistenceService;

    /** 初始化独立数据访问替身。 */
    @BeforeEach
    void setUp() {
        mobileBindingMapper = mock(MobileBindingMapper.class);
        reauthTicketMapper = mock(ReauthTicketMapper.class);
        deviceSessionMapper = mock(DeviceSessionMapper.class);
        persistenceService = new MobileBindingPersistenceService(
                mobileBindingMapper,
                reauthTicketMapper,
                deviceSessionMapper
        );
    }

    /** 验证换绑会替代旧绑定、消费票据、失效全部会话并记录实际验证方式。 */
    @Test
    void shouldChangeMobileAndInvalidateAllSessions() {
        /** 当前账号安全上下文。 */
        CurrentAuthSubjectService.CurrentTenantSubject subject =
                new CurrentAuthSubjectService.CurrentTenantSubject(
                        7L, 2L, "test_user", "测试用户"
                );
        /** 当前有效旧绑定。 */
        MobileBindingEntity current = binding(11L, 3L, "138****8000");
        /** 已验证待写入的新绑定。 */
        MobileBindingEntity replacement = binding(null, null, "139****9000");
        /** 仅允许换绑且通过短信验证的有效票据。 */
        ReauthTicketEntity ticket = ticket(ReauthAction.MOBILE_CHANGE, "SMS");
        when(reauthTicketMapper.selectByHash(hash("change-ticket"))).thenReturn(ticket);
        when(mobileBindingMapper.replaceVerified(11L, 7L, 3L)).thenReturn(1);
        when(mobileBindingMapper.insertVerified(replacement)).thenReturn(1);
        when(reauthTicketMapper.consume(
                21L, 1L, AuthConstants.TENANT_ACCOUNT_SUBJECT, 7L, "MOBILE_CHANGE"
        )).thenReturn(1);
        persistenceService.change(current, replacement, "change-ticket", subject);

        verify(deviceSessionMapper).invalidateActiveBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT, 7L
        );
    }

    /** 验证解绑会撤销当前绑定并记录当前密码验证方式。 */
    @Test
    void shouldUnbindMobileWithPasswordReauthentication() {
        /** 当前账号安全上下文。 */
        CurrentAuthSubjectService.CurrentTenantSubject subject =
                new CurrentAuthSubjectService.CurrentTenantSubject(
                        7L, 2L, "test_user", "测试用户"
                );
        /** 当前有效手机号绑定。 */
        MobileBindingEntity current = binding(11L, 3L, "138****8000");
        /** 仅允许解绑且通过当前密码验证的有效票据。 */
        ReauthTicketEntity ticket = ticket(ReauthAction.MOBILE_UNBIND, "PASSWORD");
        when(reauthTicketMapper.selectByHash(hash("unbind-ticket"))).thenReturn(ticket);
        when(mobileBindingMapper.revokeVerified(11L, 7L, 3L)).thenReturn(1);
        when(reauthTicketMapper.consume(
                21L, 1L, AuthConstants.TENANT_ACCOUNT_SUBJECT, 7L, "MOBILE_UNBIND"
        )).thenReturn(1);
        persistenceService.unbind(current, "unbind-ticket", subject);

        verify(deviceSessionMapper).invalidateActiveBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT, 7L
        );
    }

    /** 创建测试手机号绑定实体。 */
    private MobileBindingEntity binding(Long id, Long version, String maskedMobile) {
        /** 当前测试使用的手机号绑定。 */
        MobileBindingEntity entity = new MobileBindingEntity();
        entity.setId(id);
        entity.setVersion(version);
        entity.setMaskedMobile(maskedMobile);
        return entity;
    }

    /** 创建当前账号指定敏感动作的有效再认证票据。 */
    private ReauthTicketEntity ticket(ReauthAction action, String verifyMethod) {
        /** 当前测试使用的再认证票据。 */
        ReauthTicketEntity entity = new ReauthTicketEntity();
        entity.setId(21L);
        entity.setVersion(1L);
        entity.setSubjectType(AuthConstants.TENANT_ACCOUNT_SUBJECT);
        entity.setSubjectId(7L);
        entity.setAllowedAction(action.name());
        entity.setVerifyMethod(verifyMethod);
        entity.setStatus(AuthConstants.ACTIVE_STATUS);
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        return entity;
    }

    /** 计算与生产代码相同的原始票据摘要。 */
    private String hash(String rawTicket) {
        return HmacRequestSigner.sha256Hex(rawTicket.getBytes(StandardCharsets.UTF_8));
    }
}
