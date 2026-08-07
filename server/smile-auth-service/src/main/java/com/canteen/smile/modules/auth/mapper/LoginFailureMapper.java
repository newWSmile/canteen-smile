package com.canteen.smile.modules.auth.mapper;

import com.canteen.smile.modules.auth.entity.LoginFailureEntity;
import org.apache.ibatis.annotations.Param;

/** 密码登录失败计数和锁定状态数据访问接口。 */
public interface LoginFailureMapper {

    /**
     * 查询当前登录主体失败状态。
     *
     * @param subjectKeyHash 登录主体组合摘要
     * @return 失败状态，不存在时为空
     */
    LoginFailureEntity selectBySubjectKeyHash(@Param("subjectKeyHash") String subjectKeyHash);

    /**
     * 原子新增一次密码失败并在第三次/第五次设置安全门槛。
     *
     * @param subjectKeyHash 登录主体组合摘要
     * @param ipHash 来源 IP 摘要
     * @param deviceHash 设备标识摘要
     * @return 影响行数
     */
    int recordPasswordFailure(
            @Param("subjectKeyHash") String subjectKeyHash,
            @Param("ipHash") String ipHash,
            @Param("deviceHash") String deviceHash
    );

    /**
     * 成功登录后清除连续失败状态。
     *
     * @param subjectKeyHash 登录主体组合摘要
     * @return 影响行数
     */
    int resetAfterSuccess(@Param("subjectKeyHash") String subjectKeyHash);
}
