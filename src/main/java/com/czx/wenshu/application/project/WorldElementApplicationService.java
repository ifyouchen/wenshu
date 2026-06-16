package com.czx.wenshu.application.project;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldElementApplicationService {

    private final WorldElementRepository worldElementRepository;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public WorldElementApplicationService(WorldElementRepository worldElementRepository,
                                           ProjectRepository projectRepository, Clock clock) {
        this.worldElementRepository = worldElementRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Transactional
    public WorldElementInfo createWorldElement(UUID projectId, UUID userId, CreateWorldElementCommand command) {
        verifyProjectOwnership(projectId, userId);
        WorldElement element = WorldElement.create(projectId, command.type(), command.name(),
                command.description(), toJsonArray(command.aliases()), clock);
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