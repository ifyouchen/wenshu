package com.czx.wenshu.interfaces.rest.safety;

import com.czx.wenshu.application.safety.AppealInfo;
import com.czx.wenshu.application.safety.ContentSafetyService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.safety.ContentAppeal;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容安全与合规接口（P9-05 / P9-06）。
 *
 * <ul>
 *   <li>GET  /content/policy  — 内容安全策略说明（P9-05/P9-06，无需鉴权）</li>
 *   <li>POST /content/appeals — 提交内容安全误报申诉（P9-05，需鉴权）</li>
 *   <li>GET  /content/appeals — 查询当前用户申诉历史（P9-05，需鉴权）</li>
 * </ul>
 */
@Tag(name = "ContentSafety", description = "内容安全策略、AI 辅助标识与误报申诉")
@Validated
@RestController
@RequestMapping("/api/v1/content")
public class ContentSafetyController {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyController.class);

    private final ContentSafetyService contentSafetyService;
    private final CurrentUserProvider currentUserProvider;

    public ContentSafetyController(ContentSafetyService contentSafetyService,
                                   CurrentUserProvider currentUserProvider) {
        this.contentSafetyService = contentSafetyService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 查询内容安全策略及版权合规说明（P9-05 / P9-06）。
     *
     * <p>包含：</p>
     * <ul>
     *   <li>AI 输出内容安全等级说明（A/B/C/D 四级）</li>
     *   <li>版权免责：AI 辅助生成内容归用户所有</li>
     *   <li>AI 辅助标识：导出作品含 "AI 辅助生成" 标注</li>
     *   <li>隐私策略要点</li>
     * </ul>
     *
     * @return 策略文本 Map（无需鉴权）
     */
    @Operation(summary = "查询内容安全与合规策略（P9-05/P9-06）",
               description = "返回 AI 输出安全分级、版权说明、AI 辅助标识规范、隐私策略摘要。")
    @GetMapping("/policy")
    public Result<Map<String, Object>> getPolicy() {
        log.info("[ContentSafetyController] 查询内容安全策略");
        Map<String, Object> policy = Map.of(
                "safetyLevels", List.of(
                        Map.of("level", "A", "description", "内容安全，直接展示"),
                        Map.of("level", "B", "description", "轻微敏感，展示前置提示"),
                        Map.of("level", "C", "description", "中等风险，内容替换并透明说明"),
                        Map.of("level", "D", "description", "高风险，内容拦截，提供申诉入口")
                ),
                "copyright", Map.of(
                        "ownership", "AI 辅助生成内容版权归用户所有，文枢仅提供创作工具服务",
                        "disclaimer", "AI 生成内容仅供参考，用户需对最终发布内容负责",
                        "exportAnnotation", "导出文件将在封面/页脚添加 '本作品由 AI 辅助生成' 标注（P9-06）"
                ),
                "privacy", Map.of(
                        "dataUsage", "作品内容默认私有，不对外公开",
                        "aiTraining", "是否允许 AI 训练可在账户设置中随时关闭",
                        "retention", "用户注销后数据在 30 天宽限期到期后永久删除"
                ),
                "appealProcess", "如认为 AI 输出被误判，可通过 POST /api/v1/content/appeals 提交申诉"
        );
        return Result.ok(policy);
    }

    /**
     * 提交内容安全误报申诉（P9-05）。
     *
     * <p>用户提交申诉后，状态为 pending，等待人工审核。</p>
     *
     * @param request 申诉请求（含被过滤内容片段和申诉理由）
     * @return 申诉摘要
     */
    @Operation(summary = "提交内容安全误报申诉（P9-05）",
               description = "用户认为 AI 输出被误判违规时提交申诉，返回申诉记录 ID。")
    @PostMapping("/appeals")
    public Result<AppealInfo> submitAppeal(@Valid @RequestBody AppealRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[ContentSafetyController] 用户 {} 提交申诉 contentLen={}", user.id(), request.content().length());
        ContentAppeal appeal = contentSafetyService.submitAppeal(
                user.id(), request.content(), request.reason());
        return Result.ok(AppealInfo.from(appeal));
    }

    /**
     * 查询当前用户的申诉历史（P9-05）。
     *
     * @return 申诉列表（按创建时间倒序）
     */
    @Operation(summary = "查询申诉历史（P9-05）", description = "返回当前用户提交的所有内容安全申诉记录。")
    @GetMapping("/appeals")
    public Result<List<AppealInfo>> listAppeals() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(contentSafetyService.listAppeals(user.id()));
    }

    // ── 请求 DTO ──────────────────────────────────────────────────────────────

    /**
     * 提交申诉的请求体。
     *
     * @param content 被过滤的内容片段（最多 500 字符）
     * @param reason  申诉理由（最多 1000 字符）
     */
    public record AppealRequest(
            @NotBlank(message = "内容不能为空")
            @Size(max = 500, message = "内容最多 500 字符")
            String content,

            @NotBlank(message = "申诉理由不能为空")
            @Size(max = 1000, message = "申诉理由最多 1000 字符")
            String reason) {
    }
}
