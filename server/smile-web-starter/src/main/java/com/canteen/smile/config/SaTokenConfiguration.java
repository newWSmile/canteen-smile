package com.canteen.smile.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.api.AuthPublicApiPaths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sa-Token WebMVC 鉴权配置。 */
@Configuration
public class SaTokenConfiguration implements WebMvcConfigurer {

    /**
     * 对所有 API 默认执行登录校验。
     * 新增公开路径必须基于真实接口进行安全评审后显式排除。
     *
     * @param registry MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/error")
                .excludePathPatterns(AuthPublicApiPaths.ANONYMOUS_PATHS.toArray(String[]::new));
    }
}
