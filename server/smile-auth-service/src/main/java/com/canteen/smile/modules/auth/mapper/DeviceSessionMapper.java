package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.DeviceSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** 设备会话审计索引数据访问接口。 */
@Mapper
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

    /**
     * 使指定认证主体的全部有效设备会话失效。
     *
     * @param subjectType 认证主体类型
     * @param subjectId 认证主体 ID
     * @return 失效会话数量
     */
    int invalidateActiveBySubject(
            @Param("subjectType") String subjectType,
            @Param("subjectId") long subjectId
    );

    /**
     * 批量查询候选账号最近一次登录时间，用于多账号选择排序与提示。
     *
     * @param accountIds 候选租户账号 ID
     * @return 有登录历史的账号及最近登录时间
     */
    List<LatestLoginRow> selectLatestTenantLogins(@Param("accountIds") List<Long> accountIds);

    /** @param accountId 租户账号 ID @param latestLoginTime 最近登录时间 */
    record LatestLoginRow(long accountId, OffsetDateTime latestLoginTime) {
    }
}
