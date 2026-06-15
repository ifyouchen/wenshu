package com.czx.wenshu.application.user;

import com.czx.wenshu.domain.user.IdentityType;
import java.util.UUID;

public record UpdateProfileCommand(UUID id, String nickname, String avatarUrl, IdentityType identityType) {
}