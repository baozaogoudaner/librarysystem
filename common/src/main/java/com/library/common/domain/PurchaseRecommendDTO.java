package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 荐购记录 DTO（跨服务传输）
 */
@Data
public class PurchaseRecommendDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String reason;
    private Integer status;
    private String reviewComment;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
