package com.czx.wenshu.infrastructure.persistence.team;

import com.czx.wenshu.domain.team.Team;
import com.czx.wenshu.domain.team.TeamRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link TeamRepository} 的 MyBatis 实现（P9-07）。
 */
@Repository
public class MyBatisTeamRepository implements TeamRepository {

    private final TeamMapper mapper;

    public MyBatisTeamRepository(TeamMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(Team team) {
        TeamRecord rec = toRecord(team);
        if (mapper.findById(team.id().toString()) == null) {
            mapper.insert(rec);
        } else {
            mapper.update(rec);
        }
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<Team> findByMemberUserId(UUID userId) {
        return mapper.findByMemberUserId(userId.toString()).stream().map(this::toDomain).toList();
    }

    private Team toDomain(TeamRecord rec) {
        return Team.rehydrate(
                UUID.fromString(rec.getId()),
                UUID.fromString(rec.getOwnerId()),
                rec.getName(), rec.getPlanKey(),
                rec.getMonthlyCharLimit(), rec.getMonthlyAdaptationLimit(),
                rec.getCreatedAt(), rec.getUpdatedAt());
    }

    private TeamRecord toRecord(Team team) {
        TeamRecord rec = new TeamRecord();
        rec.setId(team.id().toString());
        rec.setOwnerId(team.ownerId().toString());
        rec.setName(team.name());
        rec.setPlanKey(team.planKey());
        rec.setMonthlyCharLimit(team.monthlyCharLimit());
        rec.setMonthlyAdaptationLimit(team.monthlyAdaptationLimit());
        rec.setCreatedAt(team.createdAt());
        rec.setUpdatedAt(team.updatedAt());
        return rec;
    }
}
