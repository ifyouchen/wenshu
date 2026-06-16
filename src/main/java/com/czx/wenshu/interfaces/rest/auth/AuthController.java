package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.AuthApplicationService;
import com.czx.wenshu.application.auth.ForgotPasswordCommand;
import com.czx.wenshu.application.auth.LoginCommand;
import com.czx.wenshu.application.auth.RefreshTokenCommand;
import com.czx.wenshu.application.auth.RegisterCommand;
import com.czx.wenshu.application.auth.RegisterResult;
import com.czx.wenshu.application.auth.ResendVerifyEmailCommand;
import com.czx.wenshu.application.auth.ResetPasswordCommand;
import com.czx.wenshu.application.auth.SendRegisterCodeCommand;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthApplicationService authApplicationService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 构造认证控制器。
     *
     * @param authApplicationService 认证应用服务
     * @param currentUserProvider    当前用户提供者
     */
    public AuthController(AuthApplicationService authApplicationService,
                          CurrentUserProvider currentUserProvider) {
        this.authApplicationService = authApplicationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "发送注册验证码", description = "向邮箱发送 6 位注册验证码，60 秒内不可重复发送。")
    @PostMapping("/register/code")
    public Result<SendRegisterCodeResponse> sendRegisterCode(@Valid @RequestBody SendRegisterCodeRequest request) {
        log.info("[AuthController] 发送注册验证码 email={}", request.email());
        return Result.ok(SendRegisterCodeResponse.from(authApplicationService.sendRegisterCode(
                new SendRegisterCodeCommand(request.email())
        )));
    }

    @Operation(summary = "邮箱验证码注册", description = "校验邮箱验证码，创建已验证账号并返回 Access Token、Refresh Token 和用户信息。")
    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[AuthController] 邮箱注册 email={} 昵称={}", request.email(), request.nickname());
        RegisterResult result = authApplicationService.register(new RegisterCommand(
                request.email(),
                request.password(),
                request.nickname(),
                request.verificationCode()
        ));
        return Result.ok(RegisterResponse.from(result));
    }

    @Operation(summary = "邮箱密码登录", description = "邮箱密码登录，连续 5 次失败后锁定 15 分钟。")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[AuthController] 邮箱登录 email={}", request.email());
        return Result.ok(LoginResponse.from(authApplicationService.login(new LoginCommand(
                request.email(),
                request.password()
        ))));
    }

    @Operation(summary = "登出当前设备", description = "登出当前设备；令牌持久化与吊销在 P1-06 完成。")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        log.info("[AuthController] 当前设备登出");
        authApplicationService.logout(authorizationHeader);
        return Result.ok();
    }

    @Operation(summary = "Refresh Token 轮换", description = "使用 Refresh Token 换取新的双 Token，旧 Refresh Token 立即失效。")
    @PostMapping("/refresh")
    public Result<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("[AuthController] Token 轮换");
        return Result.ok(RefreshTokenResponse.from(authApplicationService.refreshToken(
                new RefreshTokenCommand(request.refreshToken())
        )));
    }

    @Operation(summary = "发起密码重置", description = "生成 24 小时有效的密码重置 token，并发送重置邮件。")
    @PostMapping("/password/forgot")
    public Result<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("[AuthController] 发起密码重置 email={}", request.email());
        return Result.ok(ForgotPasswordResponse.from(authApplicationService.forgotPassword(
                new ForgotPasswordCommand(request.email())
        )));
    }

    @Operation(summary = "重置密码", description = "使用密码重置 token 设置新密码，并吊销所有 Refresh Token。")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("[AuthController] 重置密码");
        authApplicationService.resetPassword(new ResetPasswordCommand(request.token(), request.newPassword()));
        return Result.ok();
    }

    @Operation(summary = "邮箱验证", description = "验证 24 小时内有效的邮箱 token，验证后账号可使用 AI 功能。")
    @GetMapping("/verify-email")
    public Result<VerifyEmailResponse> verifyEmail(@RequestParam("token") @NotBlank String token) {
        log.info("[AuthController] 邮箱验证");
        return Result.ok(VerifyEmailResponse.from(authApplicationService.verifyEmail(token)));
    }

    @Operation(summary = "重发邮箱验证", description = "为未验证账号重新发送验证邮件，同一账号 60 秒限流。")
    @PostMapping("/resend-verify")
    public Result<ResendVerifyEmailResponse> resendVerifyEmail(@Valid @RequestBody ResendVerifyEmailRequest request) {
        log.info("[AuthController] 重发邮箱验证 email={}", request.email());
        return Result.ok(ResendVerifyEmailResponse.from(authApplicationService.resendVerifyEmail(
                new ResendVerifyEmailCommand(request.email())
        )));
    }

    /**
     * 登出所有设备（P0-4）。
     * 吊销当前用户的所有 Access Token 和 Refresh Token。
     */
    @Operation(summary = "登出所有设备（P0-4）")
    @PostMapping("/logout-all")
    public Result<Void> logoutAll() {
        User user = currentUserProvider.getCurrentUser();
        log.info("[AuthController] 登出所有设备 userId={}", user.id());
        authApplicationService.logoutAll(user.id());
        return Result.ok();
    }
}
