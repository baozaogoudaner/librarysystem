package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图书预约 DTO（跨服务传输）
 */
@Data
public class BookReserveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long bookId;
    private String isbn;
    private String bookTitle;
    private LocalDateTime reserveTime;
    private LocalDateTime expireTime;
    private LocalDateTime pickupTime;
    private Integer status;
    private LocalDateTime createTime;
}
