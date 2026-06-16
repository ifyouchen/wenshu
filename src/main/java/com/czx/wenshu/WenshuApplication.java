package com.czx.wenshu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@ConfigurationPropertiesScan
public class WenshuApplication {

    public static void main(String[] args) {
        SpringApplication.run(WenshuApplication.class, args);
    }
}
