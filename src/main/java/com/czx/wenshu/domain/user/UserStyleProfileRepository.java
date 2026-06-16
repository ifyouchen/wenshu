package com.czx.wenshu.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserStyleProfileRepository {

    void save(UserStyleProfile profile);

    Optional<UserStyleProfile> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
