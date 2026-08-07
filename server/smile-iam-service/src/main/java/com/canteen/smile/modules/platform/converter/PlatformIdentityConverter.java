package com.canteen.smile.modules.platform.converter;

import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.model.PlatformIdentityStatus;
import com.canteen.smile.modules.platform.vo.PlatformIdentityInternalVO;
import org.springframework.stereotype.Component;

/** 平台身份 Entity 与内部 VO 的显式转换器。 */
@Component
public class PlatformIdentityConverter {

    /**
     * 转换平台身份内部契约。
     *
     * @param entity 平台身份实体
     * @return 平台身份内部 VO
     */
    public PlatformIdentityInternalVO toInternalVo(PlatformIdentityEntity entity) {
        return new PlatformIdentityInternalVO(
                entity.getId().toString(),
                entity.getUsername(),
                entity.getDisplayName() == null ? entity.getUsername() : entity.getDisplayName(),
                PlatformIdentityStatus.valueOf(entity.getStatus()),
                entity.getAuthzVersion(),
                entity.getVersion()
        );
    }
}
