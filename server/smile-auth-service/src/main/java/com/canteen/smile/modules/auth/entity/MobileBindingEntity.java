package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_mobile_binding` 已验证手机号绑定实体，只映射 Auth 自有数据库表。 */
@Getter
@Setter
@NoArgsConstructor
public class MobileBindingEntity {

    /** 手机号绑定记录主键 ID。 */
    private Long id;

    /** 认证主体类型。 */
    private String subjectType;

    /** 认证主体 ID。 */
    private Long subjectId;

    /** 包含格式版本、随机 IV 和 AES-GCM 密文的二进制载荷。 */
    private byte[] mobileCiphertext;

    /** 带服务端 Pepper 的手机号 HMAC 查询摘要。 */
    private String mobileHash;

    /** 只用于界面展示的脱敏手机号。 */
    private String maskedMobile;

    /** 加密密钥版本标识。 */
    private String encryptionKeyId;

    /** 手机号绑定状态。 */
    private String status;

    /** 手机号通过验证码验证的时间。 */
    private OffsetDateTime verifiedTime;

    /** 旧绑定被换绑替代的时间。 */
    private OffsetDateTime replacedTime;

    /** 乐观锁版本。 */
    private Long version;
}
