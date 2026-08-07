package com.canteen.smile.modules.organization.vo;

import java.util.List;

/**
 * 已发布的平台机构类型模板版本。
 *
 * @param templateVersion 模板版本号
 * @param status 模板发布状态
 * @param types 机构类型
 * @param relations 允许的父子关系
 */
public record OrgTypeTemplateVO(
        long templateVersion,
        String status,
        List<TypeItem> types,
        List<RelationItem> relations
) {
    /** @param typeCode 类型编码 @param name 类型名称 @param sortOrder 显示顺序 */
    public record TypeItem(String typeCode, String name, int sortOrder) {
    }

    /** @param parentTypeCode 父类型编码 @param childTypeCode 子类型编码 */
    public record RelationItem(String parentTypeCode, String childTypeCode) {
    }
}
