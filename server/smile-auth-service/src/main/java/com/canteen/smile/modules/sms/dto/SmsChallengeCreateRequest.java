package com.canteen.smile.modules.sms.dto;

import com.canteen.smile.modules.sms.model.SmsPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 创建短信验证码挑战的匿名请求；响应不得泄露手机号绑定状态。 */
@Getter
@Setter
@NoArgsConstructor
public class SmsChallengeCreateRequest {

    /** 已由数据库约束确认的短信业务用途。 */
    @NotNull
    private SmsPurpose purpose;

    /** 接收验证码的完整手机号，仅在 Auth 当前调用链内使用。 */
    @NotBlank
    @Size(max = 32)
    private String mobile;

    /** 客户端持久化的稳定设备标识，用于设备维度限流。 */
    @NotBlank
    @Size(max = 128)
    private String deviceId;

    /** 达到安全门槛后使用的图形验证码票据；图形验证码能力接入前允许为空。 */
    @Size(max = 256)
    private String captchaTicket;
}
