package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.RegisterResult;
import com.czx.wenshu.application.auth.TokenPair;
import com.czx.wenshu.domain.user.User;

public record RegisterResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds,
        UserView user
) {

    public static RegisterResponse from(RegisterResult result) {
        TokenPair tokenPair = result.tokenPair();
        return new RegisterResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.accessTokenExpiresInSeconds(),
                tokenPair.refreshTokenExpiresInSeconds(),
                UserView.from(result.user())
        );
    }

    public record UserView(
            String id,
            String email,
            String nickname,
            String identityType,
            boolean isEmailVerified,
            boolean aiTrainConsent
    ) {

        public static UserView from(User user) {
            return new UserView(
                    user.id().toString(),
                    user.email().value(),
                    user.nickname(),
                    user.identityType().value(),
                    user.isEmailVerified(),
                    user.isAiTrainConsent()
            );
        }
    }
}
