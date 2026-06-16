package com.czx.wenshu.application.search;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ChapterSnapshot;
import com.czx.wenshu.domain.project.ChapterSnapshotRepository;
import com.czx.wenshu.domain.project.Character;
import com.czx.wenshu.domain.project.CharacterRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchApplicationService {

    private static final int CONTEXT_WINDOW = 30;

    private final ChapterRepository chapterRepository;
    private final ChapterSnapshotRepository snapshotRepository;
    private final CharacterRepository characterRepository;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public SearchApplicationService(ChapterRepository chapterRepository,
                                     ChapterSnapshotRepository snapshotRepository,
                                     CharacterRepository characterRepository,
                                     ProjectRepository projectRepository,
                                     Clock clock) {
        this.chapterRepository = chapterRepository;
        this.snapshotRepository = snapshotRepository;
        this.characterRepository = characterRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    // ── P4-04 全书搜索 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SearchResultInfo searchChapters(UUID projectId, UUID userId,
                                            String keyword, boolean caseSensitive, boolean wholeWord) {
        verifyProjectOwnership(projectId, userId);
        if (keyword == null || keyword.isBlank()) {
            return new SearchResultInfo(0, List.of());
        }
        Pattern pattern = buildPattern(keyword, caseSensitive, wholeWord);
        List<Chapter> chapters = chapterRepository.findByProjectId(projectId);
        List<ChapterSearchResult> results = new ArrayList<>();
        int total = 0;

        for (Chapter chapter : chapters) {
            List<MatchContext> matches = findMatches(chapter.content(), pattern);
            if (!matches.isEmpty()) {
                results.add(new ChapterSearchResult(
                        chapter.id().toString(), chapter.title(), matches.size(), matches));
                total += matches.size();
            }
        }
        return new SearchResultInfo(total, results);
    }

    // ── P4-05 全书替换（含快照保护）+ P4-06 角色名联动 ─────────────────────

    @Transactional
    public ReplaceResultInfo replaceInChapters(UUID projectId, UUID userId,
                                                String keyword, String replacement,
                                                boolean caseSensitive, boolean wholeWord,
                                                List<UUID> targetChapterIds,
                                                boolean syncCharacterName) {
        verifyProjectOwnership(projectId, userId);
        if (keyword == null || keyword.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "搜索关键词不能为空");
        }
        Pattern pattern = buildPattern(keyword, caseSensitive, wholeWord);
        List<Chapter> chapters = chapterRepository.findByProjectId(projectId);

        // 若指定了章节 ID，只替换指定章节
        if (targetChapterIds != null && !targetChapterIds.isEmpty()) {
            chapters = chapters.stream()
                    .filter(c -> targetChapterIds.contains(c.id()))
                    .toList();
        }

        List<AffectedChapterResult> affected = new ArrayList<>();
        int totalReplaced = 0;

        for (Chapter chapter : chapters) {
            String content = chapter.content();
            if (content == null || content.isBlank()) {
                continue;
            }
            Matcher matcher = pattern.matcher(content);
            if (!matcher.find()) {
                continue;
            }
            // P4-05: 替换前创建自动快照（auto_before_replace）
            ChapterSnapshot snapshot = ChapterSnapshot.create(
                    chapter.id(), content, chapter.wordCount(),
                    "auto_before_replace", "全书替换前自动快照 [" + keyword + "]", clock);
            snapshotRepository.save(snapshot);

            // 执行替换
            String replaced = pattern.matcher(content).replaceAll(
                    Matcher.quoteReplacement(replacement != null ? replacement : ""));
            int count = countOccurrences(content, pattern);
            chapter.saveContent(chapter.title(), replaced, chapter.outline(), chapter.status(), clock);
            chapterRepository.save(chapter);
            affected.add(new AffectedChapterResult(
                    chapter.id().toString(), chapter.title(), count, snapshot.id().toString()));
            totalReplaced += count;
        }

        // P4-06: 若指定同步角色名，且 keyword 与某角色名匹配，则更新角色档案
        boolean characterSynced = false;
        if (syncCharacterName && totalReplaced > 0 && replacement != null && !replacement.isBlank()) {
            characterSynced = syncMatchingCharacterNames(projectId, keyword, replacement, caseSensitive);
        }
        return new ReplaceResultInfo(totalReplaced, affected, characterSynced);
    }

    // ── 私有工具方法 ────────────────────────────────────────────────────────

    /** P4-06: 查找项目中名称与 keyword 相匹配的角色，将其改名为 replacement。 */
    private boolean syncMatchingCharacterNames(UUID projectId, String keyword,
                                                String replacement, boolean caseSensitive) {
        List<Character> characters = characterRepository.findByProjectId(projectId);
        boolean synced = false;
        for (Character character : characters) {
            boolean matches = caseSensitive
                    ? keyword.equals(character.name())
                    : keyword.equalsIgnoreCase(character.name());
            if (matches) {
                character.update(replacement, character.role(), character.appearance(),
                        character.personality(), character.abilities(),
                        character.speechStyle(), character.status(), clock);
                characterRepository.save(character);
                synced = true;
            }
        }
        return synced;
    }

    private Pattern buildPattern(String keyword, boolean caseSensitive, boolean wholeWord) {
        String escaped = Pattern.quote(keyword);
        String patternStr = wholeWord ? "\\b" + escaped + "\\b" : escaped;
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        return Pattern.compile(patternStr, flags);
    }

    private List<MatchContext> findMatches(String content, Pattern pattern) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<MatchContext> matches = new ArrayList<>();
        Matcher m = pattern.matcher(content);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            String before = content.substring(Math.max(0, start - CONTEXT_WINDOW), start);
            String match = content.substring(start, end);
            String after = content.substring(end, Math.min(content.length(), end + CONTEXT_WINDOW));
            matches.add(new MatchContext(before, match, after));
        }
        return matches;
    }

    private int countOccurrences(String content, Pattern pattern) {
        Matcher m = pattern.matcher(content);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private void verifyProjectOwnership(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
    }
}
