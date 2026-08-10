package com.canteen.smile.modules.securityevent.mapper;

import org.apache.ibatis.annotations.Param;

/** Auth 安全事件幂等记录、快照、会话和审计的数据访问接口。 */
public interface SecurityEventMapper {

    /** @return 指定事件已有的消费结果，不存在时为空。 */
    ConsumedEventRow selectConsumedEvent(@Param("eventId") String eventId);

    /** @return 成功插入幂等消费记录的行数，重复事件返回 0。 */
    int insertConsumedEvent(@Param("eventId") String eventId,
                            @Param("eventType") String eventType,
                            @Param("payloadDigest") String payloadDigest);

    /** @return 失效的活动权限快照数量。 */
    int invalidatePermissionSnapshots(@Param("tenantId") long tenantId,
                                      @Param("accountId") long accountId);

    /** @return 失效的活动设备会话数量。 */
    int invalidateDeviceSessions(@Param("tenantId") long tenantId,
                                 @Param("accountId") long accountId);

    /** @return 新增系统安全审计日志数量。 */
    int insertSecurityAudit(@Param("tenantId") long tenantId,
                            @Param("accountId") long accountId,
                            @Param("actionCode") String actionCode,
                            @Param("traceId") String traceId);

    /** 已消费事件最小投影。 */
    record ConsumedEventRow(String eventId, String eventType, String payloadDigest, String result) {
    }
}
