package com.czx.wenshu.application.novel;

import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.Character;
import com.czx.wenshu.domain.project.CharacterRepository;
import com.czx.wenshu.domain.project.WorldElement;
import com.czx.wenshu.domain.project.WorldElementRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 动态上下文组装服务（P5-06）。
 * 按 Token 预算组装：锁定角色 > 锁定世界观 > 近期原文。
 * 锁定角色/世界观不受预算裁剪（优先注入）；近期原文在剩余预算内尽量多放。
 */
@Service
public class ContextAssemblyService {

    private static final Logger log = LoggerFactory.getLogger(ContextAssemblyService.class);

    /** 默认 Token 预算（约 2000 中文字符）。 */
    public static final int DEFAULT_TOKEN_BUDGET = 4000;

    /**
     * 简单 Token 估算：中文字符按 1.5 token，其余按 0.3 token（ASCII 约 4 char/token）。
     */
    private static int estimateTokens(String text) {
        if (text == null) return 0;
        int total = 0;
        for (char c : text.toCharArray()) {
            total += c > 0x4E00 ? 2 : 1;   // 中文字符约 2 token，其他约 1
        }
        return total / 2;
    }

    private final CharacterRepository characterRepository;
    private final WorldElementRepository worldElementRepository;
    private final ChapterRepository chapterRepository;

    public ContextAssemblyService(CharacterRepository characterRepository,
                                   WorldElementRepository worldElementRepository,
                                   ChapterRepository chapterRepository) {
        this.characterRepository = characterRepository;
        this.worldElementRepository = worldElementRepository;
        this.chapterRepository = chapterRepository;
    }

    /**
     * 组装上下文包。
     *
     * @param projectId   当前作品 ID
     * @param currentChapterId 当前正在编辑的章节（用于确定「近期」范围，可为 null）
     * @param tokenBudget Token 预算（0 表示使用默认值）
     */
    @Transactional(readOnly = true)
    public ContextBundle assemble(UUID projectId, UUID currentChapterId, int tokenBudget) {
        int budget = tokenBudget > 0 ? tokenBudget : DEFAULT_TOKEN_BUDGET;

        // 1. 锁定角色档案（始终注入，不受预算裁剪）
        List<Character> lockedChars = characterRepository.findByProjectId(projectId).stream()
                .filter(Character::locked)
                .toList();
        String charContext = buildCharacterContext(lockedChars);

        // 2. 锁定世界观元素（始终注入）
        List<WorldElement> lockedElements = worldElementRepository.findByProjectId(projectId).stream()
                .filter(WorldElement::locked)
                .toList();
        String worldContext = buildWorldContext(lockedElements);

        // 3. 近期章节内容（按预算截断）
        int usedTokens = estimateTokens(charContext) + estimateTokens(worldContext);
        int remainingBudget = Math.max(0, budget - usedTokens);
        String recentContent = buildRecentContent(projectId, currentChapterId, remainingBudget);

        // 4. 组装 systemContext
        StringBuilder sys = new StringBuilder();
        if (!charContext.isBlank()) sys.append(charContext);
        if (!worldContext.isBlank()) {
            if (sys.length() > 0) sys.append("\n\n");
            sys.append(worldContext);
        }

        int totalTokens = estimateTokens(sys.toString()) + estimateTokens(recentContent);
        log.debug("[ContextAssemblyService] 上下文组装完成 projectId={} 预算Token={} 实际Token={} 锁定角色={} 锁定设定={} 包含最近章节数={}",
                projectId, budget, totalTokens, lockedChars.size(), lockedElements.size(),
                recentContent.isBlank() ? 0 : countIncludedChapters(recentContent));
        return new ContextBundle(sys.toString(), recentContent, totalTokens,
                lockedChars.size(), lockedElements.size(),
                recentContent.isBlank() ? 0 : countIncludedChapters(recentContent));
    }

    // ── 格式化工具 ─────────────────────────────────────────────────────────

    private String buildCharacterContext(List<Character> chars) {
        if (chars.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("【锁定角色档案】\n");
        for (Character c : chars) {
            sb.append("▸ ").append(c.name())
              .append("（").append(c.role() != null ? c.role() : "").append("）");
            if (c.personality() != null && !c.personality().isBlank()) {
                sb.append("\n  性格：").append(c.personality());
            }
            if (c.speechStyle() != null && !c.speechStyle().isBlank()) {
                sb.append("\n  说话风格：").append(c.speechStyle());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String buildWorldContext(List<WorldElement> elements) {
        if (elements.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("【世界观设定】\n");
        for (WorldElement e : elements) {
            sb.append("▸ ").append(e.name())
              .append("（").append(e.type() != null ? e.type() : "").append("）");
            if (e.description() != null && !e.description().isBlank()) {
                sb.append("：").append(e.description());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String buildRecentContent(UUID projectId, UUID currentChapterId, int remainingBudget) {
        if (remainingBudget <= 0) return "";
        List<Chapter> allChapters = chapterRepository.findByProjectId(projectId);
        if (allChapters.isEmpty()) return "";

        // 按 sortOrder 排序，取当前章节之前的最近几章
        allChapters = allChapters.stream()
                .filter(c -> !c.id().equals(currentChapterId))
                .sorted(Comparator.comparingInt(Chapter::sortOrder).reversed())
                .toList();

        StringBuilder sb = new StringBuilder();
        int usedTokens = 0;
        int includedCount = 0;
        for (Chapter chapter : allChapters) {
            String content = chapter.content();
            if (content == null || content.isBlank()) continue;
            int chapterTokens = estimateTokens(content);
            if (usedTokens + chapterTokens > remainingBudget) {
                // 部分截断：取能放下的前缀
                int availableChars = (remainingBudget - usedTokens) * 2;
                if (availableChars > 100) {
                    String truncated = content.substring(0, Math.min(availableChars, content.length()));
                    sb.insert(0, truncated + "\n---\n");
                    includedCount++;
                }
                break;
            }
            sb.insert(0, content + "\n---\n");
            usedTokens += chapterTokens;
            includedCount++;
        }
        return sb.toString().trim();
    }

    private int countIncludedChapters(String recentContent) {
        if (recentContent.isBlank()) return 0;
        return recentContent.split("---").length;
    }
}
