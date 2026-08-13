package com.canteen.smile.modules.navigation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 修改当前账号个人菜单偏好的命令。
 *
 * @param hidden 是否仅对本人隐藏
 * @param version 当前偏好版本；尚无记录时为零
 */
public record UpdateMenuPreferenceRequest(
        @NotNull Boolean hidden,
        @NotNull @Min(0) Long version
) { }
