package com.canteen.smile.modules.auth.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 密码历史摘要数据访问接口。 */
public interface PasswordHistoryMapper {

    /**
     * 查询最近的历史密码摘要。
     *
     * @param subjectType 认证主体类型
     * @param subjectId 认证主体 ID
     * @param limit 最大返回数量
     * @return 按变更时间倒序的密码摘要
     */
    List<String> selectRecentHashes(
            @Param("subjectType") String subjectType,
            @Param("subjectId") long subjectId,
            @Param("limit") int limit
    );

    /**
     * 将当前凭证密码摘要写入历史表。
     *
     * @param subjectType 认证主体类型
     * @param subjectId 认证主体 ID
     * @return 新增行数
     */
    int insertCurrentCredential(
            @Param("subjectType") String subjectType,
            @Param("subjectId") long subjectId
    );
}
