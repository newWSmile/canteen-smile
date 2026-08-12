package com.canteen.smile.modules.tenant.mapper;

import com.canteen.smile.modules.tenant.dto.UpdateTenantSecurityPolicyRequest;
import com.canteen.smile.modules.tenant.entity.TenantSecurityPolicyEntity;
import org.apache.ibatis.annotations.Param;

/** 租户安全策略及收紧策略会话失效事件的数据访问接口。 */
public interface TenantSecurityPolicyMapper {

    /** @return 指定租户当前有效安全策略。 */
    TenantSecurityPolicyEntity selectByTenantId(@Param("tenantId") long tenantId);

    /** @return 指定租户当前安全版本。 */
    Long selectTenantSecurityVersion(@Param("tenantId") long tenantId);

    /** @return 乐观锁修改成功的行数。 */
    int updatePolicy(@Param("tenantId") long tenantId,
                     @Param("request") UpdateTenantSecurityPolicyRequest request,
                     @Param("operatorId") long operatorId);

    /** @return 提升租户安全版本的行数。 */
    int bumpTenantSecurityVersion(@Param("tenantId") long tenantId,
                                  @Param("operatorId") long operatorId);

    /** @return 为租户账号批量生成会话失效事件的行数。 */
    int insertSecurityPolicyChangedEvents(@Param("tenantId") long tenantId,
                                          @Param("securityVersion") long securityVersion,
                                          @Param("operatorId") long operatorId,
                                          @Param("ipAddress") String ipAddress);
}
