package com.czx.wenshu.domain.user;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;

public class UserRegistrationPolicy {

    private final UserRepository userRepository;

    public UserRegistrationPolicy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void ensureEmailAvailable(EmailAddress email) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "该邮箱已注册");
        }
    }
}
