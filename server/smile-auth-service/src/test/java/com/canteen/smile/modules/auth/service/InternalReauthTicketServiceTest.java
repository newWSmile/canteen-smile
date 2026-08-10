package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.ReauthTicketMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** IAM 调用的再认证票据原子消费测试。 */
class InternalReauthTicketServiceTest {

    /** 验证主体与动作完全匹配时只消费一次。 */
    @Test
    void shouldConsumeMatchingTicket() {
        ReauthTicketMapper mapper = mock(ReauthTicketMapper.class);
        ReauthTicketEntity ticket = activeTicket();
        when(mapper.selectByHash(anyString())).thenReturn(ticket);
        when(mapper.consume(9L, 0L, "TENANT_ACCOUNT", 31L, "TENANT_USER_ROLE_ASSIGN")).thenReturn(1);

        new InternalReauthTicketService(mapper).consume(
                "raw-ticket", "TENANT_ACCOUNT", 31L, "TENANT_USER_ROLE_ASSIGN"
        );

        verify(mapper).consume(9L, 0L, "TENANT_ACCOUNT", 31L, "TENANT_USER_ROLE_ASSIGN");
    }

    /** 验证票据不能跨动作复用。 */
    @Test
    void shouldRejectDifferentActionBeforeConsume() {
        ReauthTicketMapper mapper = mock(ReauthTicketMapper.class);
        when(mapper.selectByHash(anyString())).thenReturn(activeTicket());

        assertThatThrownBy(() -> new InternalReauthTicketService(mapper).consume(
                "raw-ticket", "TENANT_ACCOUNT", 31L, "TENANT_USER_CREATE"
        )).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("AUTH_1203");
        verify(mapper, never()).consume(9L, 0L, "TENANT_ACCOUNT", 31L, "TENANT_USER_CREATE");
    }

    /** @return 有效的租户角色分配再认证票据 */
    private ReauthTicketEntity activeTicket() {
        ReauthTicketEntity ticket = new ReauthTicketEntity();
        ticket.setId(9L);
        ticket.setVersion(0L);
        ticket.setSubjectType("TENANT_ACCOUNT");
        ticket.setSubjectId(31L);
        ticket.setAllowedAction("TENANT_USER_ROLE_ASSIGN");
        ticket.setStatus("ACTIVE");
        ticket.setExpiresAt(OffsetDateTime.now().plusMinutes(2));
        return ticket;
    }
}
