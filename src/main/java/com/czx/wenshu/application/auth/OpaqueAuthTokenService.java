package com.czx.wenshu.application.auth;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.RefreshToken;
import com.czx.wenshu.domain.user.RefreshTokenRepository;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpaqueAuthTokenService implements AuthTokenService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(2);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public OpaqueAuthTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TokenPair issueFor(User user) {
        return issueInternal(user).tokenPair();
    }

    @Override
    @Transactional
    public RefreshTokenResult rotateRefreshToken(String rawRefreshToken) {
        Instant now = Instant.now(clock);
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Refresh Token 无效或已失效"));
        if (!oldToken.isUsableAt(now)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh Token 无效或已失效");
        }
        User user = userRepository.findById(oldToken.userId())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Refresh Token 无效或已失效"));

        IssuedRefreshToken issued = issueInternal(user);
        oldToken.revoke(now, issued.refreshToken().id());
        refreshTokenRepository.revoke(oldToken.id(), now, issued.refreshToken().id());
        return new RefreshTokenResult(issued.tokenPair(), user);
    }

    private IssuedRefreshToken issueInternal(User user) {
        Instant now = Instant.now(clock);
        String rawRefreshToken = "wrt_" + randomToken();
        RefreshToken refreshToken = RefreshToken.issue(
                user.id(),
                hash(rawRefreshToken),
                now.plus(REFRESH_TOKEN_TTL),
                now
        );
        refreshTokenRepository.save(refreshToken);
        TokenPair tokenPair = new TokenPair(
                "wat_" + randomToken(),
                rawRefreshToken,
                "Bearer",
                ACCESS_TOKEN_TTL.toSeconds(),
                REFRESH_TOKEN_TTL.toSeconds()
        );
        return new IssuedRefreshToken(tokenPair, refreshToken);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record IssuedRefreshToken(TokenPair tokenPair, RefreshToken refreshToken) {
    }
}
