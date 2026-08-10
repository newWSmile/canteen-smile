package com.canteen.smile.modules.sms.service;

import com.canteen.smile.modules.sms.client.SmsClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 按供应商编码选择短信策略，厂商 SDK 接入时无需修改业务发送服务。 */
@Component
public class SmsClientRouter {

    /** 已注册短信策略的不可变索引。 */
    private final Map<String, SmsClient> clientsByProviderCode;

    /**
     * 创建短信策略路由并拒绝重复策略编码。
     *
     * @param clients Spring 容器中已注册的短信策略
     */
    public SmsClientRouter(List<SmsClient> clients) {
        Map<String, SmsClient> clientsIndex = new HashMap<>();
        for (SmsClient client : clients) {
            String providerCode = normalize(client.providerCode());
            SmsClient previous = clientsIndex.putIfAbsent(providerCode, client);
            if (previous != null) {
                throw new IllegalStateException("Duplicate SMS provider code: " + providerCode);
            }
        }
        this.clientsByProviderCode = Map.copyOf(clientsIndex);
    }

    /**
     * 解析指定编码的短信策略。
     *
     * @param providerCode 配置的策略编码
     * @return 短信策略
     */
    public SmsClient resolve(String providerCode) {
        SmsClient client = clientsByProviderCode.get(normalize(providerCode));
        if (client == null) {
            throw new SmsDispatchException(
                    SmsDispatchException.PROVIDER_NOT_AVAILABLE,
                    "当前短信发送策略不可用"
            );
        }
        return client;
    }

    /** 归一化策略编码，避免大小写和首尾空白导致错误路由。 */
    private String normalize(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return "";
        }
        return providerCode.trim().toUpperCase(Locale.ROOT);
    }
}
