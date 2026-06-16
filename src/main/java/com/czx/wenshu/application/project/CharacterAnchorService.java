package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.Character;
import com.czx.wenshu.domain.project.CharacterRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色锚点自动提取（P6-02）。
 * 在章节保存/完成时调用，通过简单的字符串匹配检测角色名称出现，
 * 更新角色的 lastActiveChapterId（若为首次出现则同时设置 firstChapterId）。
 */
@Service
public class CharacterAnchorService {

    private final CharacterRepository characterRepository;
    private final Clock clock;

    public CharacterAnchorService(CharacterRepository characterRepository, Clock clock) {
        this.characterRepository = characterRepository;
        this.clock = clock;
    }

    /**
     * 扫描章节内容，更新项目中所有角色的锚点信息。
     * 字符名为空或内容为空时跳过。
     *
     * @param projectId 当前作品 ID
     * @param chapterId 当前章节 ID（锚点目标）
     * @param content   章节正文内容
     */
    @Transactional
    public void updateAnchors(UUID projectId, UUID chapterId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        List<Character> characters = characterRepository.findByProjectId(projectId);
        for (Character character : characters) {
            String name = character.name();
            if (name == null || name.isBlank()) {
                continue;
            }
            if (content.contains(name)) {
                character.updateAnchor(chapterId, clock);
                characterRepository.save(character);
            }
        }
    }
}
