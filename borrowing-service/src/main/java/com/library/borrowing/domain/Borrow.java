package com.library.borrowing.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅记录实体
 */
@Data
@TableName("t_borrow")
public class Borrow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String realName;
    private Long bookId;
    private String isbn;
    private String bookTitle;

    /** 借阅时间 */
    private LocalDateTime borrowTime;

    /** 应还日期 */
    private LocalDateTime dueDate;

    /** 实际归还时间 */
    private LocalDateTime returnTime;

    /** 状态：0-借出中，1-已归还，2-逾期，3-续借中 */
    private Integer status;

    /** 续借次数 */
    private Integer renewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
