package com.library.book.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 荐购记录实体
 */
@Data
@TableName("t_purchase_recommend")
public class PurchaseRecommend implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String realName;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String reason;

    /** 状态：0-待审核，1-已采纳，2-已拒绝，3-已采购 */
    private Integer status;

    private String reviewComment;
    private String reviewerName;
    private LocalDateTime reviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
