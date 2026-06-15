package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.AuthApplicationService;
import com.czx.wenshu.application.auth.RegisterCommand;
import com.czx.wenshu.application.auth.RegisterResult;
import com.czx.wenshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "注册、登录与令牌")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @Operation(summary = "邮箱密码注册", description = "创建未验证账号并返回 Access Token、Refresh Token 和用户信息。")
    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResult result = authApplicationService.register(new RegisterCommand(
                request.email(),
                request.password(),
                request.nickname()
        ));
        return Result.ok(RegisterResponse.from(result));
    }
}
