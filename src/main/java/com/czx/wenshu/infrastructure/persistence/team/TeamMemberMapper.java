package com.czx.wenshu.infrastructure.persistence.team;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 team_members 表（P9-07）。 */
@Mapper
public interface TeamMemberMapper {

    @Select("SELECT id, team_id, user_id, role, status, invited_by, invite_code, joined_at, created_at FROM team_members WHERE invite_code = #{inviteCode}")
    TeamMemberRecord findByInviteCode(@Param("inviteCode") String inviteCode);

    @Select("SELECT id, team_id, user_id, role, status, invited_by, invite_code, joined_at, created_at FROM team_members WHERE team_id = CAST(#{teamId} AS UUID) AND user_id = CAST(#{userId} AS UUID)")
    TeamMemberRecord findByTeamIdAndUserId(@Param("teamId") String teamId, @Param("userId") String userId);

    @Select("SELECT id, team_id, user_id, role, status, invited_by, invite_code, joined_at, created_at FROM team_members WHERE team_id = CAST(#{teamId} AS UUID) AND status = 'active' ORDER BY created_at")
    List<TeamMemberRecord> findActiveByTeamId(@Param("teamId") String teamId);

    @Select("SELECT id, team_id, user_id, role, status, invited_by, invite_code, joined_at, created_at FROM team_members WHERE team_id = CAST(#{teamId} AS UUID) ORDER BY created_at")
    List<TeamMemberRecord> findAllByTeamId(@Param("teamId") String teamId);

    @Insert("""
            INSERT INTO team_members (id, team_id, user_id, role, status, invited_by, invite_code, joined_at, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{teamId} AS UUID), CAST(#{userId} AS UUID),
                    #{role}, #{status},
                    CASE WHEN #{invitedBy} IS NOT NULL THEN CAST(#{invitedBy} AS UUID) ELSE NULL END,
                    #{inviteCode}, #{joinedAt}, #{createdAt})
            """)
    void insert(TeamMemberRecord record);

    @Update("UPDATE team_members SET role = #{role}, status = #{status}, joined_at = #{joinedAt} WHERE id = CAST(#{id} AS UUID)")
    void update(TeamMemberRecord record);
}
