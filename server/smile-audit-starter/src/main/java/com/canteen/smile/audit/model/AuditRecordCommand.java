package com.canteen.smile.audit.model;

import java.util.List;

/**
 * 编程式审计入口使用的不可变事件声明，不包含执行结果、失败码和运行时链路字段。
 *
 * @param source 来源服务或业务域稳定编码
 * @param categoryPath 任意层级中文分类路径快照
 * @param actionCode 永久稳定且不得改义复用的动作编码
 * @param actionName 事件发生时的中文动作名称
 * @param targetType 被操作目标类型
 * @param targetId 被操作目标唯一标识
 * @param targetName 被操作目标中文名称快照
 * @param targetCode 被操作目标业务编码快照
 * @param reason 可选操作原因
 * @param maskedMobile 可选脱敏手机号
 * @param loginMethod 可选登录或再认证方式
 * @param deviceSummary 可选脱敏设备摘要
 * @param actor 后端已确认的操作人不可变快照
 */
public record AuditRecordCommand(
        String source,
        List<String> categoryPath,
        String actionCode,
        String actionName,
        String targetType,
        String targetId,
        String targetName,
        String targetCode,
        String reason,
        String maskedMobile,
        String loginMethod,
        String deviceSummary,
        AuditActor actor
) {

    /** @return 新的编程式审计声明构建器 */
    public static Builder builder() {
        return new Builder();
    }

    /** 仅负责组装审计声明，不在构建阶段读取认证上下文或发布事件。 */
    public static final class Builder {

        /** 来源服务或业务域稳定编码。 */
        private String source;

        /** 任意层级中文分类路径快照。 */
        private List<String> categoryPath = List.of();

        /** 永久稳定的动作编码。 */
        private String actionCode;

        /** 中文动作名称。 */
        private String actionName;

        /** 被操作目标类型。 */
        private String targetType;

        /** 被操作目标唯一标识。 */
        private String targetId;

        /** 被操作目标中文名称快照。 */
        private String targetName;

        /** 被操作目标业务编码快照。 */
        private String targetCode;

        /** 可选操作原因。 */
        private String reason;

        /** 可选脱敏手机号。 */
        private String maskedMobile;

        /** 可选登录或再认证方式。 */
        private String loginMethod;

        /** 可选脱敏设备摘要。 */
        private String deviceSummary;

        /** 后端已确认的操作人快照。 */
        private AuditActor actor;

        /** @param value 来源编码 @return 当前构建器 */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /** @param values 中文分类路径 @return 当前构建器 */
        public Builder categoryPath(String... values) {
            this.categoryPath = values == null
                    ? List.of()
                    : java.util.Arrays.stream(values)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::strip)
                    .toList();
            return this;
        }

        /** @param value 动作编码 @return 当前构建器 */
        public Builder actionCode(String value) {
            this.actionCode = value;
            return this;
        }

        /** @param value 中文动作名称 @return 当前构建器 */
        public Builder actionName(String value) {
            this.actionName = value;
            return this;
        }

        /** @param value 目标类型 @return 当前构建器 */
        public Builder targetType(String value) {
            this.targetType = value;
            return this;
        }

        /** @param value 目标唯一标识 @return 当前构建器 */
        public Builder targetId(Object value) {
            this.targetId = value == null ? null : String.valueOf(value);
            return this;
        }

        /** @param value 目标中文名称 @return 当前构建器 */
        public Builder targetName(String value) {
            this.targetName = value;
            return this;
        }

        /** @param value 目标业务编码 @return 当前构建器 */
        public Builder targetCode(String value) {
            this.targetCode = value;
            return this;
        }

        /** @param value 操作原因 @return 当前构建器 */
        public Builder reason(String value) {
            this.reason = value;
            return this;
        }

        /** @param value 脱敏手机号 @return 当前构建器 */
        public Builder maskedMobile(String value) {
            this.maskedMobile = value;
            return this;
        }

        /** @param value 登录或再认证方式 @return 当前构建器 */
        public Builder loginMethod(String value) {
            this.loginMethod = value;
            return this;
        }

        /** @param value 脱敏设备摘要 @return 当前构建器 */
        public Builder deviceSummary(String value) {
            this.deviceSummary = value;
            return this;
        }

        /** @param value 后端已确认的操作人快照 @return 当前构建器 */
        public Builder actor(AuditActor value) {
            this.actor = value;
            return this;
        }

        /** @return 不可变编程式审计声明 */
        public AuditRecordCommand build() {
            return new AuditRecordCommand(
                    source,
                    categoryPath == null ? List.of() : List.copyOf(categoryPath),
                    actionCode,
                    actionName,
                    targetType,
                    targetId,
                    targetName,
                    targetCode,
                    reason,
                    maskedMobile,
                    loginMethod,
                    deviceSummary,
                    actor
            );
        }
    }
}
