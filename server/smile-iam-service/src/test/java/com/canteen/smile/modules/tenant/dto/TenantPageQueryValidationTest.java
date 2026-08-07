package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户分页查询参数校验测试。 */
class TenantPageQueryValidationTest {

    /** 验证默认分页条件符合统一分页规范。 */
    @Test
    void shouldAcceptDefaultQuery() {
        /** 使用默认值的分页查询。 */
        TenantPageQuery query = new TenantPageQuery();

        /** 参数校验结果。 */
        Set<ConstraintViolation<TenantPageQuery>> violations = validate(query);

        assertThat(violations).isEmpty();
        assertThat(query.getPageNo()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(20);
    }

    /** 验证超过最大页大小和未知状态会被拒绝。 */
    @Test
    void shouldRejectInvalidPageSizeAndStatus() {
        /** 包含非法条件的分页查询。 */
        TenantPageQuery query = new TenantPageQuery();
        query.setPageSize(101);
        query.setStatus("UNKNOWN");

        /** 参数校验结果。 */
        Set<ConstraintViolation<TenantPageQuery>> violations = validate(query);

        assertThat(violations).hasSize(2);
    }

    /**
     * 使用 Jakarta Validation 校验查询对象。
     *
     * @param query 待校验查询对象
     * @return 约束违反集合
     */
    private Set<ConstraintViolation<TenantPageQuery>> validate(TenantPageQuery query) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            /** Jakarta Validation 校验器。 */
            Validator validator = factory.getValidator();
            return validator.validate(query);
        }
    }
}
