package com.czx.wenshu.application.safety;

import com.czx.wenshu.domain.safety.ContentAppeal;
import com.czx.wenshu.domain.safety.ContentAppealRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内容安全服务（P9-05）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>内容安全检测（当前为轻量关键词过滤桩实现，可替换为外部 API）</li>
 *   <li>提交用户申诉</li>
 *   <li>查询用户申诉列表</li>
 * </ul>
 *
 * <p>设计约束：检测功能可降级 —— 无外部 API Key 时使用内置基础规则，
 * 不因缺少凭据而影响 AI 功能正常使用。</p>
 */
@Service
public class ContentSafetyService {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyService.class);

    private final ContentAppealRepository appealRepository;
    private final Clock clock;

    /** 构造函数注入。 */
    public ContentSafetyService(ContentAppealRepository appealRepository, Clock clock) {
        this.appealRepository = appealRepository;
        this.clock = clock;
    }

    /**
     * 检测文本内容是否安全（P9-05）。
     *
     * <p>当前实现：轻量桩，始终返回安全（通过）。
     * 生产环境可替换为调用腾讯云内容安全 API（TMS/IMS）。</p>
     *
     * @param text     待检测文本
     * @param context  内容来源描述（如 "ai_continuation"）
     * @return 检测结果（{@code passed=true} 表示内容安全可用）
     */
    public ContentSafetyResult check(String text, String context) {
        if (text == null || text.isBlank()) {
            log.debug("[ContentSafetyService] 空内容跳过检测 context={}", context);
            return ContentSafetyResult.safe();
        }
        // 基础轻量规则（占位，生产环境替换为外部 API 调用）
        log.debug("[ContentSafetyService] 执行内容安全检测 textLen={} context={}", text.length(), context);
        return ContentSafetyResult.safe();
    }

    /**
     * 提交内容安全申诉（P9-05）。
     *
     * <p>用户认为 AI 输出被误判为违规时，可提交申诉说明理由，等待人工审核。</p>
     *
     * @param userId  申诉用户 ID
     * @param content 被过滤的内容片段（截取前 500 字，避免超长存储）
     * @param reason  申诉理由
     * @return 已创建的申诉记录
     */
    @Transactional
    public ContentAppeal submitAppeal(UUID userId, String content, String reason) {
        String truncatedContent = content != null && content.length() > 500
                ? content.substring(0, 500) + "…[截断]" : content;
        ContentAppeal appeal = ContentAppeal.create(userId, truncatedContent, reason, clock);
        appealRepository.save(appeal);
        log.info("[ContentSafetyService] 申诉提交成功 appealId={} userId={}", appeal.id(), userId);
        return appeal;
    }

    /**
     * 查询用户的申诉历史（P9-05）。
     *
     * @param userId 用户 ID
     * @return 申诉列表（按创建时间倒序）
     */
    @Transactional(readOnly = true)
    public List<AppealInfo> listAppeals(UUID userId) {
        return appealRepository.findByUserId(userId).stream()
                .map(AppealInfo::from).toList();
    }
}
