package com.czx.wenshu.infrastructure.persistence.task;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AsyncTaskMapper {

    @Select("""
            SELECT id, user_id, project_id, task_type, status,
                   current_step, total_steps, step_label, progress_pct,
                   result_id, result_json, error_message, created_at, updated_at
            FROM ai_task_progress
            WHERE id = CAST(#{id} AS UUID)
            """)
    AsyncTaskRecord findById(@Param("id") String id);

    @Insert("""
            INSERT INTO ai_task_progress
                (id, user_id, project_id, task_type, status,
                 current_step, total_steps, step_label, progress_pct,
                 result_id, result_json, error_message, created_at, updated_at)
            VALUES
                (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), CAST(#{projectId} AS UUID),
                 #{taskType}, #{status}, #{currentStep}, #{totalSteps}, #{stepLabel}, #{progressPct},
                 CAST(#{resultId} AS UUID), #{resultJson}, #{errorMessage}, #{createdAt}, #{updatedAt})
            """)
    void insert(AsyncTaskRecord record);

    @Update("""
            UPDATE ai_task_progress
            SET status = #{status}, current_step = #{currentStep}, total_steps = #{totalSteps},
                step_label = #{stepLabel}, progress_pct = #{progressPct},
                result_id = CAST(#{resultId} AS UUID), result_json = #{resultJson},
                error_message = #{errorMessage}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(AsyncTaskRecord record);
}
