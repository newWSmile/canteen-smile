package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 登录设备的受控客户端描述；登录 IP 必须由服务端获取。 */
@Getter
@Setter
@NoArgsConstructor
public class DeviceRequest {

    /** 客户端生成并持久保存的稳定设备标识。 */
    @NotBlank
    @Size(max = 128)
    private String deviceId;

    /** 设备类型编码。 */
    @NotBlank
    @Size(max = 64)
    private String deviceType;

    /** 用户可识别的设备名称。 */
    @NotBlank
    @Size(max = 128)
    private String deviceName;

    /** 经客户端概括且不含秘密的 User-Agent 摘要。 */
    @Size(max = 256)
    private String userAgentSummary;
}
