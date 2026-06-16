package com.czx.wenshu.infrastructure.persistence.consistency;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** MyBatis Mapper，对应 ai_operation_logs 表（P6-06）。 */
@Mapper
public interface AiOperationLogMapper {

    @Select("""
            SELECT id, user_id, project_id, operation, model, created_at
            FROM ai_operation_logs WHERE id = CAST(#{id} AS UUID)
            """)
    AiOperationLogRecord findById(@Param("id") String id);

    @Insert("""
            INSERT INTO ai_operation_logs (id, user_id, project_id, operation, model, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), CAST(#{projectId} AS UUID),
                    #{operation}, #{model}, #{createdAt})
            """)
    void insert(AiOperationLogRecord record);
}
