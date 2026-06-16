package com.czx.wenshu.infrastructure.persistence.imports;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ImportParseSessionMapper {

    @Select("""
            SELECT id, project_id, user_id, parsed_chapters, expires_at, created_at
            FROM import_parse_sessions WHERE id = CAST(#{id} AS UUID)
            """)
    ImportParseSessionRecord findById(@Param("id") String id);

    @Insert("""
            INSERT INTO import_parse_sessions (id, project_id, user_id, parsed_chapters, expires_at, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), CAST(#{userId} AS UUID),
                    #{parsedChapters}, #{expiresAt}, #{createdAt})
            """)
    void insert(ImportParseSessionRecord record);

    @Update("""
            UPDATE import_parse_sessions SET parsed_chapters = #{parsedChapters}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(ImportParseSessionRecord record);

    @Delete("DELETE FROM import_parse_sessions WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}
