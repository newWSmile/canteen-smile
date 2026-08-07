package com.canteen.smile.modules.auth.service;

import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设备会话数据库事务边界。 */
@Service
@RequiredArgsConstructor
public class DeviceSessionPersistenceService {

    /** 设备会话数据访问接口。 */
    private final DeviceSessionMapper deviceSessionMapper;

    /** @param entity 设备会话实体 */
    @Transactional
    public void create(DeviceSessionEntity entity) {
        if (deviceSessionMapper.insertDeviceSession(entity) != 1) {
            throw new IllegalStateException("Device session was not inserted");
        }
    }

    /** @param sessionId 会话 ID @return 当前有效设备会话 */
    @Transactional(readOnly = true)
    public DeviceSessionEntity findActive(String sessionId) {
        return deviceSessionMapper.selectActiveBySessionId(sessionId);
    }

    /** @param sessionId 会话 ID */
    @Transactional
    public void markLoggedOut(String sessionId) {
        deviceSessionMapper.markLoggedOut(sessionId);
    }
}
