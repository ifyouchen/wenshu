package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PasswordResetMapper {

    @Insert("""
            INSERT INTO password_resets (id, user_id, token_hash, expires_at, used_at, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{tokenHash}, #{expiresAt}, #{usedAt}, #{createdAt})
            """)
    void insert(PasswordResetRecord record);

    @Select("""
            SELECT id, user_id, token_hash, expires_at, used_at, created_at
            FROM password_resets
            WHERE token_hash = #{tokenHash}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    PasswordResetRecord findByTokenHash(@Param("tokenHash") String tokenHash);

    @Update("""
            UPDATE password_resets
            SET used_at = #{usedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void markUsed(@Param("id") String id, @Param("usedAt") Instant usedAt);
}
