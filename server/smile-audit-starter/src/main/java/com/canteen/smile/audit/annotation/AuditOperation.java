package com.canteen.smile.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 声明公共 Service 方法需要生成一条通用异步审计事件。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditOperation {

    /** @return 审计来源服务或业务域稳定编码 */
    String source();

    /** @return 与菜单、权限和路由无关的任意长度中文分类路径快照 */
    String[] categoryPath() default {};

    /** @return 永久稳定且不得改义复用的动作编码 */
    String actionCode();

    /** @return 事件发生时写入的中文动作名称 */
    String actionName();

    /** @return 被操作目标类型；支持固定文本，不执行隐式推断 */
    String targetType();

    /** @return 目标 ID 的 SpEL 或固定文本；可引用参数、result 和 actor */
    String targetId();

    /** @return 目标中文名称的 SpEL 或固定文本 */
    String targetName() default "";

    /** @return 目标业务编码或用户名的 SpEL 或固定文本 */
    String targetCode() default "";

    /** @return 操作原因的 SpEL 或固定文本 */
    String reason() default "";

    /** @return 脱敏手机号的 SpEL；禁止引用完整手机号 */
    String maskedMobile() default "";

    /** @return 是否记录业务拒绝和执行失败 */
    boolean recordFailure() default true;
}
