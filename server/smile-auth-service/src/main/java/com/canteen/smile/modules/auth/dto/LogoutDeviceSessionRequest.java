package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** @param version 设备会话乐观锁版本。 */
public record LogoutDeviceSessionRequest(@NotNull @Min(0) Long version) {
}
