package com.canteen.smile.modules.sms.mapper;

import com.canteen.smile.modules.sms.entity.SmsChallengeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 短信验证码挑战数据访问接口。 */
@Mapper
public interface SmsChallengeMapper {

    /** @return 被废弃的同手机号、同用途未完成挑战数量 */
    int invalidateOpenChallenges(
            @Param("mobileHash") String mobileHash,
            @Param("purpose") String purpose
    );

    /** @param entity 新短信挑战 @return 新增行数 */
    int insert(SmsChallengeEntity entity);

    /** @param challengeId 外部挑战标识 @return 匹配的有效逻辑记录，不存在时为空 */
    SmsChallengeEntity selectByChallengeId(@Param("challengeId") String challengeId);

    /** @param challengeId 挑战标识 @param maxAttempts 最大错误次数 @return 成功消费行数 */
    int consume(
            @Param("challengeId") String challengeId,
            @Param("maxAttempts") int maxAttempts
    );

    /** @param challengeId 挑战标识 @param maxAttempts 最大错误次数 @return 更新行数 */
    int recordFailure(
            @Param("challengeId") String challengeId,
            @Param("maxAttempts") int maxAttempts
    );

    /** @param challengeId 挑战标识 @return 被标记过期的行数 */
    int markExpired(@Param("challengeId") String challengeId);

    /** @param challengeId 挑战标识 @return 被主动失效的行数 */
    int invalidate(@Param("challengeId") String challengeId);
}
