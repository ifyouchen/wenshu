package com.czx.wenshu.infrastructure.persistence.user;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserStyleProfileMapper {

    @Select("""
            SELECT id, user_id, sample_text, style_tags, analysis_task_id, created_at, updated_at
            FROM user_style_profiles WHERE user_id = CAST(#{userId} AS UUID)
            """)
    UserStyleProfileRecord findByUserId(@Param("userId") String userId);

    @Insert("""
            INSERT INTO user_style_profiles (id, user_id, sample_text, style_tags, analysis_task_id, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{sampleText}, #{styleTags},
                    CAST(#{analysisTaskId} AS UUID), #{createdAt}, #{updatedAt})
            """)
    void insert(UserStyleProfileRecord record);

    @Update("""
            UPDATE user_style_profiles
            SET sample_text = #{sampleText}, style_tags = #{styleTags},
                analysis_task_id = CAST(#{analysisTaskId} AS UUID), updated_at = #{updatedAt}
            WHERE user_id = CAST(#{userId} AS UUID)
            """)
    void update(UserStyleProfileRecord record);

    @Delete("DELETE FROM user_style_profiles WHERE user_id = CAST(#{userId} AS UUID)")
    void deleteByUserId(@Param("userId") String userId);
}
