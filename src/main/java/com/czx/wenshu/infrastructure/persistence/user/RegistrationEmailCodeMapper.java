package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RegistrationEmailCodeMapper {

    @Insert("""
            INSERT INTO registration_email_codes (id, email, code_hash, expires_at, used_at, created_at)
            VALUES (CAST(#{id} AS UUID), #{email}, #{codeHash}, #{expiresAt}, #{usedAt}, #{createdAt})
            """)
    void insert(RegistrationEmailCodeRecord record);

    @Select("""
            SELECT id, email, code_hash, expires_at, used_at, created_at
            FROM registration_email_codes
            WHERE email = #{email}
              AND code_hash = #{codeHash}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    RegistrationEmailCodeRecord findLatestByEmailAndCodeHash(@Param("email") String email,
                                                              @Param("codeHash") String codeHash);

    @Select("""
            SELECT COUNT(1)
            FROM registration_email_codes
            WHERE email = #{email}
              AND used_at IS NULL
              AND created_at >= #{createdAfter}
            """)
    boolean existsUnusedCreatedAfter(@Param("email") String email, @Param("createdAfter") Instant createdAfter);

    @Update("""
            UPDATE registration_email_codes
            SET used_at = #{usedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void markUsed(@Param("id") String id, @Param("usedAt") Instant usedAt);
}
