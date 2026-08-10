package com.canteen.smile.modules.sms.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.internal.dto.SmsRateLimitPolicyUpdateInternalRequest;
import com.canteen.smile.internal.dto.SmsSecurityPolicyUpdateInternalRequest;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.ReauthAction;
import com.canteen.smile.modules.auth.service.InternalReauthTicketService;
import com.canteen.smile.modules.sms.entity.SmsRuntimePolicyEntity;
import com.canteen.smile.modules.sms.mapper.SmsRuntimePolicyMapper;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 全局短信运行策略读取和敏感修改事务服务。 */
@Service
@RequiredArgsConstructor
public class SmsRuntimePolicyService {

    /** 策略并发更新冲突错误码。 */
    private static final String POLICY_CONFLICT_CODE = "AUTH_1410";

    /** 策略数据访问接口。 */
    private final SmsRuntimePolicyMapper mapper;
    /** 配置文件提供的安全默认值。 */
    private final SmsProperties properties;
    /** 一次性管理员再认证票据消费服务。 */
    private final InternalReauthTicketService reauthTicketService;

    /** @return 当前策略；尚未保存时返回配置安全默认值且不在查询中写库 */
    @Transactional(readOnly = true)
    public SmsRuntimePolicy current() {
        SmsRuntimePolicyEntity entity = mapper.selectGlobal();
        return toPolicy(entity == null ? defaultEntity() : entity);
    }

    /** @param request 限流设置和再认证票据 @return 更新后的策略 */
    @Transactional
    public SmsRuntimePolicy updateRateLimits(SmsRateLimitPolicyUpdateInternalRequest request) {
        ensurePersistedDefault();
        validateRateHierarchy(request);
        consumePlatformReauth(request.actorId(), request.reauthTicket());
        SmsRuntimePolicyEntity entity = new SmsRuntimePolicyEntity();
        entity.setChallengeTtlSeconds(request.challengeTtlSeconds());
        entity.setResendIntervalSeconds(request.resendIntervalSeconds());
        entity.setMaxVerificationAttempts(request.maxVerificationAttempts());
        entity.setMobileHourlyLimit(request.mobileHourlyLimit());
        entity.setMobileDailyLimit(request.mobileDailyLimit());
        entity.setIpHourlyLimit(request.ipHourlyLimit());
        entity.setIpDailyLimit(request.ipDailyLimit());
        entity.setDeviceHourlyLimit(request.deviceHourlyLimit());
        entity.setDeviceDailyLimit(request.deviceDailyLimit());
        entity.setVersion(request.version());
        if (mapper.updateRateLimits(entity, request.actorId()) != 1) throw conflict();
        return toPolicy(mapper.selectGlobal());
    }

    /** @param request 安全开关和再认证票据 @return 更新后的策略 */
    @Transactional
    public SmsRuntimePolicy updateSecurity(SmsSecurityPolicyUpdateInternalRequest request) {
        ensurePersistedDefault();
        consumePlatformReauth(request.actorId(), request.reauthTicket());
        if (mapper.updateSecurity(
                request.plaintextCodeRetentionEnabled(), request.version(), request.actorId()
        ) != 1) {
            throw conflict();
        }
        return toPolicy(mapper.selectGlobal());
    }

    /** 校验每日限额不得低于对应小时限额。 */
    private void validateRateHierarchy(SmsRateLimitPolicyUpdateInternalRequest request) {
        if (request.mobileDailyLimit() < request.mobileHourlyLimit()
                || request.ipDailyLimit() < request.ipHourlyLimit()
                || request.deviceDailyLimit() < request.deviceHourlyLimit()) {
            throw new BusinessException("AUTH_1409", "每日发送限额不能低于对应的每小时限额", 400);
        }
    }

    /** 原子消费绑定短信策略修改动作的平台密码再认证票据。 */
    private void consumePlatformReauth(long actorId, String ticket) {
        reauthTicketService.consume(
                ticket,
                AuthConstants.PLATFORM_IDENTITY_SUBJECT,
                actorId,
                ReauthAction.PLATFORM_SMS_POLICY_UPDATE.name()
        );
    }

    /** @return 从 yml 默认值构造且默认不保留敏感内容的策略实体 */
    private SmsRuntimePolicyEntity defaultEntity() {
        SmsRuntimePolicyEntity entity = new SmsRuntimePolicyEntity();
        entity.setChallengeTtlSeconds(properties.getChallengeTtlSeconds());
        entity.setResendIntervalSeconds(properties.getResendIntervalSeconds());
        entity.setMaxVerificationAttempts(properties.getMaxVerificationAttempts());
        entity.setMobileHourlyLimit(properties.getMobileHourlyLimit());
        entity.setMobileDailyLimit(properties.getMobileDailyLimit());
        entity.setIpHourlyLimit(properties.getIpHourlyLimit());
        entity.setIpDailyLimit(properties.getIpDailyLimit());
        entity.setDeviceHourlyLimit(properties.getDeviceHourlyLimit());
        entity.setDeviceDailyLimit(properties.getDeviceDailyLimit());
        entity.setPlaintextCodeRetentionEnabled(false);
        entity.setVersion(0L);
        return entity;
    }

    /** 在首个修改命令中幂等持久化默认策略，避免 GET 请求产生数据库副作用。 */
    private void ensurePersistedDefault() {
        mapper.insertDefault(defaultEntity());
    }

    /** @param entity 数据库实体 @return 不暴露持久化对象的运行策略 */
    private SmsRuntimePolicy toPolicy(SmsRuntimePolicyEntity entity) {
        return new SmsRuntimePolicy(
                entity.getChallengeTtlSeconds(), entity.getResendIntervalSeconds(),
                entity.getMaxVerificationAttempts(), entity.getMobileHourlyLimit(),
                entity.getMobileDailyLimit(), entity.getIpHourlyLimit(), entity.getIpDailyLimit(),
                entity.getDeviceHourlyLimit(), entity.getDeviceDailyLimit(),
                Boolean.TRUE.equals(entity.getPlaintextCodeRetentionEnabled()),
                entity.getUpdatedTime(), entity.getVersion()
        );
    }

    /** @return 统一乐观锁冲突异常 */
    private BusinessException conflict() {
        return new BusinessException(POLICY_CONFLICT_CODE, "短信设置已被其他管理员修改，请刷新后重试", 409);
    }
}
