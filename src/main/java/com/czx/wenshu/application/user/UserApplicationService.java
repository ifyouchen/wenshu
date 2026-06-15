package com.czx.wenshu.application.user;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.IdentityType;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRepository;
import com.czx.wenshu.domain.user.AccessTokenRepository;
import com.czx.wenshu.domain.user.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserApplicationService(UserRepository userRepository, AccessTokenRepository accessTokenRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserInfo getCurrentUser(UUIDCommand command) {
        User user = userRepository.findById(command.id())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        return UserInfo.from(user);
    }

    @Transactional
    public UserInfo updateProfile(UpdateProfileCommand command) {
        User user = userRepository.findById(command.id())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.updateProfile(command.nickname(), command.avatarUrl(), command.identityType(), clock);
        userRepository.save(user);
        return UserInfo.from(user);
    }

    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        User user = userRepository.findById(command.id())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (!passwordEncoder.matches(command.currentPassword(), user.passwordHash())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "当前密码错误");
        }
        user.changePasswordByUser(user.passwordHash(), passwordEncoder.encode(command.newPassword()), clock);
        userRepository.save(user);
        Instant now = Instant.now(clock);
        accessTokenRepository.revokeAllForUser(user.id(), now);
        refreshTokenRepository.revokeAllForUser(user.id(), now);
    }

    @Transactional
    public UserInfo updateAiConsent(UpdateAiConsentCommand command) {
        User user = userRepository.findById(command.id())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.updateAiConsent(command.aiTrainConsent(), clock);
        userRepository.save(user);
        return UserInfo.from(user);
    }

    @Transactional
    public void deleteAccount(UUIDCommand command) {
        User user = userRepository.findById(command.id())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.markDeleted(clock);
        userRepository.save(user);
        Instant now = Instant.now(clock);
        accessTokenRepository.revokeAllForUser(user.id(), now);
        refreshTokenRepository.revokeAllForUser(user.id(), now);
    }

    @Transactional
    public UserInfo restoreAccount(UUIDCommand command) {
        User user = userRepository.findById(command.id())
                .filter(User::isDeleted)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在或未注销"));
        user.restore(clock);
        userRepository.save(user);
        return UserInfo.from(user);
    }
}