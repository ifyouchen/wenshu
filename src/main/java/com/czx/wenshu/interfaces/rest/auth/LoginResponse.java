package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.LoginResult;
import com.czx.wenshu.application.auth.TokenPair;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds,
        RegisterResponse.UserView user
) {

    public static LoginResponse from(LoginResult result) {
        TokenPair tokenPair = result.tokenPair();
        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.accessTokenExpiresInSeconds(),
                tokenPair.refreshTokenExpiresInSeconds(),
                RegisterResponse.UserView.from(result.user())
        );
    }
}
