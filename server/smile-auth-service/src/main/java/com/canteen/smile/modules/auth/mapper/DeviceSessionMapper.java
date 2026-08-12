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
     * 将已经不在 Sa-Token 有效终端集合中的设备会话标记为失效。
     *
     * @param subjectType 认证主体类型
     * @param subjectId 认证主体 ID
     * @param activeSessionIds Sa-Token 当前仍有效的业务设备会话 ID
     * @return 失效的陈旧设备会话数量
     */
    int invalidateActiveExceptSessions(
            @Param("subjectType") String subjectType,
            @Param("subjectId") long subjectId,
            @Param("activeSessionIds") List<String> activeSessionIds
    );

    /**
     * 批量查询候选账号最近一次登录时间，用于多账号选择排序与提示。
     *
     * @param accountIds 候选租户账号 ID
     * @return 有登录历史的账号及最近登录时间
     */
    List<LatestLoginRow> selectLatestTenantLogins(@Param("accountIds") List<Long> accountIds);

    /** @return 当前账号有效设备会话总数。 */
    long countActiveByTenantAccount(@Param("tenantId") long tenantId,
                                    @Param("accountId") long accountId);

    /** @return 当前账号有效设备会话分页。 */
    List<DeviceSessionEntity> selectActiveByTenantAccount(
            @Param("tenantId") long tenantId,
            @Param("accountId") long accountId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /** @return 当前账号拥有的指定有效设备会话。 */
    DeviceSessionEntity selectOwnedActiveSession(@Param("tenantId") long tenantId,
                                                 @Param("accountId") long accountId,
                                                 @Param("sessionId") String sessionId);

    /** @return 乐观锁标记指定设备会话失效的行数。 */
    int invalidateOwnedSession(@Param("tenantId") long tenantId,
                               @Param("accountId") long accountId,
                               @Param("sessionId") String sessionId,
                               @Param("version") long version);

    /** @return 标记当前设备之外全部有效会话失效的行数。 */
    int invalidateOtherSessions(@Param("tenantId") long tenantId,
                                @Param("accountId") long accountId,
                                @Param("currentSessionId") String currentSessionId);

    /** @param accountId 租户账号 ID @param latestLoginTime 最近登录时间 */
    record LatestLoginRow(long accountId, OffsetDateTime latestLoginTime) {
    }
}
