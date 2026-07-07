package com.library.borrowing.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图书预约实体
 */
@Data
@TableName("t_book_reserve")
public class BookReserve implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String realName;
    private Long bookId;
    private String isbn;
    private String bookTitle;

    /** 预约时间 */
    private LocalDateTime reserveTime;

    /** 预约过期时间（有书可取后保留7天） */
    private LocalDateTime expireTime;

    /** 取书时间 */
    private LocalDateTime pickupTime;

    /** 状态：0-等待中，1-可取，2-已取，3-已取消，4-已过期 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
