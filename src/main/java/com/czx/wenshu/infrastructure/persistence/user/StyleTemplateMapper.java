package com.czx.wenshu.infrastructure.persistence.user;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StyleTemplateMapper {

    @Select("""
            SELECT id, user_id, name, template_type, genres, prompt, is_active AS active, created_at, updated_at
            FROM style_templates WHERE id = CAST(#{id} AS UUID)
            """)
    StyleTemplateRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, user_id, name, template_type, genres, prompt, is_active AS active, created_at, updated_at
            FROM style_templates WHERE user_id = CAST(#{userId} AS UUID)
            ORDER BY template_type, is_active DESC, updated_at DESC
            """)
    List<StyleTemplateRecord> findByUserId(@Param("userId") String userId);

    @Select("""
            SELECT id, user_id, name, template_type, genres, prompt, is_active AS active, created_at, updated_at
            FROM style_templates
            WHERE user_id = CAST(#{userId} AS UUID) AND template_type = #{templateType}
            ORDER BY is_active DESC, updated_at DESC
            """)
    List<StyleTemplateRecord> findByUserIdAndType(@Param("userId") String userId,
                                                  @Param("templateType") String templateType);

    @Insert("""
            INSERT INTO style_templates (id, user_id, name, template_type, genres, prompt, is_active, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{name}, #{templateType},
                    #{genres}, #{prompt}, #{active}, #{createdAt}, #{updatedAt})
            """)
    void insert(StyleTemplateRecord record);

    @Update("""
            UPDATE style_templates
            SET name = #{name}, template_type = #{templateType}, genres = #{genres}, prompt = #{prompt},
                is_active = #{active}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(StyleTemplateRecord record);

    @Update("""
            UPDATE style_templates SET is_active = FALSE
            WHERE user_id = CAST(#{userId} AS UUID) AND template_type = #{templateType}
            """)
    void deactivateByUserIdAndType(@Param("userId") String userId, @Param("templateType") String templateType);

    @Delete("DELETE FROM style_templates WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}
