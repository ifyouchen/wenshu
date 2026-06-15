package com.czx.wenshu.interfaces.rest.user;

public record UpdateAiConsentRequest(
        boolean aiTrainConsent
) {
}