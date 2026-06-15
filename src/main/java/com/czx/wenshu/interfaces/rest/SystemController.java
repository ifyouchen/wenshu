package com.czx.wenshu.interfaces.rest;

import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.infrastructure.config.WenshuProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "系统探针与基础信息")
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final WenshuProperties properties;

    public SystemController(WenshuProperties properties) {
        this.properties = properties;
    }

    @Operation(summary = "系统探针", description = "返回服务状态、产品名、API 版本和当前时间。")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
                "status", "UP",
                "product", properties.getProductName(),
                "apiVersion", properties.getApiVersion(),
                "time", Instant.now()
        ));
    }
}
