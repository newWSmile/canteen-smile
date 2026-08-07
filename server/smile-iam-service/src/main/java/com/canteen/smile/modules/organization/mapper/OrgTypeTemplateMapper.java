package com.canteen.smile.modules.organization.mapper;

import com.canteen.smile.modules.organization.dto.OrgTypeTemplateItemRequest;
import com.canteen.smile.modules.organization.dto.OrgTypeTemplateRelationRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 平台机构类型模板数据访问接口。 */
public interface OrgTypeTemplateMapper {

    /** 串行化模板版本分配，防止并发发布得到相同版本。 */
    void lockTemplateVersionAllocation();

    /** @return 下一个单调递增模板版本 */
    long nextTemplateVersion();

    /** @return 新增的模板类型行数 */
    int insertPublishedTypes(@Param("templateVersion") long templateVersion,
                             @Param("types") List<OrgTypeTemplateItemRequest> types,
                             @Param("operatorId") long operatorId);

    /** @return 新增的模板关系行数 */
    int insertRelations(@Param("templateVersion") long templateVersion,
                        @Param("relations") List<OrgTypeTemplateRelationRequest> relations,
                        @Param("operatorId") long operatorId);

    /** @return 所有已发布模板类型行 */
    List<PublishedTypeRow> selectPublishedTypes();

    /** @return 所有已发布模板关系行 */
    List<PublishedRelationRow> selectPublishedRelations();

    /** @return 指定模板版本有效类型数量 */
    long countPublishedTypes(@Param("templateVersion") long templateVersion);

    /** @return 指定模板版本中的类型数量 */
    long countPublishedType(@Param("templateVersion") long templateVersion, @Param("typeCode") String typeCode);

    /** 已发布模板类型数据行。 */
    record PublishedTypeRow(long templateVersion, String typeCode, String name, int sortOrder) {
    }

    /** 已发布模板关系数据行。 */
    record PublishedRelationRow(long templateVersion, String parentTypeCode, String childTypeCode) {
    }
}
