package com.canteen.smile.modules.sms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** 短信脱敏投递记录数据访问接口。 */
@Mapper
public interface SmsDeliveryRecordMapper {

    /**
     * 原子领取一次短信发送请求；重复请求 ID 不会再次领取。
     *
     * @param requestId 发送请求唯一标识
     * @param challengeId 关联挑战标识
     * @param providerCode 短信策略编码
     * @param purpose 业务用途
     * @param maskedMobile 脱敏手机号
     * @param mobileHash 手机号查询摘要
     * @param templateCode 模板编码
     * @param contentSnapshot 按记录创建时安全策略生成的短信正文快照
     * @param sensitiveContentRetained 正文是否保留验证码等敏感内容
     * @return 新增行数，零表示请求 ID 已存在
     */
    int insertProcessing(
            @Param("requestId") String requestId,
            @Param("challengeId") String challengeId,
            @Param("providerCode") String providerCode,
            @Param("purpose") String purpose,
            @Param("maskedMobile") String maskedMobile,
            @Param("mobileHash") String mobileHash,
            @Param("templateCode") String templateCode,
            @Param("contentSnapshot") String contentSnapshot,
            @Param("sensitiveContentRetained") boolean sensitiveContentRetained
    );

    /**
     * 将已领取请求标记为策略接受。
     *
     * @param requestId 发送请求唯一标识
     * @param providerMessageId 供应商消息标识
     * @param acceptedTime 策略接受时间
     * @return 更新行数
     */
    int markAccepted(
            @Param("requestId") String requestId,
            @Param("providerMessageId") String providerMessageId,
            @Param("acceptedTime") OffsetDateTime acceptedTime
    );

    /**
     * 将已领取请求标记为失败。
     *
     * @param requestId 发送请求唯一标识
     * @param failureCode 稳定内部失败编码
     * @param failureMessage 脱敏失败说明
     * @return 更新行数
     */
    int markFailed(
            @Param("requestId") String requestId,
            @Param("failureCode") String failureCode,
            @Param("failureMessage") String failureMessage
    );

    /** @param mobileHash 可选手机号摘要 @param startTime 开始时间 @param endTime 结束时间 @return 记录数 */
    long countPage(
            @Param("mobileHash") String mobileHash,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 分页查询短信投递记录。
     *
     * @param mobileHash 可选手机号摘要
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 页大小
     * @param offset 偏移量
     * @return 当前页审计投影
     */
    List<SmsDeliveryRow> selectPage(
            @Param("mobileHash") String mobileHash,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 短信投递数据库查询投影。
     *
     * @param id 记录 ID
     * @param requestId 请求唯一标识
     * @param challengeId 关联挑战标识
     * @param providerCode 策略编码
     * @param purpose 业务用途
     * @param maskedMobile 脱敏手机号
     * @param templateCode 模板编码
     * @param contentSnapshot 按安全策略生成的正文快照
     * @param sensitiveContentRetained 正文是否保留验证码等敏感内容
     * @param status 投递状态
     * @param providerMessageId 供应商消息标识
     * @param failureCode 失败编码
     * @param failureMessage 脱敏失败说明
     * @param acceptedTime 策略接受时间
     * @param createdTime 创建时间
     */
    record SmsDeliveryRow(
            long id,
            String requestId,
            String challengeId,
            String providerCode,
            String purpose,
            String maskedMobile,
            String templateCode,
            String contentSnapshot,
            boolean sensitiveContentRetained,
            String status,
            String providerMessageId,
            String failureCode,
            String failureMessage,
            OffsetDateTime acceptedTime,
            OffsetDateTime createdTime
    ) {
    }
}
