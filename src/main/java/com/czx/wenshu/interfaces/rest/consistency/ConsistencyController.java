package com.czx.wenshu.interfaces.rest.consistency;

import com.czx.wenshu.application.consistency.ConsistencyItemInfo;
import com.czx.wenshu.application.consistency.ConsistencyReportInfo;
import com.czx.wenshu.application.consistency.ConsistencyService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 一致性审查控制器（P6-06/P6-07）。
 * 提供触发审查、查询报告、更新条目状态的接口。
 */
@Tag(name = "Consistency", description = "一致性审查（P6-06/P6-07）")
@Validated
@RestController
@RequestMapping("/api/v1/consistency")
public class ConsistencyController {

    /** 一致性审查服务。 */
    private final ConsistencyService consistencyService;
    /** 当前用户提供者。 */
    private final CurrentUserProvider currentUserProvider;

    public ConsistencyController(ConsistencyService consistencyService,
                                  CurrentUserProvider currentUserProvider) {
        this.consistencyService = consistencyService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "触发一致性审查（P6-06）",
               description = "异步分析作品内容，消耗一次月度审查配额，返回 taskId 和 reportId。")
    @PostMapping("/check")
    public Result<Map<String, String>> check(@RequestParam @NotNull UUID projectId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(consistencyService.submitCheck(projectId, user.id()));
    }

    @Operation(summary = "查询审查报告（P6-06）",
               description = "返回报告元信息和所有审查条目（含已处理/已忽略）。")
    @GetMapping("/reports/{id}")
    public Result<ConsistencyReportInfo> getReport(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(consistencyService.getReport(id, user.id()));
    }

    @Operation(summary = "更新审查条目状态（P6-07）",
               description = "将条目状态更新为 open/handled/ignored 之一。")
    @PatchMapping("/items/{id}")
    public Result<ConsistencyItemInfo> updateItemStatus(
            @PathVariable UUID id,
            @RequestBody StatusUpdateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(consistencyService.updateItemStatus(id, user.id(), request.status()));
    }

    /**
     * 状态更新请求体。
     *
     * @param status 新状态：open / handled / ignored
     */
    public record StatusUpdateRequest(@NotBlank String status) {}
}
