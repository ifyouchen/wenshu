package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisProjectRepository implements ProjectRepository {

    private final ProjectMapper projectMapper;

    public MyBatisProjectRepository(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public Project save(Project project) {
        ProjectRecord record = toRecord(project);
        if (projectMapper.findById(project.id().toString()) == null) {
            projectMapper.insert(record);
        } else {
            projectMapper.update(record);
        }
        return project;
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return Optional.ofNullable(projectMapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<Project> findByUserId(UUID userId) {
        return projectMapper.findByUserId(userId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        projectMapper.deleteById(id.toString());
    }

    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        return projectMapper.existsByIdAndUserId(id.toString(), userId.toString());
    }

    private Project toDomain(ProjectRecord r) {
        return Project.rehydrate(UUID.fromString(r.getId()), UUID.fromString(r.getUserId()), r.getTitle(), r.getGenre(),
                r.getSynopsis(), r.getWorldview(), r.getTotalWords(), r.getDailyCharGoal(), ProjectStatus.fromValue(r.getStatus()),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private ProjectRecord toRecord(Project p) {
        ProjectRecord r = new ProjectRecord();
        r.setId(p.id().toString());
        r.setUserId(p.userId().toString());
        r.setTitle(p.title());
        r.setGenre(p.genre());
        r.setSynopsis(p.synopsis());
        r.setWorldview(p.worldview());
        r.setTotalWords(p.totalWords());
        r.setDailyCharGoal(p.dailyCharGoal());
        r.setStatus(p.status().value());
        r.setCreatedAt(p.createdAt());
        r.setUpdatedAt(p.updatedAt());
        return r;
    }
}