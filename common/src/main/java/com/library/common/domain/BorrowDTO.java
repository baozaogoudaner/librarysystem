package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅记录 DTO（跨服务传输）
 */
@Data
public class BorrowDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long bookId;
    private String isbn;
    private String bookTitle;
    private LocalDateTime borrowTime;
    private LocalDateTime dueDate;
    private LocalDateTime returnTime;
    private Integer status;
    private Integer renewCount;
    private LocalDateTime createTime;
}
