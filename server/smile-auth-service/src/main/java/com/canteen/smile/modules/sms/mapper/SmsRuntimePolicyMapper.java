package com.canteen.smile.modules.sms.mapper;

import com.canteen.smile.modules.sms.entity.SmsRuntimePolicyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 全局短信运行策略数据访问接口。 */
@Mapper
public interface SmsRuntimePolicyMapper {

    /** @return 当前有效的全局短信策略，不存在时为空 */
    SmsRuntimePolicyEntity selectGlobal();

    /**
     * 在全局策略不存在时写入配置默认值。
     *
     * @param policy 配置默认策略
     * @return 新增行数，已存在时为零
     */
    int insertDefault(SmsRuntimePolicyEntity policy);

    /**
     * 乐观锁修改验证码和多维限流设置。
     *
     * @param policy 待更新策略
     * @param actorId 平台身份 ID
     * @return 更新行数
     */
    int updateRateLimits(@Param("policy") SmsRuntimePolicyEntity policy, @Param("actorId") long actorId);

    /**
     * 乐观锁修改验证码明文留存开关。
     *
     * @param enabled 是否启用
     * @param version 当前版本
     * @param actorId 平台身份 ID
     * @return 更新行数
     */
    int updateSecurity(
            @Param("enabled") boolean enabled,
            @Param("version") long version,
            @Param("actorId") long actorId
    );
}
