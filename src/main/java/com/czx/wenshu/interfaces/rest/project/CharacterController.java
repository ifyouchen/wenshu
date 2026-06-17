package com.czx.wenshu.interfaces.rest.project;

import com.czx.wenshu.application.project.CharacterApplicationService;
import com.czx.wenshu.application.project.CharacterInfo;
import com.czx.wenshu.application.project.CreateCharacterCommand;
import com.czx.wenshu.application.project.UpdateCharacterCommand;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Characters", description = "角色管理")
@Validated
@RestController
@RequestMapping("/api/v1")
public class CharacterController {

    private final CharacterApplicationService characterService;
    private final CurrentUserProvider currentUserProvider;

    public CharacterController(CharacterApplicationService characterService, CurrentUserProvider currentUserProvider) {
        this.characterService = characterService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "角色列表", description = "获取作品下所有角色。")
    @GetMapping("/projects/{id}/characters")
    public Result<List<CharacterInfo>> listCharacters(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(characterService.listCharacters(id, user.id()));
    }

    @Operation(summary = "创建角色", description = "在指定作品下创建角色。")
    @PostMapping("/projects/{id}/characters")
    public Result<CharacterInfo> createCharacter(@PathVariable UUID id, @Valid @RequestBody CreateCharacterRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(characterService.createCharacter(id, user.id(), new CreateCharacterCommand(
                request.name(), request.role(), request.appearance(), request.personality(),
                request.abilities(), request.speechStyle(), request.status())));
    }

    @Operation(summary = "角色详情", description = "获取角色详情。")
    @GetMapping("/characters/{id}")
    public Result<CharacterInfo> getCharacter(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(characterService.getCharacter(id, user.id()));
    }

    @Operation(summary = "更新角色", description = "更新角色信息。")
    @PutMapping("/characters/{id}")
    public Result<CharacterInfo> updateCharacter(@PathVariable UUID id, @RequestBody UpdateCharacterRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(characterService.updateCharacter(id, user.id(), new UpdateCharacterCommand(
                request.name(), request.role(), request.appearance(), request.personality(),
                request.abilities(), request.speechStyle(), request.status())));
    }

    @Operation(summary = "删除角色", description = "删除角色。")
    @DeleteMapping("/characters/{id}")
    public Result<Void> deleteCharacter(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        characterService.deleteCharacter(id, user.id());
        return Result.ok();
    }

    @Operation(summary = "锁定/解锁角色", description = "切换角色锁定状态，锁定角色优先进入上下文。")
    @PutMapping("/characters/{id}/lock")
    public Result<CharacterInfo> toggleLock(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(characterService.toggleLock(id, user.id()));
    }
}
