package com.canteen.smile.modules.audit.mapper;

import com.canteen.smile.modules.audit.entity.IamAuditLogEntity;

/** IAM 管理操作审计数据访问接口。 */
public interface IamAuditLogMapper {

    /** @param entity 已脱敏审计实体 @return 新增行数 */
    int insert(IamAuditLogEntity entity);
}
