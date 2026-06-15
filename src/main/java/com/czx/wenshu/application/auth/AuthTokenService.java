package com.czx.wenshu.application.auth;

import com.czx.wenshu.domain.user.User;

public interface AuthTokenService {

    TokenPair issueFor(User user);

    RefreshTokenResult rotateRefreshToken(String rawRefreshToken);
}
