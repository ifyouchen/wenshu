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
                .addPathPatterns(
                        "/api/v1/user/**",
                        "/api/v1/projects/**",
                        "/api/v1/volumes/**",
                        "/api/v1/chapters/**",
                        "/api/v1/snapshots/**",
                        "/api/v1/characters/**",
                        "/api/v1/world-dict/**",
                        "/api/v1/import/**",
                        "/api/v1/stats/**",
                        "/api/v1/tasks/**",
                        "/api/v1/novel/**",
                        "/api/v1/skeleton/**",
                        "/api/v1/polish/**",
                        "/api/v1/consistency/**",
                        "/api/v1/script/**",
                        "/api/v1/subscriptions/current",
                        "/api/v1/subscriptions/checkout",
                        "/api/v1/subscriptions/topup",
                        "/api/v1/subscriptions/cancel",
                        "/api/v1/content/appeals",
                        "/api/v1/teams/**"
                )
                .excludePathPatterns(
                        "/api/v1/user/cancel-restore"
                );
    }
}