package com.campus.wall.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.wall.entity.system.SysNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {

    @Select("""
        SELECT n.*, u.nickname as created_by_name
        FROM sys_notice n
        LEFT JOIN users u ON n.created_by = u.id
        WHERE n.status = 1
          AND (n.scope_type IS NULL OR n.scope_type = '' OR n.scope_type = 'ALL')
          AND (n.start_at IS NULL OR n.start_at <= #{now})
          AND (n.end_at IS NULL OR n.end_at >= #{now})
        ORDER BY n.is_pinned DESC, n.published_at DESC NULLS LAST
        LIMIT #{limit}
        """)
    List<SysNotice> selectPublicNotices(@Param("limit") int limit, @Param("now") java.time.LocalDateTime now);

    @Select("""
        SELECT n.*, u.nickname as created_by_name
        FROM sys_notice n
        LEFT JOIN users u ON n.created_by = u.id
        WHERE n.status = 1
          AND (n.start_at IS NULL OR n.start_at <= #{now})
          AND (n.end_at IS NULL OR n.end_at >= #{now})
          AND (
            n.scope_type IS NULL OR n.scope_type = '' OR n.scope_type = 'ALL'
            OR (n.scope_type = 'DEPT' AND #{deptId} IS NOT NULL AND n.scope_ids @> to_jsonb(ARRAY[#{deptId}]::bigint[]))
            OR (n.scope_type = 'USERS' AND #{userId} IS NOT NULL AND n.scope_ids @> to_jsonb(ARRAY[#{userId}]::bigint[]))
          )
        ORDER BY n.is_pinned DESC, n.published_at DESC NULLS LAST
        LIMIT #{size} OFFSET #{offset}
        """)
    List<SysNotice> selectVisibleNotices(@Param("userId") Long userId, @Param("deptId") Long deptId, @Param("now") java.time.LocalDateTime now, @Param("size") int size, @Param("offset") int offset);

    @Select("""
        SELECT COUNT(*)
        FROM sys_notice n
        WHERE n.status = 1
          AND (n.start_at IS NULL OR n.start_at <= #{now})
          AND (n.end_at IS NULL OR n.end_at >= #{now})
          AND (
            n.scope_type IS NULL OR n.scope_type = '' OR n.scope_type = 'ALL'
            OR (n.scope_type = 'DEPT' AND #{deptId} IS NOT NULL AND n.scope_ids @> to_jsonb(ARRAY[#{deptId}]::bigint[]))
            OR (n.scope_type = 'USERS' AND #{userId} IS NOT NULL AND n.scope_ids @> to_jsonb(ARRAY[#{userId}]::bigint[]))
          )
        """)
    long countVisibleNotices(@Param("userId") Long userId, @Param("deptId") Long deptId, @Param("now") java.time.LocalDateTime now);
}
