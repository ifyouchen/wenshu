package com.czx.wenshu.interfaces.rest.user;

import com.czx.wenshu.application.novel.StyleProfileService;
import com.czx.wenshu.application.novel.UserStyleProfileInfo;
import com.czx.wenshu.application.user.ChangePasswordCommand;
import com.czx.wenshu.application.user.DeleteAccountResult;
import com.czx.wenshu.application.user.UpdateAiConsentCommand;
import com.czx.wenshu.application.user.UpdateProfileCommand;
import com.czx.wenshu.application.user.UserApplicationService;
import com.czx.wenshu.application.user.UserInfo;
import com.czx.wenshu.application.user.UUIDCommand;
import com.czx.wenshu.interfaces.rest.project.UpdateWritingGoalRequest;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.IdentityType;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "当前用户、资料、密码与设置")
@Validated
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserApplicationService userApplicationService;
    private final StyleProfileService styleProfileService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserApplicationService userApplicationService,
                           StyleProfileService styleProfileService,
                           CurrentUserProvider currentUserProvider) {
        this.userApplicationService = userApplicationService;
        this.styleProfileService = styleProfileService;
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

    @Operation(summary = "设置首次登录身份类型", description = "选择身份类型（网文作者/短剧编剧/新人作者），影响入口排序和引导文案。")
    @PutMapping("/identity-type")
    public Result<UserInfo> updateIdentityType(@Valid @RequestBody UpdateIdentityTypeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(userApplicationService.updateIdentityType(user.id(), request.identityType()));
    }

    @Operation(summary = "注销账号", description = "软删除账号，30 天内可使用恢复令牌撤销注销。")
    @DeleteMapping
    public Result<DeleteAccountResponse> deleteAccount() {
        User user = currentUserProvider.getCurrentUser();
        DeleteAccountResult result = userApplicationService.deleteAccount(new UUIDCommand(user.id()));
        return Result.ok(new DeleteAccountResponse(result.restoreToken(), result.restoreTokenExpiresAt().toString()));
    }

    @Operation(summary = "撤销账号注销", description = "30 天内使用恢复令牌撤销注销，恢复账号。")
    @PostMapping("/cancel-restore")
    public Result<UserInfo> restoreAccount(@Valid @RequestBody RestoreAccountRequest request) {
        return Result.ok(userApplicationService.restoreAccount(request.restoreToken()));
    }

    @Operation(summary = "设置全局每日写作目标", description = "设置用户全局每日目标字数，优先级低于作品级别目标。")
    @PutMapping("/writing-goal")
    public Result<UserInfo> updateWritingGoal(@Valid @RequestBody UpdateWritingGoalRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(userApplicationService.updateGlobalWritingGoal(user.id(), request.dailyCharGoal()));
    }

    @Operation(summary = "获取文风档案（P5-10）", description = "返回用户的文风样本与分析标签。")
    @GetMapping("/style-profile")
    public Result<UserStyleProfileInfo> getStyleProfile() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(styleProfileService.getProfile(user.id()));
    }

    @Operation(summary = "保存文风样本并触发异步分析（P5-10）",
               description = "保存写作样本，异步调用 LLM 提取文风标签，返回 analysisTaskId 供轮询。")
    @PutMapping("/style-profile")
    public Result<UserStyleProfileInfo> saveStyleProfile(@Valid @RequestBody StyleProfileRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(styleProfileService.saveProfile(user.id(), request.sampleText()));
    }

    @Operation(summary = "删除文风档案（P5-10）", description = "删除用户的文风档案及分析标签。")
    @DeleteMapping("/style-profile")
    public Result<Void> deleteStyleProfile() {
        User user = currentUserProvider.getCurrentUser();
        styleProfileService.deleteProfile(user.id());
        return Result.ok();
    }
}