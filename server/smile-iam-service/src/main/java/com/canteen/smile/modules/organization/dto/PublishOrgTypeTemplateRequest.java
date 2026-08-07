package com.canteen.smile.modules.organization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发布不可变平台机构类型模板版本的请求。
 *
 * @param types 当前版本全部机构类型
 * @param relations 当前版本全部允许关系
 */
public record PublishOrgTypeTemplateRequest(
        @NotEmpty @Size(max = 50) List<@Valid OrgTypeTemplateItemRequest> types,
        @Size(max = 500) List<@Valid OrgTypeTemplateRelationRequest> relations
) {
    /** 将可选关系集合规范为空集合。 */
    public PublishOrgTypeTemplateRequest {
        relations = relations == null ? List.of() : List.copyOf(relations);
        types = types == null ? List.of() : List.copyOf(types);
    }
}
