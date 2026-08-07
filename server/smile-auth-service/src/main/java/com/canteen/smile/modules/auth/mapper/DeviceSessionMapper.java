package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import org.apache.ibatis.annotations.Param;

/** 设备会话审计索引数据访问接口。 */
public interface DeviceSessionMapper {

    /** @param entity 设备会话实体 @return 新增行数 */
    int insertDeviceSession(DeviceSessionEntity entity);

    /**
     * 查询当前有效设备会话。
     *
     * @param sessionId 设备会话 ID
     * @return 有效设备会话，不存在时为空
     */
    DeviceSessionEntity selectActiveBySessionId(@Param("sessionId") String sessionId);

    /**
     * 将当前设备会话标记为已退出。
     *
     * @param sessionId 设备会话 ID
     * @return 影响行数
     */
    int markLoggedOut(@Param("sessionId") String sessionId);
}
