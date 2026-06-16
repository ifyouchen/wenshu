package com.czx.wenshu.application.user;

import com.czx.wenshu.application.auth.EmailVerificationTokenService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.AccountRestoreToken;
import com.czx.wenshu.domain.user.AccountRestoreTokenRepository;
import com.czx.wenshu.domain.user.AccessTokenRepository;
import com.czx.wenshu.domain.user.IdentityType;
import com.czx.wenshu.domain.user.RefreshTokenRepository;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserApplicationService {

    private static final Duration RESTORE_TOKEN_TTL = Duration.ofDays(30);
    private static final DateTimeFormatter ALERT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    private final UserRepository userRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRestoreTokenRepository accountRestoreTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenService tokenService;
    private final SecurityAlertEmailSender securityAlertEmailSender;
    private final Clock clock;

    public UserApplicationService(UserRepository userRepository, AccessTokenRepository accessTokenRepository, RefreshTokenRepository refreshTokenRepository, AccountRestoreTokenRepository accountRestoreTokenRepository, PasswordEncoder passwordEncoder, EmailVerificationTokenService tokenService, SecurityAlertEmailSender securityAlertEmailSender, Clock clock) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accountRestoreTokenRepository = accountRestoreTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.securityAlertEmailSender = securityAlertEmailSender;
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
        securityAlertEmailSender.sendSecurityAlertEmail(
                user.email(), "密码修改", "您的账号密码已被修改", ALERT_TIME_FORMATTER.format(now));
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
    public UserInfo updateIdentityType(UUID userId, String identityTypeValue) {
        User user = userRepository.findById(userId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        IdentityType identityType = IdentityType.fromValue(identityTypeValue);
        user.updateProfile(user.nickname(), user.avatarUrl(), identityType, clock);
        userRepository.save(user);
        return UserInfo.from(user);
    }

    @Transactional
    public DeleteAccountResult deleteAccount(UUIDCommand command) {
        User user = userRepository.findById(command.id())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.markDeleted(clock);
        userRepository.save(user);
        Instant now = Instant.now(clock);
        accessTokenRepository.revokeAllForUser(user.id(), now);
        refreshTokenRepository.revokeAllForUser(user.id(), now);

        Instant expiresAt = now.plus(RESTORE_TOKEN_TTL);
        String rawToken = tokenService.generateRawToken();
        AccountRestoreToken restoreToken = AccountRestoreToken.issue(
                user.id(), tokenService.hash(rawToken), expiresAt, now);
        accountRestoreTokenRepository.save(restoreToken);
        return new DeleteAccountResult(rawToken, expiresAt);
    }

    /** P4-09：更新用户全局每日写作目标（字数/天）。 */
    @Transactional
    public UserInfo updateGlobalWritingGoal(UUID userId, int dailyCharGoal) {
        User user = userRepository.findById(userId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.updateDailyCharGoal(dailyCharGoal, clock);
        userRepository.save(user);
        return UserInfo.from(user);
    }

    @Transactional
    public UserInfo restoreAccount(String rawToken) {
        Instant now = Instant.now(clock);
        AccountRestoreToken restoreToken = accountRestoreTokenRepository.findByTokenHash(tokenService.hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "恢复链接无效或已过期"));
        if (!restoreToken.isUsableAt(now)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "恢复链接无效或已过期");
        }
        User user = userRepository.findById(restoreToken.userId())
                .filter(User::isDeleted)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在或未注销"));

        user.restore(clock);
        userRepository.save(user);
        restoreToken.markUsed(now);
        accountRestoreTokenRepository.markUsed(restoreToken.id(), now);
        securityAlertEmailSender.sendSecurityAlertEmail(
                user.email(), "账号恢复", "您的账号已从注销状态恢复", ALERT_TIME_FORMATTER.format(now));
        return UserInfo.from(user);
    }
}