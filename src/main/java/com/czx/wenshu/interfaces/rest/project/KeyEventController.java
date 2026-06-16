package com.czx.wenshu.interfaces.rest.project;

import com.czx.wenshu.application.project.KeyEventInfo;
import com.czx.wenshu.application.project.KeyEventService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 章节关键事件控制器（P6-03）。
 * 提供关键事件的异步提取触发和查询接口。
 */
@Tag(name = "KeyEvents", description = "章节关键事件时间线（P6-03）")
@RestController
@RequestMapping("/api/v1")
public class KeyEventController {

    /** 关键事件服务。 */
    private final KeyEventService keyEventService;
    /** 当前用户提供者。 */
    private final CurrentUserProvider currentUserProvider;

    public KeyEventController(KeyEventService keyEventService,
                               CurrentUserProvider currentUserProvider) {
        this.keyEventService = keyEventService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "触发章节关键事件提取（P6-03）",
               description = "异步调用 LLM 提取章节中的关键事件节点，返回 taskId，可通过 /tasks/{taskId}/progress 轮询进度。")
    @PostMapping("/chapters/{id}/key-events")
    public Result<Map<String, String>> extractKeyEvents(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        String taskId = keyEventService.submitKeyEventExtraction(id, user.id());
        return Result.ok(Map.of("taskId", taskId));
    }

    @Operation(summary = "查询章节关键事件（P6-03）",
               description = "返回已提取的章节关键事件列表，按重要程度降序排列。")
    @GetMapping("/chapters/{id}/key-events")
    public Result<List<KeyEventInfo>> getKeyEvents(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(keyEventService.getKeyEvents(id, user.id()));
    }

    @Operation(summary = "查询作品关键事件时间线（P6-03）",
               description = "返回作品所有章节的关键事件，按重要程度降序排列，用于时间线展示。")
    @GetMapping("/projects/{id}/key-events")
    public Result<List<KeyEventInfo>> getProjectKeyEvents(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(keyEventService.getProjectKeyEvents(id, user.id()));
    }
}
