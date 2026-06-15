package com.czx.wenshu.application.auth;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        long refreshTokenExpiresInSeconds
) {
}
