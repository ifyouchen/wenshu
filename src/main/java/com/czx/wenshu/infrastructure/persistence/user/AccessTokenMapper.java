package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccessTokenMapper {

    @Select("""
            SELECT id, user_id, token_hash, expires_at, revoked_at, created_at
            FROM access_tokens
            WHERE token_hash = #{tokenHash}
            """)
    AccessTokenRecord findByTokenHash(@Param("tokenHash") String tokenHash);

    @Insert("""
            INSERT INTO access_tokens (id, user_id, token_hash, expires_at, revoked_at, created_at)
            VALUES (
                CAST(#{id} AS UUID),
                CAST(#{userId} AS UUID),
                #{tokenHash},
                #{expiresAt},
                #{revokedAt},
                #{createdAt}
            )
            """)
    void insert(AccessTokenRecord record);

    @Update("""
            UPDATE access_tokens
            SET revoked_at = #{revokedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void revoke(@Param("id") String id, @Param("revokedAt") Instant revokedAt);

    @Update("""
            UPDATE access_tokens
            SET revoked_at = #{revokedAt}
            WHERE user_id = CAST(#{userId} AS UUID)
              AND revoked_at IS NULL
            """)
    void revokeAllForUser(@Param("userId") String userId, @Param("revokedAt") Instant revokedAt);
}