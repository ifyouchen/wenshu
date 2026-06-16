package com.czx.wenshu.interfaces.rest.stats;

import com.czx.wenshu.application.stats.HeatmapEntry;
import com.czx.wenshu.application.stats.MonthlySummaryInfo;
import com.czx.wenshu.application.stats.ProjectProgressInfo;
import com.czx.wenshu.application.stats.WritingOverviewInfo;
import com.czx.wenshu.application.stats.WritingStatsQueryService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stats", description = "写作统计")
@Validated
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final WritingStatsQueryService statsQueryService;
    private final CurrentUserProvider currentUserProvider;

    public StatsController(WritingStatsQueryService statsQueryService,
                            CurrentUserProvider currentUserProvider) {
        this.statsQueryService = statsQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "写作统计总览",
               description = "返回今日字数/目标/进度、7 日趋势、连续写作天数、365 日总字数。")
    @GetMapping("/writing")
    public Result<WritingOverviewInfo> getOverview() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(statsQueryService.getOverview(user.id()));
    }

    @Operation(summary = "写作热力图",
               description = "返回过去 365 天每天的写作字数，用于热力图展示。")
    @GetMapping("/writing/heatmap")
    public Result<List<HeatmapEntry>> getHeatmap() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(statsQueryService.getHeatmap(user.id()));
    }

    @Operation(summary = "各作品写作进度",
               description = "返回用户所有作品的总字数、每日目标及今日完成字数。")
    @GetMapping("/writing/projects")
    public Result<List<ProjectProgressInfo>> getProjectProgress() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(statsQueryService.getProjectProgress(user.id()));
    }

    @Operation(summary = "月度写作摘要",
               description = "返回指定月份的总字数、活跃天数、日均字数及每日明细，yearMonth 格式为 yyyy-MM。")
    @GetMapping("/writing/monthly/{yearMonth}")
    public Result<MonthlySummaryInfo> getMonthlySummary(@PathVariable String yearMonth) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(statsQueryService.getMonthlySummary(user.id(), yearMonth));
    }
}
