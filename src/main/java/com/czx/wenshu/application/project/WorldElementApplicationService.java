package com.czx.wenshu.application.project;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.WorldElement;
import com.czx.wenshu.domain.project.WorldElementRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldElementApplicationService {

    private final WorldElementRepository worldElementRepository;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public WorldElementApplicationService(WorldElementRepository worldElementRepository, ProjectRepository projectRepository, Clock clock) {
        this.worldElementRepository = worldElementRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Transactional
    public WorldElementInfo createWorldElement(UUID projectId, UUID userId, CreateWorldElementCommand command) {
        verifyProjectOwnership(projectId, userId);
        WorldElement element = WorldElement.create(projectId, command.type(), command.name(), command.description(), clock);
        worldElementRepository.save(element);
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
        element.update(command.type(), command.name(), command.description());
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

    private void verifyProjectOwnership(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
    }
}