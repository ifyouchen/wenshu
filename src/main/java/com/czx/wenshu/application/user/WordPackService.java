package com.czx.wenshu.application.user;

import com.czx.wenshu.domain.user.WordPack;
import com.czx.wenshu.domain.user.WordPackRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 字数包服务（P9-09）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>为新注册用户发放体验额度（5 万字，自动）</li>
 *   <li>为完成支付的用户发放字数包（topup）</li>
 *   <li>查询用户字数包剩余量（叠加到月度配额展示）</li>
 *   <li>消耗字数包（月度配额耗尽后按字数包优先扣减）</li>
 * </ul>
 */
@Service
public class WordPackService {

    private static final Logger log = LoggerFactory.getLogger(WordPackService.class);

    private final WordPackRepository wordPackRepository;
    private final Clock clock;

    /** 构造函数注入。 */
    public WordPackService(WordPackRepository wordPackRepository, Clock clock) {
        this.wordPackRepository = wordPackRepository;
        this.clock = clock;
    }

    /**
     * 为新用户发放体验额度（P9-09）。
     *
     * <p>每个用户只发放一次，重复调用静默跳过。</p>
     *
     * @param userId 新用户 ID
     */
    @Transactional
    public void issueTrial(UUID userId) {
        if (wordPackRepository.existsTrialByUserId(userId)) {
            log.debug("[WordPackService] 用户 {} 已有体验额度，跳过发放", userId);
            return;
        }
        WordPack trial = WordPack.createTrial(userId, clock);
        wordPackRepository.save(trial);
        log.info("[WordPackService] 体验额度已发放 userId={} chars={}", userId, WordPack.TRIAL_CHARS);
    }

    /**
     * 发放购买的字数包（P9-09）。
     *
     * <p>由 {@link com.czx.wenshu.application.payment.PaymentService} 在支付成功后调用。</p>
     *
     * @param userId     购买用户 ID
     * @param packKey    字数包 key（如 topup_100k）
     * @param charsTotal 包含字符数
     */
    @Transactional
    public void issueTopup(UUID userId, String packKey, long charsTotal) {
        WordPack pack = WordPack.create(userId, packKey, "topup", charsTotal, clock);
        wordPackRepository.save(pack);
        log.info("[WordPackService] 字数包已发放 userId={} packKey={} chars={}", userId, packKey, charsTotal);
    }

    /**
     * 查询用户字数包剩余总量（P9-09）。
     *
     * <p>汇总所有有效字数包（trial + topup）的剩余字符数。</p>
     *
     * @param userId 用户 ID
     * @return 字数包剩余字符数总量
     */
    @Transactional(readOnly = true)
    public long getTotalRemainingChars(UUID userId) {
        return wordPackRepository.findActiveByUserId(userId).stream()
                .mapToLong(WordPack::remainingChars)
                .sum();
    }

    /**
     * 从字数包消耗指定字符数（P9-09）。
     *
     * <p>按创建时间（FIFO）顺序消耗，优先消耗最早购买的包。
     * 返回实际从字数包中扣减的字符数。</p>
     *
     * @param userId 用户 ID
     * @param chars  请求消耗的字符数
     * @return 实际从字数包消耗的字符数（可能 < chars，当字数包不足时）
     */
    @Transactional
    public long consumeFromPacks(UUID userId, long chars) {
        if (chars <= 0) return 0;
        List<WordPack> packs = wordPackRepository.findActiveByUserId(userId);
        long remaining = chars;
        for (WordPack pack : packs) {
            if (remaining <= 0) break;
            long consumed = pack.consume(remaining, clock);
            wordPackRepository.save(pack);
            remaining -= consumed;
            log.debug("[WordPackService] 字数包消耗 packId={} consumed={} remaining={}",
                    pack.id(), consumed, remaining);
        }
        long totalConsumed = chars - remaining;
        log.info("[WordPackService] 字数包消耗完成 userId={} requested={} consumed={}",
                userId, chars, totalConsumed);
        return totalConsumed;
    }

    /**
     * 查询用户所有有效字数包列表（P9-09）。
     *
     * @param userId 用户 ID
     * @return 有效字数包列表（按创建时间升序）
     */
    @Transactional(readOnly = true)
    public List<WordPack> listActivePacks(UUID userId) {
        return wordPackRepository.findActiveByUserId(userId);
    }
}
