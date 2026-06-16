package com.czx.wenshu.infrastructure.persistence.team;

import com.czx.wenshu.domain.team.TeamMember;
import com.czx.wenshu.domain.team.TeamMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link TeamMemberRepository} 的 MyBatis 实现（P9-07）。
 */
@Repository
public class MyBatisTeamMemberRepository implements TeamMemberRepository {

    private final TeamMemberMapper mapper;

    public MyBatisTeamMemberRepository(TeamMemberMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(TeamMember member) {
        TeamMemberRecord rec = toRecord(member);
        // 按 teamId+userId 判断是否存在
        boolean exists = mapper.findByTeamIdAndUserId(
                member.teamId().toString(), member.userId().toString()) != null;
        if (!exists) {
            mapper.insert(rec);
        } else {
            mapper.update(rec);
        }
    }

    @Override
    public Optional<TeamMember> findByInviteCode(String inviteCode) {
        return Optional.ofNullable(mapper.findByInviteCode(inviteCode)).map(this::toDomain);
    }

    @Override
    public Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId) {
        return Optional.ofNullable(
                mapper.findByTeamIdAndUserId(teamId.toString(), userId.toString()))
                .map(this::toDomain);
    }

    @Override
    public List<TeamMember> findActiveByTeamId(UUID teamId) {
        return mapper.findActiveByTeamId(teamId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<TeamMember> findAllByTeamId(UUID teamId) {
        return mapper.findAllByTeamId(teamId.toString()).stream().map(this::toDomain).toList();
    }

    private TeamMember toDomain(TeamMemberRecord rec) {
        return TeamMember.rehydrate(
                UUID.fromString(rec.getId()),
                UUID.fromString(rec.getTeamId()),
                UUID.fromString(rec.getUserId()),
                rec.getRole(), rec.getStatus(),
                rec.getInvitedBy() != null ? UUID.fromString(rec.getInvitedBy()) : null,
                rec.getInviteCode(),
                rec.getJoinedAt(), rec.getCreatedAt());
    }

    private TeamMemberRecord toRecord(TeamMember member) {
        TeamMemberRecord rec = new TeamMemberRecord();
        rec.setId(member.id().toString());
        rec.setTeamId(member.teamId().toString());
        rec.setUserId(member.userId().toString());
        rec.setRole(member.role());
        rec.setStatus(member.status());
        rec.setInvitedBy(member.invitedBy() != null ? member.invitedBy().toString() : null);
        rec.setInviteCode(member.inviteCode());
        rec.setJoinedAt(member.joinedAt());
        rec.setCreatedAt(member.createdAt());
        return rec;
    }
}
