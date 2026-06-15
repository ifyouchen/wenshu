package com.czx.wenshu.interfaces.rest.user;

import com.czx.wenshu.application.user.ChangePasswordCommand;
import com.czx.wenshu.application.user.UpdateAiConsentCommand;
import com.czx.wenshu.application.user.UpdateProfileCommand;
import com.czx.wenshu.application.user.UserApplicationService;
import com.czx.wenshu.application.user.UserInfo;
import com.czx.wenshu.application.user.UUIDCommand;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.IdentityType;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "当前用户、资料、密码与设置")
@Validated
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserApplicationService userApplicationService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserApplicationService userApplicationService, CurrentUserProvider currentUserProvider) {
        this.userApplicationService = userApplicationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户信息及配额摘要。")
    @GetMapping("/me")
    public Result<UserInfo> me() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(userApplicationService.getCurrentUser(new UUIDCommand(user.id())));
    }

    @Operation(summary = "更新资料", description = "更新昵称、头像、身份类型。")
    @PutMapping("/profile")
    public Result<UserInfo> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(userApplicationService.updateProfile(new UpdateProfileCommand(
                user.id(),
                request.nickname(),
                request.avatarUrl(),
                request.identityType() != null ? IdentityType.fromValue(request.identityType()) : null
        )));
    }

    @Operation(summary = "修改密码", description = "验证当前密码后修改，修改后所有设备需要重新登录。")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();
        userApplicationService.changePassword(new ChangePasswordCommand(
                user.id(),
                request.currentPassword(),
                request.newPassword()
        ));
        return Result.ok();
    }

    @Operation(summary = "AI 训练授权开关", description = "设置是否允许 AI 训练使用内容。")
    @PutMapping("/ai-consent")
    public Result<UserInfo> updateAiConsent(@Valid @RequestBody UpdateAiConsentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(userApplicationService.updateAiConsent(new UpdateAiConsentCommand(
                user.id(),
                request.aiTrainConsent()
        )));
    }

    @Operation(summary = "注销账号", description = "软删除账号，30 天内可撤销注销。")
    @DeleteMapping
    public Result<Void> deleteAccount() {
        User user = currentUserProvider.getCurrentUser();
        userApplicationService.deleteAccount(new UUIDCommand(user.id()));
        return Result.ok();
    }

    @Operation(summary = "撤销账号注销", description = "30 天内撤销注销，恢复账号。")
    @PostMapping("/cancel-restore")
    public Result<UserInfo> restoreAccount() {
        User user = currentUserProvider.getCurrentUserForRestore();
        return Result.ok(userApplicationService.restoreAccount(new UUIDCommand(user.id())));
    }
}