package com.canteen.smile.modules.platform.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** `iam_username_registry` 全平台用户名永久保留表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class UsernameRegistryEntity {

    /** 注册表主键 ID。 */
    private Long id;

    /** 归一化后的全局唯一用户名。 */
    private String normalizedUsername;

    /** 用户名所属身份类型。 */
    private String subjectType;

    /** 用户名所属身份 ID。 */
    private Long subjectId;

    /** 用户录入的原始用户名。 */
    private String originalUsername;

    /** 当前用户名是否允许登录。 */
    private Boolean loginEnabled;
}
