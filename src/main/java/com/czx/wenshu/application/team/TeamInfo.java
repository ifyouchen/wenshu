package com.czx.wenshu.application.team;

import com.czx.wenshu.domain.team.Team;
import java.util.UUID;

/**
 * 团队摘要 DTO（P9-07）。
 */
public record TeamInfo(
        /** 团队主键。 */
        UUID id,
        /** 所有者 ID。 */
        UUID ownerId,
        /** 团队名称。 */
        String name,
        /** 套餐 key。 */
        String planKey,
        /** 月度字符额度。 */
        long monthlyCharLimit,
        /** 月度改编次数额度。 */
        int monthlyAdaptationLimit,
        /** 活跃成员数。 */
        int memberCount) {

    /** 从领域对象创建 DTO。 */
    public static TeamInfo from(Team team, int memberCount) {
        return new TeamInfo(team.id(), team.ownerId(), team.name(), team.planKey(),
                team.monthlyCharLimit(), team.monthlyAdaptationLimit(), memberCount);
    }
}
