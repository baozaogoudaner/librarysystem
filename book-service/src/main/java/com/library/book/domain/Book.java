package com.library.book.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书实体
 */
@Data
@TableName("t_book")
public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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

    /** 状态：0-在库，1-借出，2-下架，3-遗失 */
    private Integer status;

    /** 总库存 */
    private Integer totalStock;

    /** 可用库存 */
    private Integer availableStock;

    /** 馆藏位置 */
    private String location;

    /** 索书号 */
    private String callNumber;

    /** 是否有电子版 */
    private Boolean hasEbook;

    /** 电子版URL */
    private String ebookUrl;

    /** 借阅次数 */
    private Integer borrowCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
