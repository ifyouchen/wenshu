package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailVerificationMapper {

    @Insert("""
            INSERT INTO email_verifications (id, user_id, token_hash, expires_at, used_at, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{tokenHash}, #{expiresAt}, #{usedAt}, #{createdAt})
            """)
    void insert(EmailVerificationRecord record);

    @Select("""
            SELECT id, user_id, token_hash, expires_at, used_at, created_at
            FROM email_verifications
            WHERE token_hash = #{tokenHash}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    EmailVerificationRecord findByTokenHash(@Param("tokenHash") String tokenHash);

    @Select("""
            SELECT COUNT(1)
            FROM email_verifications
            WHERE user_id = CAST(#{userId} AS UUID)
              AND used_at IS NULL
              AND created_at >= #{createdAfter}
            """)
    boolean existsUnusedCreatedAfter(@Param("userId") String userId, @Param("createdAfter") Instant createdAfter);

    @Update("""
            UPDATE email_verifications
            SET used_at = #{usedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void markUsed(@Param("id") String id, @Param("usedAt") Instant usedAt);
}
