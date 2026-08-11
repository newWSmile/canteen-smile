package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.MobileBindingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 已验证手机号绑定数据访问接口。 */
@Mapper
public interface MobileBindingMapper {

    /** @param subjectType 主体类型 @param subjectId 主体 ID @return 当前有效绑定 */
    MobileBindingEntity selectVerifiedBySubject(
            @Param("subjectType") String subjectType,
            @Param("subjectId") long subjectId
    );

    /** @param entity 已完成验证码校验的绑定实体 @return 新增行数；主体已有绑定时为零 */
    int insertVerified(MobileBindingEntity entity);

    /**
     * 按手机号摘要一次查询全部有效租户账号绑定，禁止循环访问数据库。
     *
     * @param mobileHash 带服务端 Pepper 的手机号摘要
     * @return 最多一百个租户账号 ID
     */
    List<Long> selectVerifiedTenantAccountIdsByMobileHash(@Param("mobileHash") String mobileHash);
}
