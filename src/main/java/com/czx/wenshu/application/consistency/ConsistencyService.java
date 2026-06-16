package com.czx.wenshu.application.consistency;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.application.user.QuotaService;
import com.czx.wenshu.application.user.SubscriptionService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.consistency.AiOperationLog;
import com.czx.wenshu.domain.consistency.AiOperationLogRepository;
import com.czx.wenshu.domain.consistency.ConsistencyReportItem;
import com.czx.wenshu.domain.consistency.ConsistencyReportItemRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.task.AsyncTask;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 一致性审查服务（P6-06/P6-07）。
 * 负责触发审查任务、查询报告、更新条目状态。
 */
@Service
public class ConsistencyService {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyService.class);

    private final AiOperationLogRepository logRepository;
    private final ConsistencyReportItemRepository itemRepository;
    private final ProjectRepository projectRepository;
    private final AsyncTaskService asyncTaskService;
    private final QuotaService quotaService;
    private final SubscriptionService subscriptionService;
    private final ConsistencyTaskRunner taskRunner;
    private final Clock clock;

    /**
     * 构造一致性审查服务。
     *
     * @param logRepository       AI 操作日志仓储
     * @param itemRepository      审查条目仓储
     * @param projectRepository   作品仓储
     * @param asyncTaskService    异步任务服务
     * @param quotaService        配额服务
     * @param subscriptionService 订阅服务（用于动态获取套餐限额，P0-3）
     * @param taskRunner          审查任务执行器
     * @param clock               时钟
     */
    public ConsistencyService(AiOperationLogRepository logRepository,
                               ConsistencyReportItemRepository itemRepository,
                               ProjectRepository projectRepository,
                               AsyncTaskService asyncTaskService,
                               QuotaService quotaService,
                               SubscriptionService subscriptionService,
                               ConsistencyTaskRunner taskRunner,
                               Clock clock) {
        this.logRepository = logRepository;
        this.itemRepository = itemRepository;
        this.projectRepository = projectRepository;
        this.asyncTaskService = asyncTaskService;
        this.quotaService = quotaService;
        this.subscriptionService = subscriptionService;
        this.taskRunner = taskRunner;
        this.clock = clock;
    }

    /**
     * 提交一致性审查任务（P6-06）。
     * 触发前检查配额，消耗一次改编/审查次数。
     *
     * @param projectId 目标作品 ID
     * @param userId    当前用户 ID
     * @return Map 包含 taskId 和 reportId
     */
    @Transactional
    public Map<String, String> submitCheck(UUID projectId, UUID userId) {
        log.info("[ConsistencyService] 提交一致性审查 projectId={} userId={}", projectId, userId);
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }

        // 检查配额（P0-3：按用户订阅套餐动态限额扣减次数）
        long[] planLimits = subscriptionService.getUserPlanLimits(userId);
        int adaptationLimit = (int) planLimits[1];
        quotaService.checkAndIncrementAdaptationWithLimit(userId, adaptationLimit);

        // 创建 AI 操作日志作为报告容器
        AiOperationLog report = AiOperationLog.create(userId, projectId,
                "consistency_check", "creative", Instant.now(clock));
        logRepository.save(report);

        // 创建异步任务
        AsyncTask task = asyncTaskService.createTask(userId, projectId, "consistency_check");
        taskRunner.run(task.id(), report.id(), projectId);

        log.info("[ConsistencyService] 审查任务已提交 reportId={} taskId={}", report.id(), task.id());
        return Map.of("taskId", task.id().toString(), "reportId", report.id().toString());
    }

    /**
     * 获取一致性审查报告详情（P6-06）。
     *
     * @param reportId 报告 ID（ai_operation_logs.id）
     * @param userId   当前用户 ID
     * @return 报告详情（含所有审查条目）
     */
    @Transactional(readOnly = true)
    public ConsistencyReportInfo getReport(UUID reportId, UUID userId) {
        AiOperationLog report = logRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "审查报告不存在"));
        if (!userId.equals(report.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权查看该报告");
        }

        List<ConsistencyReportItem> items = itemRepository.findByReportId(reportId);
        long openCount = items.stream().filter(i -> "open".equals(i.status())).count();

        log.debug("[ConsistencyService] 查询报告 reportId={} 条目数={}", reportId, items.size());
        List<ConsistencyItemInfo> itemInfos = items.stream().map(ConsistencyItemInfo::from).toList();
        return new ConsistencyReportInfo(
                reportId.toString(),
                report.projectId() != null ? report.projectId().toString() : null,
                items.size(), (int) openCount,
                report.createdAt().toString(), itemInfos);
    }

    /**
     * 更新审查条目处理状态（P6-07）。
     * 允许值：open / handled / ignored。
     *
     * @param itemId 条目 ID
     * @param userId 当前用户 ID
     * @param status 新状态
     * @return 更新后的条目 DTO
     */
    @Transactional
    public ConsistencyItemInfo updateItemStatus(UUID itemId, UUID userId, String status) {
        ConsistencyReportItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "审查条目不存在"));

        // 通过报告验证权限
        AiOperationLog report = logRepository.findById(item.reportId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "关联报告不存在"));
        if (!userId.equals(report.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权操作该条目");
        }

        item.updateStatus(status, clock);
        itemRepository.save(item);
        log.info("[ConsistencyService] 条目状态已更新 itemId={} newStatus={}", itemId, status);
        return ConsistencyItemInfo.from(item);
    }
}
