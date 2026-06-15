package com.czx.wenshu.interfaces.rest.project;

import com.czx.wenshu.application.project.CreateWorldElementCommand;
import com.czx.wenshu.application.project.UpdateWorldElementCommand;
import com.czx.wenshu.application.project.WorldElementApplicationService;
import com.czx.wenshu.application.project.WorldElementInfo;
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

@Tag(name = "WorldDict", description = "世界观词典与要素")
@Validated
@RestController
@RequestMapping("/api/v1")
public class WorldElementController {

    private final WorldElementApplicationService worldElementService;
    private final CurrentUserProvider currentUserProvider;

    public WorldElementController(WorldElementApplicationService worldElementService, CurrentUserProvider currentUserProvider) {
        this.worldElementService = worldElementService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "世界观要素列表", description = "获取作品下所有世界观要素。")
    @GetMapping("/projects/{id}/world-dict")
    public Result<List<WorldElementInfo>> listWorldElements(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(worldElementService.listWorldElements(id, user.id()));
    }

    @Operation(summary = "创建世界观要素", description = "在指定作品下创建世界观要素。")
    @PostMapping("/projects/{id}/world-dict")
    public Result<WorldElementInfo> createWorldElement(@PathVariable UUID id, @Valid @RequestBody CreateWorldElementRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(worldElementService.createWorldElement(id, user.id(),
                new CreateWorldElementCommand(request.type(), request.name(), request.description())));
    }

    @Operation(summary = "更新世界观要素", description = "更新世界观要素信息。")
    @PutMapping("/world-dict/{id}")
    public Result<WorldElementInfo> updateWorldElement(@PathVariable UUID id, @RequestBody UpdateWorldElementRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(worldElementService.updateWorldElement(id, user.id(),
                new UpdateWorldElementCommand(request.type(), request.name(), request.description())));
    }

    @Operation(summary = "删除世界观要素", description = "删除世界观要素。")
    @DeleteMapping("/world-dict/{id}")
    public Result<Void> deleteWorldElement(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        worldElementService.deleteWorldElement(id, user.id());
        return Result.ok();
    }
}