package com.czx.wenshu.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI wenshuOpenApi(WenshuProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getProductName() + " API")
                        .version(properties.getApiVersion())
                        .description("文枢 wenshu 后端接口文档")
                        .license(new License().name("Proprietary")))
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current server")));
    }
}
