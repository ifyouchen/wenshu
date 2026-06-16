package com.czx.wenshu.application.auth;

import com.czx.wenshu.application.user.WordPackService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.AccessTokenRepository;
import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.EmailVerification;
import com.czx.wenshu.domain.user.EmailVerificationRepository;
import com.czx.wenshu.domain.user.PasswordReset;
import com.czx.wenshu.domain.user.PasswordResetRepository;
import com.czx.wenshu.domain.user.RegistrationEmailCode;
import com.czx.wenshu.domain.user.RegistrationEmailCodeRepository;
import com.czx.wenshu.domain.user.RefreshTokenRepository;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRegistrationPolicy;
import com.czx.wenshu.domain.user.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration REGISTER_CODE_TTL = Duration.ofMinutes(10);
    private static final Duration REGISTER_CODE_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration VERIFY_EMAIL_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESEND_VERIFY_EMAIL_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final RegistrationEmailCodeRepository registrationEmailCodeRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final VerificationEmailSender verificationEmailSender;
    private final PasswordResetEmailSender passwordResetEmailSender;
    /** P9-09：注册时自动发放体验额度。 */
    private final WordPackService wordPackService;
    private final Clock clock;

    public AuthApplicationService(
            UserRepository userRepository,
            RegistrationEmailCodeRepository registrationEmailCodeRepository,
            EmailVerificationRepository emailVerificationRepository,
            PasswordResetRepository passwordResetRepository,
            RefreshTokenRepository refreshTokenRepository,
            AccessTokenRepository accessTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            EmailVerificationTokenService emailVerificationTokenService,
            VerificationEmailSender verificationEmailSender,
            PasswordResetEmailSender passwordResetEmailSender,
            WordPackService wordPackService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.registrationEmailCodeRepository = registrationEmailCodeRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.verificationEmailSender = verificationEmailSender;
        this.passwordResetEmailSender = passwordResetEmailSender;
        this.wordPackService = wordPackService;
        this.clock = clock;
    }

    /**
     * 用户注册（P1-03 / P9-09）。
     *
     * <p>注册成功后自动发放 5 万字体验额度。</p>
     */
    @Transactional
    public RegisterResult register(RegisterCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        new UserRegistrationPolicy(userRepository).ensureEmailAvailable(email);
        RegistrationEmailCode code = registrationEmailCodeRepository
                .findLatestByEmailAndCodeHash(email, hashRegistrationCode(email, command.verificationCode()))
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "验证码错误或已失效"));
        Instant now = Instant.now(clock);
        if (!code.isUsableAt(now)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "验证码错误或已失效");
        }

        User user = User.register(email.value(), passwordEncoder.encode(command.password()), command.nickname(), clock);
        user.verifyEmail(clock);
        userRepository.save(user);
        code.markUsed(now);
        registrationEmailCodeRepository.markUsed(code.id(), now);
        TokenPair tokenPair = authTokenService.issueFor(user);

        // P9-09：新用户自动发放体验额度（5 万字）
        wordPackService.issueTrial(user.id());
        log.info("[AuthApplicationService] 新用户注册成功，体验额度已发放 userId={}", user.id());

        return new RegisterResult(tokenPair, user);
    }

    @Transactional
    public SendRegisterCodeResult sendRegisterCode(SendRegisterCodeCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        new UserRegistrationPolicy(userRepository).ensureEmailAvailable(email);
        Instant now = Instant.now(clock);
        if (registrationEmailCodeRepository.existsUnusedCreatedAfter(email, now.minus(REGISTER_CODE_COOLDOWN))) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "请 60 秒后再试");
        }

        String rawCode = generateRegisterCode();
        Instant expiresAt = now.plus(REGISTER_CODE_TTL);
        RegistrationEmailCode code = RegistrationEmailCode.issue(
                email.value(),
                hashRegistrationCode(email, rawCode),
                expiresAt,
                now
        );
        registrationEmailCodeRepository.save(code);
        verificationEmailSender.sendVerificationEmail(email, rawCode, expiresAt);
        return new SendRegisterCodeResult(true, expiresAt);
    }

    @Transactional
    public VerifyEmailResult verifyEmail(String rawToken) {
        Instant now = Instant.now(clock);
        EmailVerification verification = emailVerificationRepository.findByTokenHash(emailVerificationTokenService.hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "邮箱验证链接无效"));
        if (!verification.isUsableAt(now)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "邮箱验证链接已失效");
        }

        User user = userRepository.findById(verification.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.verifyEmail(clock);
        userRepository.save(user);
        verification.markUsed(now);
        emailVerificationRepository.markUsed(verification.id(), now);

        return new VerifyEmailResult(user);
    }

    @Transactional
    public ResendVerifyEmailResult resendVerifyEmail(ResendVerifyEmailCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .filter(user -> !user.isEmailVerified())
                .map(user -> {
                    Instant now = Instant.now(clock);
                    if (emailVerificationRepository.existsUnusedCreatedAfter(user.id(), now.minus(RESEND_VERIFY_EMAIL_COOLDOWN))) {
                        throw new ApiException(ErrorCode.RATE_LIMITED, "请 60 秒后再试");
                    }
                    return issueVerificationEmail(user);
                })
                .orElseGet(() -> new ResendVerifyEmailResult(false, null));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResult login(LoginCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        User user = userRepository.findByEmail(email)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "邮箱或密码错误"));

        Instant now = Instant.now(clock);
        if (user.isLockedAt(now)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "账号已锁定，请 15 分钟后再试");
        }
        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            user.recordLoginFailure(clock);
            userRepository.save(user);
            throw new ApiException(ErrorCode.BAD_REQUEST, "邮箱或密码错误");
        }

        user.recordLoginSuccess(clock);
        userRepository.save(user);
        return new LoginResult(authTokenService.issueFor(user), user);
    }

    public void logout(String authorizationHeader) {
        // Token persistence and revocation are completed in P1-06.
    }

    /**
     * 登出所有设备（P0-4）。
     * 吊销该用户的所有 Access Token 和 Refresh Token。
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void logoutAll(UUID userId) {
        Instant now = Instant.now(clock);
        accessTokenRepository.revokeAllForUser(userId, now);
        refreshTokenRepository.revokeAllForUser(userId, now);
        log.info("[AuthApplicationService] 已登出所有设备 userId={}", userId);
    }

    @Transactional
    public RefreshTokenResult refreshToken(RefreshTokenCommand command) {
        return authTokenService.rotateRefreshToken(command.refreshToken());
    }

    @Transactional
    public ForgotPasswordResult forgotPassword(ForgotPasswordCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted())
                .map(this::issuePasswordResetEmail)
                .orElseGet(() -> new ForgotPasswordResult(false, null));
    }

    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        Instant now = Instant.now(clock);
        PasswordReset passwordReset = passwordResetRepository.findByTokenHash(emailVerificationTokenService.hash(command.token()))
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "密码重置链接无效"));
        if (!passwordReset.isUsableAt(now)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "密码重置链接已失效");
        }

        User user = userRepository.findById(passwordReset.userId())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.changePassword(passwordEncoder.encode(command.newPassword()), clock);
        userRepository.save(user);
        passwordReset.markUsed(now);
        passwordResetRepository.markUsed(passwordReset.id(), now);
        refreshTokenRepository.revokeAllForUser(user.id(), now);
    }

    private ResendVerifyEmailResult issueVerificationEmail(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(VERIFY_EMAIL_TOKEN_TTL);
        String rawToken = emailVerificationTokenService.generateRawToken();
        EmailVerification verification = EmailVerification.issue(
                user.id(),
                emailVerificationTokenService.hash(rawToken),
                expiresAt,
                now
        );
        emailVerificationRepository.save(verification);
        verificationEmailSender.sendVerificationEmail(user.email(), rawToken, expiresAt);
        return new ResendVerifyEmailResult(true, expiresAt);
    }

    private ForgotPasswordResult issuePasswordResetEmail(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(PASSWORD_RESET_TOKEN_TTL);
        String rawToken = emailVerificationTokenService.generateRawToken();
        PasswordReset passwordReset = PasswordReset.issue(
                user.id(),
                emailVerificationTokenService.hash(rawToken),
                expiresAt,
                now
        );
        passwordResetRepository.save(passwordReset);
        passwordResetEmailSender.sendPasswordResetEmail(user.email(), rawToken, expiresAt);
        return new ForgotPasswordResult(true, expiresAt);
    }

    private String generateRegisterCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String hashRegistrationCode(EmailAddress email, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "请输入验证码");
        }
        return emailVerificationTokenService.hash(email.value() + ":" + rawCode.trim());
    }
}
