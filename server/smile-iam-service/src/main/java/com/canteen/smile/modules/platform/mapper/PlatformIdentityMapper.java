package com.canteen.smile.modules.platform.mapper;

import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.entity.UsernameRegistryEntity;
import org.apache.ibatis.annotations.Param;

/** 平台身份和首位身份用户名注册数据访问接口。 */
public interface PlatformIdentityMapper {

    /** @return 尚未逻辑删除的平台身份数量 */
    long countPlatformIdentities();

    /**
     * 按归一化用户名查询平台身份。
     *
     * @param normalizedUsername 归一化用户名
     * @return 平台身份，不存在时为空
     */
    PlatformIdentityEntity selectByNormalizedUsername(@Param("normalizedUsername") String normalizedUsername);

    /**
     * 按 ID 查询平台身份。
     *
     * @param id 平台身份 ID
     * @return 平台身份，不存在时为空
     */
    PlatformIdentityEntity selectById(@Param("id") long id);

    /**
     * 查询用户名是否已被任何历史身份永久占用。
     *
     * @param normalizedUsername 归一化用户名
     * @return 占用记录数量
     */
    long countReservedUsername(@Param("normalizedUsername") String normalizedUsername);

    /**
     * 新增初始化中的平台身份。
     *
     * @param entity 平台身份实体
     * @return 新增行数
     */
    int insertPlatformIdentity(PlatformIdentityEntity entity);

    /**
     * 永久保留首位平台身份用户名。
     *
     * @param entity 用户名注册实体
     * @return 新增行数
     */
    int insertUsernameRegistry(UsernameRegistryEntity entity);

    /**
     * 将初始化中的平台身份激活。
     *
     * @param id 平台身份 ID
     * @param version 当前乐观锁版本
     * @return 更新行数
     */
    int activatePlatformIdentity(@Param("id") long id, @Param("version") long version);
}
