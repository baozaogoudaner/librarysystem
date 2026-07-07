package com.library.common.domain;

import lombok.Data;
import java.io.Serializable;

/**
 * 座位信息 DTO（跨服务传输）
 */
@Data
public class SeatDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String seatNo;
    private Long roomId;
    private String roomName;
    private Integer rowNum;
    private Integer colNum;
    private Integer hasPower;
    private Integer status;
}
