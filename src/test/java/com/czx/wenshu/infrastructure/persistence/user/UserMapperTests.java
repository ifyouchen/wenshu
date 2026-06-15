package com.czx.wenshu.infrastructure.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@SpringBootTest
@Sql(scripts = "/sql/users-test-schema.sql")
class UserMapperTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MyBatisUserRepository userRepository;

    @Test
    void insertsAndFindsUserByIdAndEmail() {
        User user = User.register("mapper@example.com", "hash", "映射测试", FIXED_CLOCK);

        userRepository.save(user);

        UserRecord byId = userMapper.findById(user.id().toString());
        UserRecord byEmail = userMapper.findByEmail("mapper@example.com");

        assertThat(byId).isNotNull();
        assertThat(byId.getEmail()).isEqualTo("mapper@example.com");
        assertThat(byEmail).isNotNull();
        assertThat(byEmail.getId()).isEqualTo(user.id().toString());
        assertThat(userMapper.existsByEmail("mapper@example.com")).isTrue();
    }

    @Test
    void repositoryFindsDomainUserByEmail() {
        User user = User.register("repo@example.com", "hash", "仓储测试", FIXED_CLOCK);
        user.markDeleted(FIXED_CLOCK);

        userRepository.save(user);

        User found = userRepository.findByEmail(new EmailAddress("REPO@example.com")).orElseThrow();

        assertThat(found.id()).isEqualTo(user.id());
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.deletedAt()).isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
    }
}
