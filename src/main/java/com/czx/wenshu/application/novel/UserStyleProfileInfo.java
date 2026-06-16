package com.czx.wenshu.application.novel;

import com.czx.wenshu.domain.user.UserStyleProfile;
import java.util.List;

/** 文风档案响应 DTO（P5-10）。 */
public record UserStyleProfileInfo(
        String userId,
        List<String> styleTags,
        String analysisTaskId,
        boolean analysisCompleted,
        String updatedAt) {

    public static UserStyleProfileInfo from(UserStyleProfile profile, List<String> parsedTags) {
        return new UserStyleProfileInfo(
                profile.userId().toString(),
                parsedTags,
                profile.analysisTaskId() != null ? profile.analysisTaskId().toString() : null,
                !parsedTags.isEmpty(),
                profile.updatedAt().toString()
        );
    }
}
