package com.czx.wenshu.application.project;

import com.czx.wenshu.application.user.SubscriptionService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.WorldElement;
import com.czx.wenshu.domain.project.WorldElementRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 世界观要素（专有名词词典）应用服务（P3-05 / P1-5）。
 */
@Service
public class WorldElementApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WorldElementApplicationService.class);

    /** 免费套餐词典条目上限。 */
    private static final int FREE_DICT_LIMIT = 50;
    /** 专业版套餐词典条目上限。 */
    private static final int PRO_DICT_LIMIT = 300;

    private final WorldElementRepository worldElementRepository;
    private final ProjectRepository projectRepository;
    private final SubscriptionService subscriptionService;
    private final Clock clock;

    /**
     * 构造世界观要素应用服务。
     *
     * @param worldElementRepository 世界观要素仓储
     * @param projectRepository      作品仓储
     * @param subscriptionService    订阅服务（用于获取套餐限额）
     * @param clock                  时钟
     */
    public WorldElementApplicationService(WorldElementRepository worldElementRepository,
                                           ProjectRepository projectRepository,
                                           SubscriptionService subscriptionService,
                                           Clock clock) {
        this.worldElementRepository = worldElementRepository;
        this.projectRepository = projectRepository;
        this.subscriptionService = subscriptionService;
        this.clock = clock;
    }

    /**
     * 创建世界观要素（专有名词词典条目），创建前检查套餐容量限制（P1-5）。
     *
     * @param projectId 作品 ID
     * @param userId    用户 ID
     * @param command   创建命令
     * @return 创建的要素信息
     */
    @Transactional
    public WorldElementInfo createWorldElement(UUID projectId, UUID userId, CreateWorldElementCommand command) {
        verifyProjectOwnership(projectId, userId);
        // P1-5：按套餐检查词典容量
        int currentCount = worldElementRepository.countByProjectId(projectId);
        int limit = getDictLimit(userId);
        if (limit >= 0 && currentCount >= limit) {
            log.warn("[WorldElementApplicationService] 词典已达容量上限 projectId={} count={} limit={}",
                    projectId, currentCount, limit);
            throw new ApiException(ErrorCode.RATE_LIMITED,
                    "专有名词词典已达套餐上限（免费版50条/专业版300条），请升级套餐");
        }
        WorldElement element = WorldElement.create(projectId, command.type(), command.name(),
                command.description(), toJsonArray(command.aliases()), clock);
        worldElementRepository.save(element);
        log.info("[WorldElementApplicationService] 创建词典条目 projectId={} name={}", projectId, command.name());
        return WorldElementInfo.from(element);
    }

    @Transactional(readOnly = true)
    public List<WorldElementInfo> listWorldElements(UUID projectId, UUID userId) {
        verifyProjectOwnership(projectId, userId);
        return worldElementRepository.findByProjectId(projectId).stream().map(WorldElementInfo::from).toList();
    }

    @Transactional
    public WorldElementInfo updateWorldElement(UUID elementId, UUID userId, UpdateWorldElementCommand command) {
        WorldElement element = worldElementRepository.findById(elementId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "世界观要素不存在"));
        verifyProjectOwnership(element.projectId(), userId);
        element.update(command.type(), command.name(), command.description(), toJsonArray(command.aliases()));
        worldElementRepository.save(element);
        return WorldElementInfo.from(element);
    }

    @Transactional
    public void deleteWorldElement(UUID elementId, UUID userId) {
        WorldElement element = worldElementRepository.findById(elementId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "世界观要素不存在"));
        verifyProjectOwnership(element.projectId(), userId);
        worldElementRepository.deleteById(elementId);
    }

    /**
     * P3-05：角色名更新时同步词典条目。
     * 若同一作品下存在名称与旧角色名完全一致的词典条目，则将其名称更新为新角色名。
     */
    @Transactional
    public void syncCharacterName(UUID projectId, String oldName, String newName) {
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return;
        }
        Optional<WorldElement> existing = worldElementRepository.findByProjectIdAndName(projectId, oldName);
        existing.ifPresent(element -> {
            element.syncName(newName);
            worldElementRepository.save(element);
        });
    }

    private void verifyProjectOwnership(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
    }

    /**
     * 根据用户订阅套餐获取词典容量上限（P1-5）。
     * 返回 -1 表示无限制（企业版/团队版）。
     *
     * @param userId 用户 ID
     * @return 容量上限（-1 表示无限制）
     */
    private int getDictLimit(UUID userId) {
        try {
            long[] planLimits = subscriptionService.getUserPlanLimits(userId);
            long monthlyCharLimit = planLimits[0];
            // 按月字符限额判断套餐层级：免费版=100000，专业版=2000000，企业版=10000000
            if (monthlyCharLimit >= 10_000_000L) {
                return -1; // 企业版/团队版：无限制
            } else if (monthlyCharLimit >= 2_000_000L) {
                return PRO_DICT_LIMIT; // 专业版：300条
            } else {
                return FREE_DICT_LIMIT; // 免费版：50条
            }
        } catch (Exception e) {
            log.warn("[WorldElementApplicationService] 获取套餐限额失败，使用免费版限额 userId={}", userId);
            return FREE_DICT_LIMIT;
        }
    }

    /** 将 List<String> 序列化为 JSON 数组字符串，如 ["别名A","别名B"]。 */
    static String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return list.stream()
                .map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}