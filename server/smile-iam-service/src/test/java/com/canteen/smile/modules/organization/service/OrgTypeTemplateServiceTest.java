package com.canteen.smile.modules.organization.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.organization.dto.OrgTypeTemplateItemRequest;
import com.canteen.smile.modules.organization.dto.OrgTypeTemplateRelationRequest;
import com.canteen.smile.modules.organization.dto.PublishOrgTypeTemplateRequest;
import com.canteen.smile.modules.organization.mapper.OrgTypeTemplateMapper;
import com.canteen.smile.modules.organization.vo.OrgTypeTemplateVO;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 机构类型模板整图校验和发布服务测试。 */
class OrgTypeTemplateServiceTest {

    /** 验证合法 DAG 会分配新版本并整版发布。 */
    @Test
    void shouldPublishAcyclicTemplate() {
        /** 模板 Mapper 替身。 */
        OrgTypeTemplateMapper mapper = mock(OrgTypeTemplateMapper.class);
        /** 平台操作者服务替身。 */
        PlatformActorService actorService = mock(PlatformActorService.class);
        when(actorService.currentPlatformIdentityId()).thenReturn(7L);
        when(mapper.nextTemplateVersion()).thenReturn(3L);
        /** 被测服务。 */
        OrgTypeTemplateService service = new OrgTypeTemplateService(mapper, actorService);
        /** 合法模板请求。 */
        PublishOrgTypeTemplateRequest request = request(List.of(
                new OrgTypeTemplateRelationRequest("CITY", "DISTRICT"),
                new OrgTypeTemplateRelationRequest("CITY", "SCHOOL")
        ));

        OrgTypeTemplateVO result = service.publish(request);

        assertThat(result.templateVersion()).isEqualTo(3L);
        verify(mapper).lockTemplateVersionAllocation();
        verify(mapper).insertPublishedTypes(3L, request.types(), 7L);
        verify(mapper).insertRelations(3L, request.relations(), 7L);
    }

    /** 验证关系图存在环时拒绝发布且不分配版本。 */
    @Test
    void shouldRejectCyclicTemplate() {
        /** 模板 Mapper 替身。 */
        OrgTypeTemplateMapper mapper = mock(OrgTypeTemplateMapper.class);
        /** 平台操作者服务替身。 */
        PlatformActorService actorService = mock(PlatformActorService.class);
        /** 被测服务。 */
        OrgTypeTemplateService service = new OrgTypeTemplateService(mapper, actorService);
        /** 存在 CITY 与 DISTRICT 环的请求。 */
        PublishOrgTypeTemplateRequest request = request(List.of(
                new OrgTypeTemplateRelationRequest("CITY", "DISTRICT"),
                new OrgTypeTemplateRelationRequest("DISTRICT", "CITY")
        ));

        assertThatThrownBy(() -> service.publish(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("机构类型关系存在环");
    }

    /** 创建包含三个类型的测试模板。 */
    private PublishOrgTypeTemplateRequest request(List<OrgTypeTemplateRelationRequest> relations) {
        return new PublishOrgTypeTemplateRequest(List.of(
                new OrgTypeTemplateItemRequest("CITY", "市", 10),
                new OrgTypeTemplateItemRequest("DISTRICT", "区县", 20),
                new OrgTypeTemplateItemRequest("SCHOOL", "学校", 30)
        ), relations);
    }
}
