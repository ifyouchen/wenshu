package com.czx.wenshu.application.auth;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.EmailVerification;
import com.czx.wenshu.domain.user.EmailVerificationRepository;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRegistrationPolicy;
import com.czx.wenshu.domain.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private static final Duration VERIFY_EMAIL_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESEND_VERIFY_EMAIL_COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final VerificationEmailSender verificationEmailSender;
    private final Clock clock;

    public AuthApplicationService(
            UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            EmailVerificationTokenService emailVerificationTokenService,
            VerificationEmailSender verificationEmailSender,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.verificationEmailSender = verificationEmailSender;
        this.clock = clock;
    }

    @Transactional
    public RegisterResult register(RegisterCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        new UserRegistrationPolicy(userRepository).ensureEmailAvailable(email);

        User user = User.register(email.value(), passwordEncoder.encode(command.password()), command.nickname(), clock);
        userRepository.save(user);
        issueVerificationEmail(user);
        TokenPair tokenPair = authTokenService.issueFor(user);

        return new RegisterResult(tokenPair, user);
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
}
