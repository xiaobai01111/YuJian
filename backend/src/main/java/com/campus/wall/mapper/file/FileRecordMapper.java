package com.campus.wall.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.file.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文件记录 Mapper
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {

    @Update("""
        UPDATE files SET status = 1, orphan_marked_at = COALESCE(orphan_marked_at, NOW())
        WHERE target_id IS NULL
          AND status = 0
          AND created_at < NOW() - INTERVAL '24 hours'
        """)
    int markOrphanFilesForCleanup();

    @Update("""
        UPDATE files SET status = 1, orphan_marked_at = COALESCE(orphan_marked_at, NOW())
        WHERE target_id IS NULL
          AND status = 0
          AND created_at < NOW() - make_interval(hours => #{hours})
        """)
    int markOrphanFilesForCleanupByHours(@Param("hours") int hours);

    @Select("""
        SELECT * FROM files
        WHERE status = 1
          AND COALESCE(orphan_marked_at, created_at) < NOW() - INTERVAL '7 days'
        """)
    List<FileRecord> selectFilesToDelete();

    @Select("""
        SELECT * FROM files
        WHERE status = 1
          AND COALESCE(orphan_marked_at, created_at) < NOW() - make_interval(days => #{retentionDays})
        ORDER BY id
        LIMIT #{limit}
        """)
    List<FileRecord> selectFilesToDeleteByRetention(@Param("retentionDays") int retentionDays,
                                                    @Param("limit") int limit);

    @Update("UPDATE files SET audit_status = #{auditStatus} WHERE id = #{fileId}")
    @SuppressWarnings("UnusedReturnValue")
    int updateAuditStatus(@Param("fileId") Long fileId, @Param("auditStatus") Integer auditStatus);
}
