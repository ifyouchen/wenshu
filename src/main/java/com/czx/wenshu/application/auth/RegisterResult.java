package com.czx.wenshu.application.auth;

import com.czx.wenshu.domain.user.User;

public record RegisterResult(TokenPair tokenPair, User user) {
}
