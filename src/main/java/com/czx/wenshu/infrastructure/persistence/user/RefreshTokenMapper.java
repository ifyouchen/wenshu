package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefreshTokenMapper {

    @Insert("""
            INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, revoked_at, replaced_by_id, created_at)
            VALUES (
                CAST(#{id} AS UUID),
                CAST(#{userId} AS UUID),
                #{tokenHash},
                #{expiresAt},
                #{revokedAt},
                CAST(#{replacedById} AS UUID),
                #{createdAt}
            )
            """)
    void insert(RefreshTokenRecord record);

    @Select("""
            SELECT id, user_id, token_hash, expires_at, revoked_at, replaced_by_id, created_at
            FROM refresh_tokens
            WHERE token_hash = #{tokenHash}
            """)
    RefreshTokenRecord findByTokenHash(@Param("tokenHash") String tokenHash);

    @Update("""
            UPDATE refresh_tokens
            SET revoked_at = #{revokedAt},
                replaced_by_id = CAST(#{replacedById} AS UUID)
            WHERE id = CAST(#{id} AS UUID)
            """)
    void revoke(@Param("id") String id, @Param("revokedAt") Instant revokedAt, @Param("replacedById") String replacedById);
}
