package com.canteen.smile.modules.sms.client;

import com.canteen.smile.modules.sms.model.SmsPurpose;

/** 短信供应商策略统一边界；后续厂商 SDK 必须通过该接口接入。 */
public interface SmsClient {

    /**
     * 返回该策略唯一编码。
     *
     * @return 策略编码
     */
    String providerCode();

    /**
     * 解析本次用途对应的受管供应商和模板配置。
     *
     * @param purpose 短信用途
     * @return 配置引用；本地策略返回两个 ID 均为空的明确结果
     */
    SmsClientConfiguration configurationFor(SmsPurpose purpose);

    /**
     * 提交一次短信发送请求。
     *
     * @param command 发送命令
     * @return 策略接受结果
     */
    SmsSendResult send(SmsSendCommand command);
}
