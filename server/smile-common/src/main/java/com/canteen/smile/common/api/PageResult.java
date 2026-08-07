package com.canteen.smile.common.api;

import java.util.List;
import java.util.Objects;

/**
 * 列表接口统一分页响应。
 *
 * @param items 当前页数据
 * @param pageNo 当前页码，从 1 开始
 * @param pageSize 当前每页数量
 * @param total 满足条件的数据总量
 * @param <T> 列表元素类型
 */
public record PageResult<T>(
        List<T> items,
        int pageNo,
        int pageSize,
        long total
) {

    /** 校验分页响应边界，并创建不可变的当前页数据副本。 */
    public PageResult {
        Objects.requireNonNull(items, "items must not be null");
        if (pageNo < PageConstants.DEFAULT_PAGE_NO) {
            throw new IllegalArgumentException("pageNo must be greater than or equal to 1");
        }
        if (pageSize < 1 || pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        items = List.copyOf(items);
    }
}
