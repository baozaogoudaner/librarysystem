package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;

/**
 * 统计报表 DTO（跨服务传输）
 */
@Data
public class StatisticsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 概览数据
    private Long totalBooks;
    private Long totalReaders;
    private Long activeBorrows;
    private Long overdueBooks;
    private Long todayBorrows;
    private Long todayReturns;

    // 借阅排行
    private Long bookId;
    private String isbn;
    private String bookTitle;
    private String author;
    private Long borrowCount;
    private Integer rank;

    // 分类统计
    private String category;
    private Long categoryCount;
}
