package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书信息 DTO（跨服务传输）
 */
@Data
public class BookDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDateTime publishDate;
    private String category;
    private String description;
    private String coverUrl;
    private BigDecimal price;
    private Integer status;
    private Integer totalStock;
    private Integer availableStock;
    private String location;
    private String callNumber;
    private Boolean hasEbook;
    private String ebookUrl;
    /** 借阅次数 */
    private Integer borrowCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
