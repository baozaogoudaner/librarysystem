package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息 DTO（跨服务传输）
 */
@Data
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer role;
    private Integer status;
    private Integer violationCount;
    private Integer creditScore;
    private Integer readerType;
    private Integer maxBorrowCount;
    private Integer totalBorrows;
    private LocalDateTime freezeUntil;
}
