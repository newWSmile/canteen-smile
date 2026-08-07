package com.canteen.smile.modules.platform.service;

import com.canteen.smile.modules.platform.dto.UsernameLoginResolutionRequest;
import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.mapper.PlatformIdentityMapper;
import com.canteen.smile.modules.platform.model.PlatformIdentityStatus;
import com.canteen.smile.modules.platform.vo.UsernameLoginResolutionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台用户名登录主体解析服务。 */
@Service
@RequiredArgsConstructor
public class PlatformLoginResolutionService {

    /** 平台身份数据访问接口。 */
    private final PlatformIdentityMapper platformIdentityMapper;

    /**
     * 解析平台管理端用户名；其它入口暂不伪造租户账号解析。
     *
     * @param request 用户名和应用入口
     * @return 内部登录解析结果
     */
    @Transactional(readOnly = true)
    public UsernameLoginResolutionVO resolve(UsernameLoginResolutionRequest request) {
        if (!"PLATFORM_ADMIN".equals(request.appCode())) {
            return UsernameLoginResolutionVO.unresolved();
        }
        /** 按统一规则归一化后的用户名。 */
        String normalizedUsername = UsernameNormalizer.normalize(request.username());
        /** IAM 平台身份。 */
        PlatformIdentityEntity identity = platformIdentityMapper.selectByNormalizedUsername(normalizedUsername);
        if (identity == null
                || Boolean.TRUE.equals(identity.getDeleted())
                || !PlatformIdentityStatus.ACTIVE.name().equals(identity.getStatus())) {
            return UsernameLoginResolutionVO.unresolved();
        }
        return new UsernameLoginResolutionVO(
                true,
                "PLATFORM_IDENTITY",
                identity.getId().toString(),
                identity.getUsername(),
                identity.getDisplayName() == null ? identity.getUsername() : identity.getDisplayName(),
                identity.getStatus(),
                identity.getAuthzVersion()
        );
    }
}
