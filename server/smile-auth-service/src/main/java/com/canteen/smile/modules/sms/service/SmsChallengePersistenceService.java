package com.canteen.smile.modules.sms.service;

import com.canteen.smile.modules.sms.entity.SmsChallengeEntity;
import com.canteen.smile.modules.sms.mapper.SmsChallengeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 短信挑战创建、失败计数和一次性状态迁移事务边界。 */
@Service
@RequiredArgsConstructor
public class SmsChallengePersistenceService {

    /** 短信挑战 Mapper。 */
    private final SmsChallengeMapper smsChallengeMapper;

    /**
     * 废弃同手机号、同用途旧挑战并创建新挑战。
     *
     * @param entity 新挑战实体
     */
    @Transactional
    public void create(SmsChallengeEntity entity) {
        smsChallengeMapper.invalidateOpenChallenges(entity.getMobileHash(), entity.getPurpose());
        int inserted = smsChallengeMapper.insert(entity);
        if (inserted != 1) {
            throw new IllegalStateException("SMS challenge insert did not affect exactly one row");
        }
    }

    /** @param challengeId 挑战标识 @return 挑战实体，不存在时为空 */
    @Transactional(readOnly = true)
    public SmsChallengeEntity find(String challengeId) {
        return smsChallengeMapper.selectByChallengeId(challengeId);
    }

    /** @param challengeId 挑战标识 @param maxAttempts 最大错误次数 @return 是否成功消费 */
    @Transactional
    public boolean consume(String challengeId, int maxAttempts) {
        return smsChallengeMapper.consume(challengeId, maxAttempts) == 1;
    }

    /** @param challengeId 挑战标识 @param maxAttempts 最大错误次数 */
    @Transactional
    public void recordFailure(String challengeId, int maxAttempts) {
        smsChallengeMapper.recordFailure(challengeId, maxAttempts);
    }

    /** @param challengeId 挑战标识 */
    @Transactional
    public void markExpired(String challengeId) {
        smsChallengeMapper.markExpired(challengeId);
    }

    /** @param challengeId 挑战标识 */
    @Transactional
    public void invalidate(String challengeId) {
        smsChallengeMapper.invalidate(challengeId);
    }
}
