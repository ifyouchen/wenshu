package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.ProjectCollaborator;
import com.czx.wenshu.domain.project.ProjectCollaboratorRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link ProjectCollaboratorRepository} 的 MyBatis 实现（P9-08）。
 */
@Repository
public class MyBatisProjectCollaboratorRepository implements ProjectCollaboratorRepository {

    private final ProjectCollaboratorMapper mapper;

    public MyBatisProjectCollaboratorRepository(ProjectCollaboratorMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ProjectCollaborator collab) {
        mapper.insert(toRecord(collab));
    }

    @Override
    public void deleteByProjectIdAndUserId(UUID projectId, UUID userId) {
        mapper.deleteByProjectIdAndUserId(projectId.toString(), userId.toString());
    }

    @Override
    public Optional<ProjectCollaborator> findByProjectIdAndUserId(UUID projectId, UUID userId) {
        ProjectCollaboratorMapper.CollaboratorRecord rec =
                mapper.findByProjectIdAndUserId(projectId.toString(), userId.toString());
        return Optional.ofNullable(rec).map(this::toDomain);
    }

    @Override
    public List<ProjectCollaborator> findByProjectId(UUID projectId) {
        return mapper.findByProjectId(projectId.toString()).stream()
                .map(this::toDomain).toList();
    }

    private ProjectCollaborator toDomain(ProjectCollaboratorMapper.CollaboratorRecord rec) {
        return ProjectCollaborator.rehydrate(
                UUID.fromString(rec.getId()),
                UUID.fromString(rec.getProjectId()),
                UUID.fromString(rec.getUserId()),
                rec.getRole(),
                rec.getAddedBy() != null ? UUID.fromString(rec.getAddedBy()) : null,
                rec.getCreatedAt());
    }

    private ProjectCollaboratorMapper.CollaboratorRecord toRecord(ProjectCollaborator collab) {
        ProjectCollaboratorMapper.CollaboratorRecord rec = new ProjectCollaboratorMapper.CollaboratorRecord();
        rec.setId(collab.id().toString());
        rec.setProjectId(collab.projectId().toString());
        rec.setUserId(collab.userId().toString());
        rec.setRole(collab.role());
        rec.setAddedBy(collab.addedBy() != null ? collab.addedBy().toString() : null);
        rec.setCreatedAt(collab.createdAt());
        return rec;
    }
}
