package com.czx.wenshu.infrastructure.persistence.user;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, email, password_hash, nickname, identity_type, is_email_verified,
                   ai_train_consent, is_deleted AS deleted, deleted_at, created_at, updated_at
            FROM users
            WHERE id = #{id}
            """)
    UserRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, email, password_hash, nickname, identity_type, is_email_verified,
                   ai_train_consent, is_deleted AS deleted, deleted_at, created_at, updated_at
            FROM users
            WHERE email = #{email}
            """)
    UserRecord findByEmail(@Param("email") String email);

    @Select("""
            SELECT COUNT(1)
            FROM users
            WHERE email = #{email}
            """)
    boolean existsByEmail(@Param("email") String email);

    @Insert("""
            INSERT INTO users (
                id, email, password_hash, nickname, identity_type, is_email_verified,
                ai_train_consent, is_deleted, deleted_at, created_at, updated_at
            )
            VALUES (
                CAST(#{id} AS UUID), #{email}, #{passwordHash}, #{nickname}, #{identityType}, #{emailVerified},
                #{aiTrainConsent}, #{deleted}, #{deletedAt}, #{createdAt}, #{updatedAt}
            )
            """)
    void insert(UserRecord record);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash},
                nickname = #{nickname},
                identity_type = #{identityType},
                is_email_verified = #{emailVerified},
                ai_train_consent = #{aiTrainConsent},
                is_deleted = #{deleted},
                deleted_at = #{deletedAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    void update(UserRecord record);
}
