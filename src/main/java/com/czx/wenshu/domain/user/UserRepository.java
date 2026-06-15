package com.czx.wenshu.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);

    void save(User user);
}
