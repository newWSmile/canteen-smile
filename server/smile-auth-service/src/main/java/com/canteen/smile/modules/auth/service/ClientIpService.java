package com.canteen.smile.modules.auth.service;

import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/** 解析网关确认的客户端 IP，并提供安全关联所需的不可逆摘要。 */
@Service
public class ClientIpService {

    /** 网关清洗后向内部服务传递的客户端 IP 请求头。 */
    private static final String TRUSTED_CLIENT_IP_HEADER = "X-Smile-Client-IP";

    /** @param request 当前请求 @return 网关确认的客户端 IP，缺失时回退到 TCP 上一跳。 */
    public String resolve(HttpServletRequest request) {
        String trustedIp = request.getHeader(TRUSTED_CLIENT_IP_HEADER);
        return trustedIp == null || trustedIp.isBlank() ? request.getRemoteAddr() : trustedIp.strip();
    }

    /** @param ipAddress 原始 IP @return 用于安全审计检索的 SHA-256 摘要。 */
    public String hash(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? null
                : HmacRequestSigner.sha256Hex(ipAddress.getBytes(StandardCharsets.UTF_8));
    }
}
