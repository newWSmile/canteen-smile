package com.canteen.smile.modules.platform.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.platform.converter.PlatformIdentityConverter;
import com.canteen.smile.modules.platform.dto.BootstrapPlatformIdentityRequest;
import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.entity.UsernameRegistryEntity;
import com.canteen.smile.modules.platform.mapper.PlatformIdentityMapper;
import com.canteen.smile.modules.platform.model.PlatformIdentityStatus;
import com.canteen.smile.modules.platform.vo.PlatformIdentityInternalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 首位平台身份创建和激活事务服务。 */
@Service
@RequiredArgsConstructor
public class PlatformIdentityBootstrapService {

    /** 用户名已被永久占用错误码。 */
    private static final String USERNAME_RESERVED_CODE = "IAM_2001";

    /** 首位平台身份引导已关闭错误码。 */
    private static final String BOOTSTRAP_CLOSED_CODE = "IAM_2013";

    /** 身份当前不可用错误码。 */
    private static final String IDENTITY_UNAVAILABLE_CODE = "IAM_2011";

    /** 平台身份数据访问接口。 */
    private final PlatformIdentityMapper platformIdentityMapper;

    /** 平台身份对象转换器。 */
    private final PlatformIdentityConverter platformIdentityConverter;

    /**
     * 幂等创建首位平台身份和永久用户名注册记录。
     *
     * @param request 首位平台身份请求
     * @return 初始化中或已经激活的平台身份
     */
    @Transactional
    public PlatformIdentityInternalVO bootstrap(BootstrapPlatformIdentityRequest request) {
        /** 服务端归一化用户名。 */
        String normalizedUsername = UsernameNormalizer.normalize(request.username());
        /** 同名平台身份。 */
        PlatformIdentityEntity existing = platformIdentityMapper.selectByNormalizedUsername(normalizedUsername);
        /** 当前平台身份总数。 */
        long identityCount = platformIdentityMapper.countPlatformIdentities();
        if (identityCount > 0) {
            if (existing != null) {
                return platformIdentityConverter.toInternalVo(existing);
            }
            throw new BusinessException(BOOTSTRAP_CLOSED_CODE, "首位平台管理员引导已经关闭", 409);
        }
        if (platformIdentityMapper.countReservedUsername(normalizedUsername) > 0) {
            throw new BusinessException(USERNAME_RESERVED_CODE, "用户名已被占用", 409);
        }

        /** 待写入的平台身份。 */
        PlatformIdentityEntity identity = new PlatformIdentityEntity();
        identity.setUsername(request.username().strip());
        identity.setNormalizedUsername(normalizedUsername);
        identity.setDisplayName(normalizeDisplayName(request.displayName(), identity.getUsername()));
        identity.setStatus(PlatformIdentityStatus.INITIALIZING.name());
        identity.setAuthzVersion(0L);
        platformIdentityMapper.insertPlatformIdentity(identity);

        /** 永久用户名注册记录。 */
        UsernameRegistryEntity registry = new UsernameRegistryEntity();
        registry.setNormalizedUsername(normalizedUsername);
        registry.setSubjectType("PLATFORM_IDENTITY");
        registry.setSubjectId(identity.getId());
        registry.setOriginalUsername(identity.getUsername());
        registry.setLoginEnabled(true);
        platformIdentityMapper.insertUsernameRegistry(registry);
        return platformIdentityConverter.toInternalVo(platformIdentityMapper.selectById(identity.getId()));
    }

    /**
     * Auth 凭证创建成功后幂等激活平台身份。
     *
     * @param identityId 平台身份 ID
     * @return 激活后的平台身份
     */
    @Transactional
    public PlatformIdentityInternalVO activate(long identityId) {
        /** 当前平台身份。 */
        PlatformIdentityEntity identity = platformIdentityMapper.selectById(identityId);
        if (identity == null || Boolean.TRUE.equals(identity.getDeleted())) {
            throw new BusinessException(IDENTITY_UNAVAILABLE_CODE, "平台身份当前不可用", 409);
        }
        if (PlatformIdentityStatus.ACTIVE.name().equals(identity.getStatus())) {
            return platformIdentityConverter.toInternalVo(identity);
        }
        if (!PlatformIdentityStatus.INITIALIZING.name().equals(identity.getStatus())
                || platformIdentityMapper.activatePlatformIdentity(identityId, identity.getVersion()) != 1) {
            throw new BusinessException(IDENTITY_UNAVAILABLE_CODE, "平台身份当前不可用", 409);
        }
        return platformIdentityConverter.toInternalVo(platformIdentityMapper.selectById(identityId));
    }

    /**
     * 规范化可选显示名称。
     *
     * @param displayName 可选显示名称
     * @param username 默认用户名
     * @return 最终显示名称
     */
    private String normalizeDisplayName(String displayName, String username) {
        return displayName == null || displayName.isBlank() ? username : displayName.strip();
    }
}
