package com.canteen.smile.audit.support;

import com.canteen.smile.audit.spi.AuditClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 从当前 Servlet 请求读取网关清洗后的客户端 IP。 */
public class ServletAuditClientIpResolver implements AuditClientIpResolver {

    /** 网关移除外部同名值后写入的可信客户端 IP 请求头。 */
    private static final String TRUSTED_CLIENT_IP_HEADER = "X-Smile-Client-IP";

    /** {@inheritDoc} */
    @Override
    public String resolve() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String trustedIp = text(request.getHeader(TRUSTED_CLIENT_IP_HEADER));
        return trustedIp == null ? text(request.getRemoteAddr()) : trustedIp;
    }

    /** @param value 可选文本 @return 去除首尾空白后的文本 */
    private String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
