package com.canteen.smile.common.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 统一分页响应单元测试。 */
class PageResultTest {

    /** 验证分页数据会被复制为不可变列表。 */
    @Test
    void shouldCreateImmutablePageResult() {
        /** 用于验证防御性复制的可变源列表。 */
        List<String> sourceItems = new ArrayList<>(List.of("first"));

        /** 已创建的统一分页响应。 */
        PageResult<String> result = new PageResult<>(sourceItems, 1, 20, 1);
        sourceItems.add("second");

        assertThat(result.items()).containsExactly("first");
        assertThatThrownBy(() -> result.items().add("third"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /** 验证每页数量不能超过项目统一上限。 */
    @Test
    void shouldRejectPageSizeAboveMaximum() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), 1, 101, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");
    }
}
