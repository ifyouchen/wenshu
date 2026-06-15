package com.czx.wenshu.infrastructure.persistence.project;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChapterSnapshotMapper {

    @Select("""
            SELECT id, chapter_id, content, word_count, snapshot_type, label, created_at
            FROM chapter_snapshots WHERE id = CAST(#{id} AS UUID)
            """)
    ChapterSnapshotRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, chapter_id, content, word_count, snapshot_type, label, created_at
            FROM chapter_snapshots WHERE chapter_id = CAST(#{chapterId} AS UUID)
            ORDER BY created_at DESC
            """)
    List<ChapterSnapshotRecord> findByChapterId(@Param("chapterId") String chapterId);

    @Insert("""
            INSERT INTO chapter_snapshots (id, chapter_id, content, word_count, snapshot_type, label, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{chapterId} AS UUID), #{content}, #{wordCount}, #{snapshotType}, #{label}, #{createdAt})
            """)
    void insert(ChapterSnapshotRecord record);
}