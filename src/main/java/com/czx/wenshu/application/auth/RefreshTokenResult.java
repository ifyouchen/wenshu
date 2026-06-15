package com.czx.wenshu.application.auth;

import com.czx.wenshu.domain.user.User;

public record RefreshTokenResult(TokenPair tokenPair, User user) {
}
