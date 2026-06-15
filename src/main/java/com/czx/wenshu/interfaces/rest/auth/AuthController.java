package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.AuthApplicationService;
import com.czx.wenshu.application.auth.ForgotPasswordCommand;
import com.czx.wenshu.application.auth.LoginCommand;
import com.czx.wenshu.application.auth.RefreshTokenCommand;
import com.czx.wenshu.application.auth.RegisterCommand;
import com.czx.wenshu.application.auth.RegisterResult;
import com.czx.wenshu.application.auth.ResendVerifyEmailCommand;
import com.czx.wenshu.application.auth.ResetPasswordCommand;
import com.czx.wenshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "注册、登录与令牌")
@Validated
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

    @Operation(summary = "邮箱密码登录", description = "邮箱密码登录，连续 5 次失败后锁定 15 分钟。")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(LoginResponse.from(authApplicationService.login(new LoginCommand(
                request.email(),
                request.password()
        ))));
    }

    @Operation(summary = "登出当前设备", description = "登出当前设备；令牌持久化与吊销在 P1-06 完成。")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authApplicationService.logout(authorizationHeader);
        return Result.ok();
    }

    @Operation(summary = "Refresh Token 轮换", description = "使用 Refresh Token 换取新的双 Token，旧 Refresh Token 立即失效。")
    @PostMapping("/refresh")
    public Result<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(RefreshTokenResponse.from(authApplicationService.refreshToken(
                new RefreshTokenCommand(request.refreshToken())
        )));
    }

    @Operation(summary = "发起密码重置", description = "生成 24 小时有效的密码重置 token，并发送重置邮件。")
    @PostMapping("/password/forgot")
    public Result<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return Result.ok(ForgotPasswordResponse.from(authApplicationService.forgotPassword(
                new ForgotPasswordCommand(request.email())
        )));
    }

    @Operation(summary = "重置密码", description = "使用密码重置 token 设置新密码，并吊销所有 Refresh Token。")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authApplicationService.resetPassword(new ResetPasswordCommand(request.token(), request.newPassword()));
        return Result.ok();
    }

    @Operation(summary = "邮箱验证", description = "验证 24 小时内有效的邮箱 token，验证后账号可使用 AI 功能。")
    @GetMapping("/verify-email")
    public Result<VerifyEmailResponse> verifyEmail(@RequestParam("token") @NotBlank String token) {
        return Result.ok(VerifyEmailResponse.from(authApplicationService.verifyEmail(token)));
    }

    @Operation(summary = "重发邮箱验证", description = "为未验证账号重新发送验证邮件，同一账号 60 秒限流。")
    @PostMapping("/resend-verify")
    public Result<ResendVerifyEmailResponse> resendVerifyEmail(@Valid @RequestBody ResendVerifyEmailRequest request) {
        return Result.ok(ResendVerifyEmailResponse.from(authApplicationService.resendVerifyEmail(
                new ResendVerifyEmailCommand(request.email())
        )));
    }
}
