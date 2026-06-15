package com.czx.wenshu.application.auth;

import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRegistrationPolicy;
import com.czx.wenshu.domain.user.UserRepository;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final Clock clock;

    public AuthApplicationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.clock = clock;
    }

    @Transactional
    public RegisterResult register(RegisterCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        new UserRegistrationPolicy(userRepository).ensureEmailAvailable(email);

        User user = User.register(email.value(), passwordEncoder.encode(command.password()), command.nickname(), clock);
        userRepository.save(user);
        TokenPair tokenPair = authTokenService.issueFor(user);

        return new RegisterResult(tokenPair, user);
    }
}
