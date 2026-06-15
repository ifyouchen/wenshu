package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.VerifyEmailResult;

public record VerifyEmailResponse(
        String userId,
        String email,
        boolean isEmailVerified
) {

    public static VerifyEmailResponse from(VerifyEmailResult result) {
        return new VerifyEmailResponse(
                result.user().id().toString(),
                result.user().email().value(),
                result.user().isEmailVerified()
        );
    }
}
