package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.dto.DeviceSessionPageQuery;
import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 设备会话数据库读写边界，避免在数据库事务中执行 Redis 会话操作。 */
@Service
@RequiredArgsConstructor
public class DeviceSessionPersistenceService {

    /** 设备会话数据访问接口。 */
    private final DeviceSessionMapper mapper;

    /**
     * 创建新的设备会话记录。
     *
     * @param entity 设备会话实体
     */
    @Transactional
    public void create(DeviceSessionEntity entity) {
        if (mapper.insertDeviceSession(entity) != 1) {
            throw new IllegalStateException("Device session was not inserted");
        }
    }

    /**
     * 创建新设备会话，并按 Sa-Token 当前有效终端集合清理已被顶下线的陈旧记录。
     *
     * @param entity 新设备会话实体
     * @param activeSessionIds Sa-Token 当前仍有效的业务设备会话 ID
     */
    @Transactional
    public void createAndReconcile(DeviceSessionEntity entity, List<String> activeSessionIds) {
        if (activeSessionIds == null || activeSessionIds.isEmpty()) {
            throw new IllegalArgumentException("Active session ids must not be empty");
        }
        if (mapper.insertDeviceSession(entity) != 1) {
            throw new IllegalStateException("Device session was not inserted");
        }
        mapper.invalidateActiveExceptSessions(
                entity.getSubjectType(), entity.getSubjectId(), activeSessionIds
        );
    }

    /**
     * 查询当前有效设备会话。
     *
     * @param sessionId 会话 ID
     * @return 当前有效设备会话
     */
    @Transactional(readOnly = true)
    public DeviceSessionEntity findActive(String sessionId) {
        return mapper.selectActiveBySessionId(sessionId);
    }

    /**
     * 标记指定会话主动退出。
     *
     * @param sessionId 会话 ID
     */
    @Transactional
    public void markLoggedOut(String sessionId) {
        mapper.markLoggedOut(sessionId);
    }

    /**
     * 分页查询指定租户账号的有效设备会话。
     *
     * @param tenantId 租户 ID
     * @param accountId 账号 ID
     * @param query 分页参数
     * @return 设备会话实体分页
     */
    @Transactional(readOnly = true)
    public PageResult<DeviceSessionEntity> page(long tenantId, long accountId, DeviceSessionPageQuery query) {
        long total = mapper.countActiveByTenantAccount(tenantId, accountId);
        List<DeviceSessionEntity> items = mapper.selectActiveByTenantAccount(
                tenantId, accountId,
                (long) (query.getPageNo() - 1) * query.getPageSize(), query.getPageSize()
        );
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), total);
    }

    /**
     * 使用乐观锁失效当前账号拥有的指定设备会话。
     *
     * @param tenantId 租户 ID
     * @param accountId 账号 ID
     * @param sessionId 业务设备会话 ID
     * @param version 乐观锁版本
     */
    @Transactional
    public void invalidateOwnedSession(long tenantId, long accountId, String sessionId, long version) {
        DeviceSessionEntity session = mapper.selectOwnedActiveSession(tenantId, accountId, sessionId);
        if (session == null) {
            throw new BusinessException("AUTH_1301", "设备会话不存在或已经失效", 404);
        }
        if (mapper.invalidateOwnedSession(tenantId, accountId, sessionId, version) != 1) {
            throw new BusinessException("AUTH_1302", "设备会话状态已经变化，请刷新后重试", 409);
        }
    }

    /**
     * 失效当前设备以外的全部账号设备会话。
     *
     * @param tenantId 租户 ID
     * @param accountId 账号 ID
     * @param currentSessionId 当前业务设备会话 ID
     */
    @Transactional
    public void invalidateOtherSessions(long tenantId, long accountId, String currentSessionId) {
        mapper.invalidateOtherSessions(tenantId, accountId, currentSessionId);
    }
}
