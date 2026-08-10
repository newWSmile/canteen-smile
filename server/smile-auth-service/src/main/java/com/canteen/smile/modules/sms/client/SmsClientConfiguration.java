package com.canteen.smile.modules.sms.client;

/**
 * 短信策略为一次挑战选择的受管配置引用。
 *
 * @param providerConfigId 平台维护的供应商配置 ID；本地策略为空
 * @param templateConfigId 平台维护的模板配置 ID；本地策略为空
 * @param templateCode 厂商模板编码；本地渲染正文策略为空
 */
public record SmsClientConfiguration(
        Long providerConfigId,
        Long templateConfigId,
        String templateCode
) {

    /** 保证供应商和模板配置引用只能同时为空或同时有值。 */
    public SmsClientConfiguration {
        if ((providerConfigId == null) != (templateConfigId == null)) {
            throw new IllegalArgumentException("SMS provider and template configuration must be both present or absent");
        }
    }

    /** @return 不依赖真实供应商或模板数据的本地策略配置引用 */
    public static SmsClientConfiguration localUnmanaged() {
        return new SmsClientConfiguration(null, null, null);
    }
}
