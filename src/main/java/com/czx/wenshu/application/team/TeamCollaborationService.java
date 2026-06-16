package com.czx.wenshu.application.team;

import com.czx.wenshu.application.user.QuotaInfo;
import com.czx.wenshu.application.user.QuotaService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectCollaborator;
import com.czx.wenshu.domain.project.ProjectCollaboratorRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.team.TeamMember;
import com.czx.wenshu.domain.team.TeamMemberRepository;
import com.czx.wenshu.domain.team.TeamRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 团队作品协作与统一账单服务（P9-08）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>为项目添加/移除协作者（需项目所有者权限）</li>
 *   <li>查询项目所有协作者</li>
 *   <li>校验用户是否有项目访问权（所有者或协作者）</li>
 *   <li>统计团队整体配额用量（按成员汇总）</li>
 * </ul>
 */
@Service
public class TeamCollaborationService {

    private static final Logger log = LoggerFactory.getLogger(TeamCollaborationService.class);

    private final ProjectCollaboratorRepository collaboratorRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final QuotaService quotaService;
    private final Clock clock;

    /** 构造函数注入。 */
    public TeamCollaborationService(ProjectCollaboratorRepository collaboratorRepository,
                                    ProjectRepository projectRepository,
                                    TeamRepository teamRepository,
                                    TeamMemberRepository teamMemberRepository,
                                    QuotaService quotaService,
                                    Clock clock) {
        this.collaboratorRepository = collaboratorRepository;
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.quotaService = quotaService;
        this.clock = clock;
    }

    /**
     * 添加项目协作者（P9-08）。
     *
     * <p>只有项目所有者可以添加协作者。</p>
     *
     * @param projectId    作品 ID
     * @param ownerId      操作者（必须为作品所有者）
     * @param collaboratorId 新协作者 ID
     * @param role         协作角色（editor/viewer）
     * @return 新协作者信息
     */
    @Transactional
    public CollaboratorInfo addCollaborator(UUID projectId, UUID ownerId,
                                            UUID collaboratorId, String role) {
        log.info("[TeamCollaborationService] 添加协作者 projectId={} ownerId={} collaboratorId={}",
                projectId, ownerId, collaboratorId);
        // 验证项目所有者
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        if (!project.userId().equals(ownerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "只有作品所有者可以添加协作者");
        }
        // 检查是否已是协作者
        collaboratorRepository.findByProjectIdAndUserId(projectId, collaboratorId)
                .ifPresent(c -> {
                    throw new ApiException(ErrorCode.BAD_REQUEST, "该用户已是协作者");
                });

        ProjectCollaborator collab = ProjectCollaborator.add(projectId, collaboratorId,
                role != null ? role : "editor", ownerId, clock);
        collaboratorRepository.save(collab);
        log.info("[TeamCollaborationService] 协作者已添加 collaboratorId={} role={}", collaboratorId, role);
        return CollaboratorInfo.from(collab);
    }

    /**
     * 移除项目协作者（P9-08）。
     *
     * @param projectId      作品 ID
     * @param ownerId        操作者（必须为作品所有者）
     * @param collaboratorId 要移除的协作者用户 ID
     */
    @Transactional
    public void removeCollaborator(UUID projectId, UUID ownerId, UUID collaboratorId) {
        log.info("[TeamCollaborationService] 移除协作者 projectId={} collaboratorId={}", projectId, collaboratorId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        if (!project.userId().equals(ownerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "只有作品所有者可以移除协作者");
        }
        collaboratorRepository.deleteByProjectIdAndUserId(projectId, collaboratorId);
        log.info("[TeamCollaborationService] 协作者已移除 collaboratorId={}", collaboratorId);
    }

    /**
     * 查询项目所有协作者（P9-08）。
     *
     * @param projectId 作品 ID
     * @param userId    当前用户（需为所有者或协作者）
     * @return 协作者列表
     */
    @Transactional(readOnly = true)
    public List<CollaboratorInfo> listCollaborators(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        // 所有者或协作者可查看
        boolean hasAccess = project.userId().equals(userId) ||
                collaboratorRepository.findByProjectIdAndUserId(projectId, userId).isPresent();
        if (!hasAccess) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该作品的协作者列表");
        }
        return collaboratorRepository.findByProjectId(projectId).stream()
                .map(CollaboratorInfo::from).toList();
    }

    /**
     * 查询团队整体配额用量（P9-08）。
     *
     * <p>汇总团队所有活跃成员当月的 AI 配额用量，返回统一账单视图。</p>
     *
     * @param teamId  团队 ID
     * @param userId  查询者（需为团队成员）
     * @return 每个成员的配额用量 Map（userId → QuotaInfo）
     */
    @Transactional(readOnly = true)
    public TeamUsageInfo getTeamUsage(UUID teamId, UUID userId) {
        log.info("[TeamCollaborationService] 查询团队用量 teamId={} requestedBy={}", teamId, userId);
        // 验证查询者为团队成员
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .filter(m -> "active".equals(m.status()))
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "非团队活跃成员"));

        List<TeamMember> activeMembers = teamMemberRepository.findActiveByTeamId(teamId);

        // 汇总每个成员的配额用量
        Map<String, QuotaInfo> memberUsage = activeMembers.stream()
                .collect(Collectors.toMap(
                        m -> m.userId().toString(),
                        m -> quotaService.getQuotaInfo(m.userId())
                ));

        // 计算团队总用量
        long totalUsedChars = memberUsage.values().stream()
                .mapToLong(QuotaInfo::usedChars).sum();
        int totalUsedAdaptations = memberUsage.values().stream()
                .mapToInt(QuotaInfo::usedAdaptations).sum();

        return new TeamUsageInfo(teamId, activeMembers.size(),
                totalUsedChars, totalUsedAdaptations, memberUsage);
    }

    // ── 内嵌 DTO ──────────────────────────────────────────────────────────────

    /**
     * 协作者信息 DTO。
     */
    public record CollaboratorInfo(
            UUID id, UUID projectId, UUID userId, String role, Instant createdAt) {
        public static CollaboratorInfo from(ProjectCollaborator c) {
            return new CollaboratorInfo(c.id(), c.projectId(), c.userId(), c.role(), c.createdAt());
        }
    }

    /**
     * 团队用量统计 DTO。
     */
    public record TeamUsageInfo(
            UUID teamId,
            int memberCount,
            long totalUsedChars,
            int totalUsedAdaptations,
            Map<String, QuotaInfo> memberUsage) {
    }

}
