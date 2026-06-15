package com.czx.wenshu.infrastructure.persistence.user;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountRestoreTokenMapper {

    @Select("""
            SELECT id, user_id, token_hash, expires_at, used_at, created_at
            FROM account_restore_tokens
            WHERE token_hash = #{tokenHash}
            """)
    AccountRestoreTokenRecord findByTokenHash(@Param("tokenHash") String tokenHash);

    @Insert("""
            INSERT INTO account_restore_tokens (id, user_id, token_hash, expires_at, used_at, created_at)
            VALUES (
                CAST(#{id} AS UUID),
                CAST(#{userId} AS UUID),
                #{tokenHash},
                #{expiresAt},
                #{usedAt},
                #{createdAt}
            )
            """)
    void insert(AccountRestoreTokenRecord record);

    @Update("""
            UPDATE account_restore_tokens
            SET used_at = #{usedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void markUsed(@Param("id") String id, @Param("usedAt") Instant usedAt);
}