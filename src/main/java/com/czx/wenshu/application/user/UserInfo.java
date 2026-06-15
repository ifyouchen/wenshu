package com.czx.wenshu.application.user;

import com.czx.wenshu.domain.user.User;

public record UserInfo(
        String id,
        String email,
        String nickname,
        String avatarUrl,
        String identityType,
        boolean isEmailVerified,
        boolean aiTrainConsent,
        String createdAt,
        String updatedAt
) {

    public static UserInfo from(User user) {
        return new UserInfo(
                user.id().toString(),
                user.email().value(),
                user.nickname(),
                user.avatarUrl(),
                user.identityType().value(),
                user.isEmailVerified(),
                user.isAiTrainConsent(),
                user.createdAt().toString(),
                user.updatedAt().toString()
        );
    }
}