package com.czx.wenshu.application.auth;

import com.czx.wenshu.domain.user.User;

public record LoginResult(TokenPair tokenPair, User user) {
}
