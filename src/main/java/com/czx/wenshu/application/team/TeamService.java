package com.czx.wenshu.application.team;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.team.Team;
import com.czx.wenshu.domain.team.TeamMember;
import com.czx.wenshu.domain.team.TeamMemberRepository;
import com.czx.wenshu.domain.team.TeamRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 团队版成员管理服务（P9-07）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>创建团队（创建者自动成为 admin 成员）</li>
 *   <li>邀请成员（生成邀请码）</li>
 *   <li>接受邀请（通过邀请码加入）</li>
 *   <li>查询成员列表</li>
 *   <li>移除成员（admin 权限）</li>
 *   <li>修改成员角色（admin 权限）</li>
 *   <li>共享配额：成员使用量从团队配额池中扣减</li>
 * </ul>
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final Clock clock;

    /** 构造函数注入。 */
    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository memberRepository,
                       Clock clock) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    /**
     * 创建团队（P9-07）。
     *
     * <p>创建者自动成为 admin 成员（active 状态）。</p>
     *
     * @param ownerId   创建者用户 ID
     * @param teamName  团队名称
     * @return 创建的团队摘要
     */
    @Transactional
    public TeamInfo createTeam(UUID ownerId, String teamName) {
        log.info("[TeamService] 创建团队 ownerId={} name={}", ownerId, teamName);
        Team team = Team.create(ownerId, teamName, clock);
        teamRepository.save(team);

        // 创建者自动成为 admin 成员
        TeamMember owner = TeamMember.createOwner(team.id(), ownerId, clock);
        memberRepository.save(owner);

        log.info("[TeamService] 团队创建成功 teamId={}", team.id());
        return TeamInfo.from(team, 1);
    }

    /**
     * 查询用户所在的团队列表（P9-07）。
     *
     * @param userId 用户 ID
     * @return 团队列表
     */
    @Transactional(readOnly = true)
    public List<TeamInfo> listMyTeams(UUID userId) {
        return teamRepository.findByMemberUserId(userId).stream()
                .map(t -> {
                    int memberCount = memberRepository.findActiveByTeamId(t.id()).size();
                    return TeamInfo.from(t, memberCount);
                }).toList();
    }

    /**
     * 邀请成员加入团队（P9-07）。
     *
     * <p>仅 admin 可邀请。返回含邀请码的成员信息。</p>
     *
     * @param teamId    团队 ID
     * @param adminId   邀请人（需为团队 admin）
     * @param inviteeId 受邀用户 ID
     * @return 邀请记录（含 inviteCode，发给受邀用户）
     */
    @Transactional
    public MemberInfo invite(UUID teamId, UUID adminId, UUID inviteeId) {
        log.info("[TeamService] 邀请成员 teamId={} adminId={} inviteeId={}", teamId, adminId, inviteeId);
        // 验证邀请人权限
        requireAdmin(teamId, adminId);
        // 检查受邀用户是否已在团队
        memberRepository.findByTeamIdAndUserId(teamId, inviteeId)
                .filter(m -> !"removed".equals(m.status()))
                .ifPresent(m -> {
                    throw new ApiException(ErrorCode.BAD_REQUEST, "该用户已在团队中");
                });
        // 生成唯一邀请码
        String inviteCode = "INV" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        TeamMember invite = TeamMember.invite(teamId, inviteeId, adminId, inviteCode, clock);
        memberRepository.save(invite);
        log.info("[TeamService] 邀请已发送 inviteCode={}", inviteCode);
        return MemberInfo.from(invite);
    }

    /**
     * 接受邀请（P9-07）。
     *
     * <p>受邀用户通过邀请码加入团队，状态改为 active。</p>
     *
     * @param inviteCode 邀请码
     * @param userId     当前用户 ID（必须与邀请目标一致）
     * @return 已更新的成员信息
     */
    @Transactional
    public MemberInfo acceptInvite(String inviteCode, UUID userId) {
        log.info("[TeamService] 接受邀请 inviteCode={} userId={}", inviteCode, userId);
        TeamMember invite = memberRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "邀请码不存在或已失效"));
        if (!"pending".equals(invite.status())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "邀请码已使用或已失效");
        }
        if (!invite.userId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "邀请码不属于当前用户");
        }
        invite.accept(clock);
        memberRepository.save(invite);
        log.info("[TeamService] 用户 {} 加入团队 {}", userId, invite.teamId());
        return MemberInfo.from(invite);
    }

    /**
     * 查询团队成员列表（P9-07）。
     *
     * @param teamId  团队 ID
     * @param userId  当前用户 ID（需为团队成员）
     * @return 成员列表
     */
    @Transactional(readOnly = true)
    public List<MemberInfo> listMembers(UUID teamId, UUID userId) {
        requireMember(teamId, userId);
        return memberRepository.findAllByTeamId(teamId).stream()
                .map(MemberInfo::from).toList();
    }

    /**
     * 移除团队成员（P9-07）。
     *
     * @param teamId   团队 ID
     * @param adminId  操作者（需为 admin）
     * @param memberId 被移除成员的 ID（team_members 主键）
     */
    @Transactional
    public void removeMember(UUID teamId, UUID adminId, UUID memberId) {
        log.info("[TeamService] 移除成员 teamId={} adminId={} memberId={}", teamId, adminId, memberId);
        requireAdmin(teamId, adminId);
        // 查找要移除的成员记录（这里 memberId 是 team_members.user_id）
        TeamMember member = memberRepository.findByTeamIdAndUserId(teamId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "成员不存在"));
        if ("admin".equals(member.role())) {
            // 检查是否为 owner（不允许移除所有者）
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "团队不存在"));
            if (team.ownerId().equals(memberId)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "不能移除团队所有者");
            }
        }
        member.remove();
        memberRepository.save(member);
        log.info("[TeamService] 成员已移除 userId={}", memberId);
    }

    /**
     * 修改成员角色（P9-07）。
     *
     * @param teamId   团队 ID
     * @param adminId  操作者（需为 admin）
     * @param targetId 被修改成员的用户 ID
     * @param newRole  新角色（admin/member）
     */
    @Transactional
    public void changeRole(UUID teamId, UUID adminId, UUID targetId, String newRole) {
        log.info("[TeamService] 修改成员角色 teamId={} adminId={} targetId={} newRole={}",
                teamId, adminId, targetId, newRole);
        requireAdmin(teamId, adminId);
        if (!"admin".equals(newRole) && !"member".equals(newRole)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "角色只能是 admin 或 member");
        }
        TeamMember member = memberRepository.findByTeamIdAndUserId(teamId, targetId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "成员不存在"));
        member.changeRole(newRole);
        memberRepository.save(member);
        log.info("[TeamService] 角色已更新 userId={} newRole={}", targetId, newRole);
    }

    // ── 私有辅助方法 ─────────────────────────────────────────────────────────

    /**
     * 验证用户为团队 admin。
     */
    private void requireAdmin(UUID teamId, UUID userId) {
        TeamMember member = memberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "非团队成员"));
        if (!"admin".equals(member.role()) || !"active".equals(member.status())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "需要团队管理员权限");
        }
    }

    /**
     * 验证用户为团队成员（active 状态）。
     */
    private void requireMember(UUID teamId, UUID userId) {
        TeamMember member = memberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "非团队成员"));
        if (!"active".equals(member.status())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "非团队活跃成员");
        }
    }
}
