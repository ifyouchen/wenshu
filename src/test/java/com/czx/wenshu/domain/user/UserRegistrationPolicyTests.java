package com.czx.wenshu.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.czx.wenshu.common.exception.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRegistrationPolicyTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsDuplicatedEmail() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.save(User.register("author@example.com", "hash", "作者", FIXED_CLOCK));
        UserRegistrationPolicy policy = new UserRegistrationPolicy(repository);

        assertThatThrownBy(() -> policy.ensureEmailAvailable(new EmailAddress("AUTHOR@example.com")))
                .isInstanceOf(ApiException.class)
                .hasMessage("该邮箱已注册");
    }

    @Test
    void softDeletedUserStillOccupiesEmailAndCanBeRestored() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = User.register("deleted@example.com", "hash", "作者", FIXED_CLOCK);
        user.markDeleted(FIXED_CLOCK);
        repository.save(user);
        UserRegistrationPolicy policy = new UserRegistrationPolicy(repository);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.deletedAt()).isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
        assertThatThrownBy(() -> policy.ensureEmailAvailable(new EmailAddress("deleted@example.com")))
                .isInstanceOf(ApiException.class)
                .hasMessage("该邮箱已注册");

        user.restore(FIXED_CLOCK);

        assertThat(user.isDeleted()).isFalse();
        assertThat(user.deletedAt()).isNull();
    }

    private static class InMemoryUserRepository implements UserRepository {

        private final Map<UUID, User> users = new HashMap<>();

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmail(EmailAddress email) {
            return users.values().stream()
                    .filter(user -> user.email().equals(email))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(EmailAddress email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public void save(User user) {
            users.put(user.id(), user);
        }
    }
}
