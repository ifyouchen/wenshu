package com.czx.wenshu.infrastructure.persistence.safety;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** MyBatis Mapper，对应 content_appeals 表（P9-05）。 */
@Mapper
public interface ContentAppealMapper {

    /** 插入申诉记录。 */
    @Insert("""
            INSERT INTO content_appeals
              (id, user_id, content, reason, status, reviewer_note, created_at, updated_at)
            VALUES
              (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{content}, #{reason},
               #{status}, #{reviewerNote}, #{createdAt}, #{updatedAt})
            """)
    void insert(ContentAppealRecord record);

    /** 查询用户所有申诉记录，按创建时间倒序。 */
    @Select("""
            SELECT id, user_id, content, reason, status, reviewer_note,
                   created_at, updated_at
            FROM content_appeals
            WHERE user_id = CAST(#{userId} AS UUID)
            ORDER BY created_at DESC
            """)
    List<ContentAppealRecord> findByUserId(@Param("userId") String userId);
}
