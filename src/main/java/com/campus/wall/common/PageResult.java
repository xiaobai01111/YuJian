package com.campus.wall.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装
 */
@Data
@SuppressWarnings("serial")
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private long size;
    private long current;
    private long pages;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, long size, long current) {
        this.records = records;
        this.total = total;
        long safeSize = size > 0 ? size : 1;
        this.size = safeSize;
        this.current = current;
        this.pages = (total + safeSize - 1) / safeSize;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long size, long current) {
        return new PageResult<>(records, total, size, current);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(java.util.Collections.emptyList(), 0, 10, 1);
    }
}
