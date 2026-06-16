package com.czx.wenshu.application.stats;

import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.stats.WritingDailyStats;
import com.czx.wenshu.domain.stats.WritingDailyStatsRepository;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WritingStatsQueryService {

    private final WritingDailyStatsRepository statsRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public WritingStatsQueryService(WritingDailyStatsRepository statsRepository,
                                     ProjectRepository projectRepository,
                                     UserRepository userRepository,
                                     Clock clock) {
        this.statsRepository = statsRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // ── P4-07 写作统计总览 ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WritingOverviewInfo getOverview(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate sevenDaysAgo = today.minusDays(6);
        LocalDate yearAgo = today.minusDays(364);

        // 近 365 天数据（用于 streak 和 all-time）
        List<WritingDailyStats> yearData = statsRepository.findByUserIdAndStatDateBetween(userId, yearAgo, today);

        // 今日字数
        int todayChars = yearData.stream()
                .filter(s -> s.statDate().equals(today))
                .mapToInt(WritingDailyStats::totalChars).sum();

        // 用户全局日目标
        int todayGoal = userRepository.findById(userId)
                .map(User::dailyCharGoal).orElse(2000);

        double progress = todayGoal > 0 ? Math.min(100.0, todayChars * 100.0 / todayGoal) : 0.0;

        // 7 天趋势（含今天）
        Map<LocalDate, Integer> dateToChars = yearData.stream()
                .collect(Collectors.toMap(WritingDailyStats::statDate,
                        WritingDailyStats::totalChars, Integer::sum));
        List<DailyStatEntry> trend = new ArrayList<>();
        for (LocalDate d = sevenDaysAgo; !d.isAfter(today); d = d.plusDays(1)) {
            int manual = 0;
            int total = dateToChars.getOrDefault(d, 0);
            // 对 manual 也做同样处理
            LocalDate finalD = d;
            manual = yearData.stream()
                    .filter(s -> s.statDate().equals(finalD))
                    .mapToInt(WritingDailyStats::manualChars).sum();
            trend.add(new DailyStatEntry(d.toString(), manual, total));
        }

        // 连续写作天数（streak）
        Set<LocalDate> writingDates = yearData.stream()
                .filter(s -> s.totalChars() > 0)
                .map(WritingDailyStats::statDate)
                .collect(Collectors.toSet());
        int streak = calcStreak(writingDates, today);

        // 全量字数（365 天内）
        long totalAllTime = yearData.stream().mapToInt(WritingDailyStats::totalChars).sum();

        return new WritingOverviewInfo(todayChars, todayGoal,
                Math.round(progress * 10.0) / 10.0, streak, totalAllTime, trend);
    }

    // ── P4-08 热力图 ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HeatmapEntry> getHeatmap(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(364);

        List<WritingDailyStats> data = statsRepository.findByUserIdAndStatDateBetween(userId, from, today);
        Map<LocalDate, Integer> dateToChars = data.stream()
                .collect(Collectors.toMap(WritingDailyStats::statDate,
                        WritingDailyStats::totalChars, Integer::sum));

        List<HeatmapEntry> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            result.add(new HeatmapEntry(d.toString(), dateToChars.getOrDefault(d, 0)));
        }
        return result;
    }

    // ── P4-08 作品进度 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectProgressInfo> getProjectProgress(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        List<WritingDailyStats> todayStats = statsRepository.findByUserIdAndStatDateBetween(userId, today, today);
        Map<UUID, Integer> projectTodayChars = todayStats.stream()
                .filter(s -> s.projectId() != null)
                .collect(Collectors.toMap(WritingDailyStats::projectId,
                        WritingDailyStats::totalChars, Integer::sum));

        return projectRepository.findByUserId(userId).stream()
                .map(p -> new ProjectProgressInfo(
                        p.id().toString(), p.title(), p.totalWords(),
                        p.dailyCharGoal(),
                        projectTodayChars.getOrDefault(p.id(), 0)))
                .toList();
    }

    // ── P4-08 月度摘要 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MonthlySummaryInfo getMonthlySummary(UUID userId, String yearMonthStr) {
        YearMonth ym = YearMonth.parse(yearMonthStr);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<WritingDailyStats> data = statsRepository.findByUserIdAndStatDateBetween(userId, from, to);
        Map<LocalDate, Integer> dateToChars = data.stream()
                .collect(Collectors.toMap(WritingDailyStats::statDate,
                        WritingDailyStats::totalChars, Integer::sum));

        int totalChars = dateToChars.values().stream().mapToInt(Integer::intValue).sum();
        int activeDays = (int) dateToChars.values().stream().filter(v -> v > 0).count();
        int avg = activeDays > 0 ? totalChars / activeDays : 0;

        List<HeatmapEntry> breakdown = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            breakdown.add(new HeatmapEntry(d.toString(), dateToChars.getOrDefault(d, 0)));
        }
        return new MonthlySummaryInfo(yearMonthStr, totalChars, activeDays, avg, breakdown);
    }

    // ── 工具方法 ───────────────────────────────────────────────────────────

    private int calcStreak(Set<LocalDate> writingDates, LocalDate today) {
        int streak = 0;
        LocalDate check = today;
        // 从今天往前数连续写作天
        while (writingDates.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        // 若今天未写，从昨天开始数
        if (streak == 0) {
            check = today.minusDays(1);
            while (writingDates.contains(check)) {
                streak++;
                check = check.minusDays(1);
            }
        }
        return streak;
    }
}
