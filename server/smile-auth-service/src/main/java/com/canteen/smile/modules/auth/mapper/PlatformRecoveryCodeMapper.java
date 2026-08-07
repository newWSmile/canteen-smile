package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.PlatformRecoveryCodeEntity;
import org.apache.ibatis.annotations.Param;

/** 平台一次性恢复码数据访问接口。 */
public interface PlatformRecoveryCodeMapper {

    /**
     * 将指定平台身份原有有效恢复码整批废弃。
     *
     * @param platformIdentityId 平台身份 ID
     * @return 更新行数
     */
    int supersedeActiveCodes(@Param("platformIdentityId") long platformIdentityId);

    /**
     * 新增只保存摘要的恢复码。
     *
     * @param entity 恢复码实体
     * @return 新增行数
     */
    int insertRecoveryCode(PlatformRecoveryCodeEntity entity);

    /**
     * 加锁查询仍然有效的恢复码。
     *
     * @param platformIdentityId 平台身份 ID
     * @param codeHash 恢复码摘要
     * @return 有效恢复码，不存在时为空
     */
    PlatformRecoveryCodeEntity selectActiveForUpdate(
            @Param("platformIdentityId") long platformIdentityId,
            @Param("codeHash") String codeHash
    );

    /**
     * 乐观锁消费一次性恢复码。
     *
     * @param id 恢复码主键 ID
     * @param version 乐观锁版本
     * @return 更新行数
     */
    int consumeRecoveryCode(@Param("id") long id, @Param("version") long version);
}
