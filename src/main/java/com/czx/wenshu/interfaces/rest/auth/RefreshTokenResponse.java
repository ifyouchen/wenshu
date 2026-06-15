package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.RefreshTokenResult;
import com.czx.wenshu.application.auth.TokenPair;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds,
        RegisterResponse.UserView user
) {

    public static RefreshTokenResponse from(RefreshTokenResult result) {
        TokenPair tokenPair = result.tokenPair();
        return new RefreshTokenResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.accessTokenExpiresInSeconds(),
                tokenPair.refreshTokenExpiresInSeconds(),
                RegisterResponse.UserView.from(result.user())
        );
    }
}
