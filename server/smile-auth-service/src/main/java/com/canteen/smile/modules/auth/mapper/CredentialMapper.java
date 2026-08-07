package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.CredentialEntity;
import org.apache.ibatis.annotations.Param;

/** 平台和租户认证凭证数据访问接口。 */
public interface CredentialMapper {

    /** @return 有效平台身份凭证数量 */
    long countPlatformCredentials();

    /**
     * 查询指定认证主体凭证。
     *
     * @param subjectType 认证主体类型
     * @param subjectId 认证主体 ID
     * @return 凭证实体，不存在时为空
     */
    CredentialEntity selectBySubject(
            @Param("subjectType") String subjectType,
            @Param("subjectId") long subjectId
    );

    /**
     * 新增已经设置 Argon2id 密码的平台凭证。
     *
     * @param entity 凭证实体
     * @return 新增行数
     */
    int insertCredential(CredentialEntity entity);

    /**
     * 幂等创建待激活租户账号凭证容器。
     *
     * @param accountId IAM 租户账号 ID
     * @return 实际新增行数
     */
    int insertPendingTenantAccountCredential(@Param("accountId") long accountId);

    /**
     * 为待激活租户账号写入 Argon2id 密码摘要并激活凭证。
     *
     * @param accountId IAM 租户账号 ID
     * @param passwordHash Argon2id 密码摘要
     * @return 更新行数
     */
    int activatePendingTenantAccountCredential(
            @Param("accountId") long accountId,
            @Param("passwordHash") String passwordHash
    );

    /** @param accountId 租户账号 ID @return 进入待重置状态的行数 */
    int markTenantAccountResetRequired(@Param("accountId") long accountId);

    /**
     * 写入新密码摘要并恢复有效凭证状态。
     *
     * @param accountId 租户账号 ID
     * @param passwordHash 新 Argon2id 密码摘要
     * @return 更新行数
     */
    int completeTenantAccountPasswordReset(
            @Param("accountId") long accountId,
            @Param("passwordHash") String passwordHash
    );
}
