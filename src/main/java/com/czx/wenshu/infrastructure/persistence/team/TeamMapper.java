package com.czx.wenshu.infrastructure.persistence.team;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 teams 表（P9-07）。 */
@Mapper
public interface TeamMapper {

    @Select("SELECT id, owner_id, name, plan_key, monthly_char_limit, monthly_adaptation_limit, created_at, updated_at FROM teams WHERE id = CAST(#{id} AS UUID)")
    TeamRecord findById(@Param("id") String id);

    @Select("""
            SELECT t.id, t.owner_id, t.name, t.plan_key, t.monthly_char_limit, t.monthly_adaptation_limit, t.created_at, t.updated_at
            FROM teams t
            JOIN team_members tm ON t.id = tm.team_id
            WHERE tm.user_id = CAST(#{userId} AS UUID) AND tm.status = 'active'
            """)
    List<TeamRecord> findByMemberUserId(@Param("userId") String userId);

    @Insert("""
            INSERT INTO teams (id, owner_id, name, plan_key, monthly_char_limit, monthly_adaptation_limit, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{ownerId} AS UUID), #{name}, #{planKey},
                    #{monthlyCharLimit}, #{monthlyAdaptationLimit}, #{createdAt}, #{updatedAt})
            """)
    void insert(TeamRecord record);

    @Update("UPDATE teams SET name = #{name}, updated_at = #{updatedAt} WHERE id = CAST(#{id} AS UUID)")
    void update(TeamRecord record);
}
