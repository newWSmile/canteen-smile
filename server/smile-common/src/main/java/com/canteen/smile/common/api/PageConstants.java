package com.canteen.smile.common.api;

/** 列表接口统一分页边界。 */
public final class PageConstants {

    /** 默认页码，从 1 开始。 */
    public static final int DEFAULT_PAGE_NO = 1;

    /** 默认每页数量。 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 普通分页允许的最大每页数量。 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 禁止实例化分页常量类。 */
    private PageConstants() {
    }
}
