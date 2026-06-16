package com.czx.wenshu.infrastructure.persistence.user;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 word_packs 表（P9-09）。 */
@Mapper
public interface WordPackMapper {

    /** 查询用户有效字数包（未耗尽，按创建时间升序，优先消耗最早购买的）。 */
    @Select("""
            SELECT id, user_id, pack_key, pack_type, chars_total, chars_used,
                   expires_at, created_at, updated_at
            FROM word_packs
            WHERE user_id = CAST(#{userId} AS UUID)
              AND chars_used < chars_total
              AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
            ORDER BY created_at ASC
            """)
    List<WordPackRecord> findActiveByUserId(@Param("userId") String userId);

    /** 检查用户是否已有体验额度。 */
    @Select("SELECT COUNT(*) FROM word_packs WHERE user_id = CAST(#{userId} AS UUID) AND pack_type = 'trial'")
    int countTrialByUserId(@Param("userId") String userId);

    /** 插入字数包。 */
    @Insert("""
            INSERT INTO word_packs (id, user_id, pack_key, pack_type, chars_total, chars_used,
                                    expires_at, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{packKey}, #{packType},
                    #{charsTotal}, #{charsUsed}, #{expiresAt}, #{createdAt}, #{updatedAt})
            """)
    void insert(WordPackRecord record);

    /** 更新已消耗字符数。 */
    @Update("""
            UPDATE word_packs
            SET chars_used = #{charsUsed}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(WordPackRecord record);
}
