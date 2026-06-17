package com.czx.wenshu.application.project;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterKeyEventRepository;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.CharacterRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.WorldElementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterContextService {

    private final ChapterRepository chapterRepository;
    private final ProjectRepository projectRepository;
    private final CharacterRepository characterRepository;
    private final WorldElementRepository worldElementRepository;
    private final ChapterKeyEventRepository keyEventRepository;
    private final ObjectMapper objectMapper;

    public ChapterContextService(ChapterRepository chapterRepository,
                                 ProjectRepository projectRepository,
                                 CharacterRepository characterRepository,
                                 WorldElementRepository worldElementRepository,
                                 ChapterKeyEventRepository keyEventRepository,
                                 ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.projectRepository = projectRepository;
        this.characterRepository = characterRepository;
        this.worldElementRepository = worldElementRepository;
        this.keyEventRepository = keyEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ChapterContextInfo getContext(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        return new ChapterContextInfo(
                ChapterInfo.from(chapter),
                characterRepository.findByProjectId(chapter.projectId()).stream().map(CharacterInfo::from).toList(),
                worldElementRepository.findByProjectId(chapter.projectId()).stream().map(WorldElementInfo::from).toList(),
                keyEventRepository.findByChapterId(chapter.id()).stream()
                        .map(event -> KeyEventInfo.from(event, objectMapper)).toList());
    }
}
