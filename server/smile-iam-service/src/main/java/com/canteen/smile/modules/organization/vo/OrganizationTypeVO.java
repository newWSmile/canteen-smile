package com.canteen.smile.modules.organization.vo;

/**
 * 租户机构类型响应。
 *
 * @param id 类型 ID
 * @param typeCode 永久类型编码
 * @param name 类型名称
 * @param sortOrder 显示排序
 * @param status 类型状态
 * @param sourceTemplateVersion 来源模板版本
 * @param version 乐观锁版本
 */
public record OrganizationTypeVO(
        String id,
        String typeCode,
        String name,
        int sortOrder,
        String status,
        Long sourceTemplateVersion,
        long version
) {
}
