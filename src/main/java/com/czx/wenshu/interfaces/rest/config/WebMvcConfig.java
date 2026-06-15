package com.czx.wenshu.interfaces.rest.config;

import com.czx.wenshu.interfaces.rest.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/user/**", "/api/v1/projects/**", "/api/v1/volumes/**", "/api/v1/chapters/**")
                .excludePathPatterns(
                        "/api/v1/user/cancel-restore"
                );
    }
}