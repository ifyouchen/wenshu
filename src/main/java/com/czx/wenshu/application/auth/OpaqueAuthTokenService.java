package com.czx.wenshu.application.auth;

import com.czx.wenshu.domain.user.User;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class OpaqueAuthTokenService implements AuthTokenService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(2);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public TokenPair issueFor(User user) {
        return new TokenPair(
                "wat_" + randomToken(),
                "wrt_" + randomToken(),
                "Bearer",
                ACCESS_TOKEN_TTL.toSeconds(),
                REFRESH_TOKEN_TTL.toSeconds()
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
