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
        UPDATE files SET status = 1, created_at = NOW()
        WHERE target_id IS NULL
          AND status = 0
          AND created_at < NOW() - INTERVAL '24 hours'
        """)
    int markOrphanFilesForCleanup();

    @Select("""
        SELECT * FROM files
        WHERE status = 1
          AND created_at < NOW() - INTERVAL '7 days'
        """)
    List<FileRecord> selectFilesToDelete();

    @Update("UPDATE files SET audit_status = #{auditStatus} WHERE id = #{fileId}")
    int updateAuditStatus(@Param("fileId") Long fileId, @Param("auditStatus") Integer auditStatus);
}
