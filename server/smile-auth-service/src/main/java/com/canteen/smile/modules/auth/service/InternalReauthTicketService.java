package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.ReauthTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/** IAM 敏感命令使用的再认证票据原子消费服务。 */
@Service
@RequiredArgsConstructor
public class InternalReauthTicketService {

    /** 再认证票据数据访问接口。 */
    private final ReauthTicketMapper mapper;

    /**
     * 校验主体、动作和有效期后原子消费票据。
     *
     * @param rawTicket 原始票据
     * @param subjectType 主体类型
     * @param subjectId 主体 ID
     * @param allowedAction 唯一允许动作
     */
    @Transactional
    public void consume(String rawTicket, String subjectType, long subjectId, String allowedAction) {
        String ticketHash = HmacRequestSigner.sha256Hex(rawTicket.getBytes(StandardCharsets.UTF_8));
        ReauthTicketEntity ticket = mapper.selectByHash(ticketHash);
        if (ticket == null
                || !subjectType.equals(ticket.getSubjectType())
                || subjectId != ticket.getSubjectId()
                || !allowedAction.equals(ticket.getAllowedAction())
                || !"ACTIVE".equals(ticket.getStatus())
                || !ticket.getExpiresAt().isAfter(OffsetDateTime.now())
                || mapper.consume(ticket.getId(), ticket.getVersion(), subjectType, subjectId, allowedAction) != 1) {
            throw new BusinessException("AUTH_1203", "再认证票据无效或已经使用", 401);
        }
    }
}
