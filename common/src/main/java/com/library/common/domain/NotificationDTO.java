package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息 DTO（跨服务传输）
 */
@Data
public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private Integer status;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createTime;
}
