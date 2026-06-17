package com.czx.wenshu.application.project;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Character;
import com.czx.wenshu.domain.project.CharacterRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterApplicationService {

    private final CharacterRepository characterRepository;
    private final ProjectRepository projectRepository;
    private final WorldElementApplicationService worldElementApplicationService;
    private final Clock clock;

    public CharacterApplicationService(CharacterRepository characterRepository,
                                        ProjectRepository projectRepository,
                                        WorldElementApplicationService worldElementApplicationService,
                                        Clock clock) {
        this.characterRepository = characterRepository;
        this.projectRepository = projectRepository;
        this.worldElementApplicationService = worldElementApplicationService;
        this.clock = clock;
    }

    @Transactional
    public CharacterInfo createCharacter(UUID projectId, UUID userId, CreateCharacterCommand command) {
        verifyProjectOwnership(projectId, userId);
        Character character = Character.create(projectId, command.name(), command.role(),
                command.appearance(), command.personality(), command.abilities(),
                command.speechStyle(), command.status(), clock);
        characterRepository.save(character);
        return CharacterInfo.from(character);
    }

    @Transactional(readOnly = true)
    public List<CharacterInfo> listCharacters(UUID projectId, UUID userId) {
        verifyProjectOwnership(projectId, userId);
        return characterRepository.findByProjectId(projectId).stream().map(CharacterInfo::from).toList();
    }

    @Transactional(readOnly = true)
    public CharacterInfo getCharacter(UUID characterId, UUID userId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "角色不存在"));
        verifyProjectOwnership(character.projectId(), userId);
        return CharacterInfo.from(character);
    }

    @Transactional
    public CharacterInfo updateCharacter(UUID characterId, UUID userId, UpdateCharacterCommand command) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "角色不存在"));
        verifyProjectOwnership(character.projectId(), userId);
        String oldName = character.name();
        character.update(command.name(), command.role(), command.appearance(), command.personality(),
                command.abilities(), command.speechStyle(), command.status(), clock);
        characterRepository.save(character);
        // P3-05：若角色名发生变化，同步词典中同名条目
        if (command.name() != null && !command.name().equals(oldName)) {
            worldElementApplicationService.syncCharacterName(character.projectId(), oldName, character.name());
        }
        return CharacterInfo.from(character);
    }

    @Transactional
    public void deleteCharacter(UUID characterId, UUID userId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "角色不存在"));
        verifyProjectOwnership(character.projectId(), userId);
        characterRepository.deleteById(characterId);
    }

    @Transactional
    public CharacterInfo toggleLock(UUID characterId, UUID userId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "角色不存在"));
        verifyProjectOwnership(character.projectId(), userId);
        character.toggleLock(clock);
        characterRepository.save(character);
        return CharacterInfo.from(character);
    }

    private void verifyProjectOwnership(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
    }
}
