package com.canteen.smile.modules.auth.service;

import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.ReauthTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 再认证票据写入事务边界。 */
@Service
@RequiredArgsConstructor
public class ReauthTicketPersistenceService {

    /** 再认证票据数据访问接口。 */
    private final ReauthTicketMapper mapper;

    /** @param entity 待持久化的再认证票据 */
    @Transactional
    public void create(ReauthTicketEntity entity) {
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("Reauth ticket was not inserted");
        }
    }
}
