package com.canteen.smile.modules.sms.client;

import com.canteen.smile.modules.sms.model.SmsPurpose;

import java.util.Map;
import java.util.Set;

/**
 * 短信策略统一发送命令。
 *
 * <p>该对象包含完整手机号、短信正文和一次性秘密，因此刻意不生成 {@code toString}，
 * 禁止整体输出到日志。</p>
 */
public final class SmsSendCommand {

    /** 发送请求唯一标识。 */
    private final String requestId;

    /** 关联验证码挑战标识，非验证码短信允许为空。 */
    private final String challengeId;

    /** 发送用途。 */
    private final SmsPurpose purpose;

    /** 完整接收手机号，仅允许在内存中传给短信策略。 */
    private final String mobile;

    /** 供应商模板编码，本地策略允许为空。 */
    private final String templateCode;

    /** 已渲染短信正文，仅允许在当前发送调用栈内使用。 */
    private final String renderedContent;

    /** 模板参数，只允许传给策略，禁止写入日志或投递记录。 */
    private final Map<String, String> templateParameters;

    /** 正文中的验证码、Token 等一次性秘密。 */
    private final Set<String> sensitiveValues;

    /**
     * 创建短信发送命令。
     *
     * @param requestId 发送请求唯一标识
     * @param challengeId 关联挑战标识
     * @param purpose 发送用途
     * @param mobile 完整手机号
     * @param templateCode 模板编码
     * @param renderedContent 已渲染正文
     * @param templateParameters 模板参数
     * @param sensitiveValues 必须脱敏的一次性秘密
     */
    public SmsSendCommand(
            String requestId,
            String challengeId,
            SmsPurpose purpose,
            String mobile,
            String templateCode,
            String renderedContent,
            Map<String, String> templateParameters,
            Set<String> sensitiveValues
    ) {
        this.requestId = requestId;
        this.challengeId = challengeId;
        this.purpose = purpose;
        this.mobile = mobile;
        this.templateCode = templateCode;
        this.renderedContent = renderedContent;
        this.templateParameters = templateParameters == null ? null : Map.copyOf(templateParameters);
        this.sensitiveValues = sensitiveValues == null ? null : Set.copyOf(sensitiveValues);
    }

    /** @return 发送请求唯一标识 */
    public String requestId() { return requestId; }

    /** @return 关联验证码挑战标识 */
    public String challengeId() { return challengeId; }

    /** @return 发送用途 */
    public SmsPurpose purpose() { return purpose; }

    /** @return 完整手机号，仅供短信发送链路使用 */
    public String mobile() { return mobile; }

    /** @return 模板编码 */
    public String templateCode() { return templateCode; }

    /** @return 已渲染正文 */
    public String renderedContent() { return renderedContent; }

    /** @return 不可变模板参数 */
    public Map<String, String> templateParameters() { return templateParameters; }

    /** @return 不可变敏感值集合 */
    public Set<String> sensitiveValues() { return sensitiveValues; }
}
