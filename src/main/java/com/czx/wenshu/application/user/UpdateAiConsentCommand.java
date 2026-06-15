package com.czx.wenshu.application.user;

import java.util.UUID;

public record UpdateAiConsentCommand(UUID id, boolean aiTrainConsent) {
}