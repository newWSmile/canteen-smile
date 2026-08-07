package com.canteen.smile.modules.organization.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.organization.dto.OrgTypeTemplateItemRequest;
import com.canteen.smile.modules.organization.dto.OrgTypeTemplateRelationRequest;
import com.canteen.smile.modules.organization.dto.PublishOrgTypeTemplateRequest;
import com.canteen.smile.modules.organization.mapper.OrgTypeTemplateMapper;
import com.canteen.smile.modules.organization.vo.OrgTypeTemplateVO;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 平台机构类型模板整版发布和查询服务。 */
@Service
@RequiredArgsConstructor
public class OrgTypeTemplateService {

    /** 模板结构不合法错误码。 */
    private static final String INVALID_TEMPLATE_CODE = "IAM_2101";

    /** 机构类型模板数据访问接口。 */
    private final OrgTypeTemplateMapper mapper;

    /** 当前平台操作者解析服务。 */
    private final PlatformActorService platformActorService;

    /**
     * 校验并发布新的不可变模板版本。
     *
     * @param request 完整模板
     * @return 已发布模板
     */
    @Transactional
    public OrgTypeTemplateVO publish(PublishOrgTypeTemplateRequest request) {
        validate(request);
        /** 当前平台身份 ID。 */
        long operatorId = platformActorService.currentPlatformIdentityId();
        mapper.lockTemplateVersionAllocation();
        /** 新模板版本。 */
        long templateVersion = mapper.nextTemplateVersion();
        mapper.insertPublishedTypes(templateVersion, request.types(), operatorId);
        if (!request.relations().isEmpty()) {
            mapper.insertRelations(templateVersion, request.relations(), operatorId);
        }
        return toVO(templateVersion, request.types(), request.relations());
    }

    /** @return 已发布模板版本，按版本倒序 */
    @Transactional(readOnly = true)
    public List<OrgTypeTemplateVO> listPublished() {
        /** 按版本聚合的类型集合。 */
        Map<Long, List<OrgTypeTemplateVO.TypeItem>> types = new LinkedHashMap<>();
        for (OrgTypeTemplateMapper.PublishedTypeRow row : mapper.selectPublishedTypes()) {
            types.computeIfAbsent(row.templateVersion(), ignored -> new ArrayList<>())
                    .add(new OrgTypeTemplateVO.TypeItem(row.typeCode(), row.name(), row.sortOrder()));
        }
        /** 按版本聚合的关系集合。 */
        Map<Long, List<OrgTypeTemplateVO.RelationItem>> relations = new HashMap<>();
        for (OrgTypeTemplateMapper.PublishedRelationRow row : mapper.selectPublishedRelations()) {
            relations.computeIfAbsent(row.templateVersion(), ignored -> new ArrayList<>())
                    .add(new OrgTypeTemplateVO.RelationItem(row.parentTypeCode(), row.childTypeCode()));
        }
        return types.entrySet().stream()
                .map(entry -> new OrgTypeTemplateVO(entry.getKey(), "PUBLISHED", List.copyOf(entry.getValue()),
                        List.copyOf(relations.getOrDefault(entry.getKey(), List.of()))))
                .toList();
    }

    /** 校验编码唯一、关系引用、自环和整图无环。 */
    private void validate(PublishOrgTypeTemplateRequest request) {
        /** 模板内全部类型编码。 */
        Set<String> typeCodes = new HashSet<>();
        for (OrgTypeTemplateItemRequest type : request.types()) {
            if (!typeCodes.add(type.typeCode())) {
                throw invalid("机构类型编码重复");
            }
        }
        /** 去重后的父子关系键。 */
        Set<String> relationKeys = new HashSet<>();
        /** 每个父类型的直接子类型。 */
        Map<String, Set<String>> graph = new HashMap<>();
        /** 每个类型节点的入度。 */
        Map<String, Integer> indegree = new HashMap<>();
        typeCodes.forEach(code -> indegree.put(code, 0));
        for (OrgTypeTemplateRelationRequest relation : request.relations()) {
            if (!typeCodes.contains(relation.parentTypeCode()) || !typeCodes.contains(relation.childTypeCode())) {
                throw invalid("机构类型关系引用了不存在的编码");
            }
            if (relation.parentTypeCode().equals(relation.childTypeCode())) {
                throw invalid("机构类型不能成为自身的下级");
            }
            /** 当前关系唯一键。 */
            String relationKey = relation.parentTypeCode() + "\n" + relation.childTypeCode();
            if (!relationKeys.add(relationKey)) {
                throw invalid("机构类型关系重复");
            }
            graph.computeIfAbsent(relation.parentTypeCode(), ignored -> new HashSet<>())
                    .add(relation.childTypeCode());
            indegree.compute(relation.childTypeCode(), (ignored, value) -> value == null ? 1 : value + 1);
        }
        /** 当前全部零入度节点。 */
        ArrayDeque<String> queue = new ArrayDeque<>();
        indegree.forEach((code, degree) -> {
            if (degree == 0) {
                queue.add(code);
            }
        });
        /** 拓扑排序已访问节点数。 */
        int visited = 0;
        while (!queue.isEmpty()) {
            /** 当前拓扑节点。 */
            String parent = queue.remove();
            visited++;
            for (String child : graph.getOrDefault(parent, Set.of())) {
                /** 子节点扣减后的入度。 */
                int nextDegree = indegree.computeIfPresent(child, (ignored, value) -> value - 1);
                if (nextDegree == 0) {
                    queue.add(child);
                }
            }
        }
        if (visited != typeCodes.size()) {
            throw invalid("机构类型关系存在环");
        }
    }

    /** 将刚发布请求转换为响应。 */
    private OrgTypeTemplateVO toVO(long templateVersion, List<OrgTypeTemplateItemRequest> types,
                                   List<OrgTypeTemplateRelationRequest> relations) {
        return new OrgTypeTemplateVO(templateVersion, "PUBLISHED",
                types.stream().map(type -> new OrgTypeTemplateVO.TypeItem(
                        type.typeCode(), type.name(), type.sortOrder())).toList(),
                relations.stream().map(relation -> new OrgTypeTemplateVO.RelationItem(
                        relation.parentTypeCode(), relation.childTypeCode())).toList());
    }

    /** @return 模板校验业务异常 */
    private BusinessException invalid(String message) {
        return new BusinessException(INVALID_TEMPLATE_CODE, message, 400);
    }
}
