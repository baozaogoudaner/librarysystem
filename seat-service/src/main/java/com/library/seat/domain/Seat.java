package com.library.seat.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 座位实体
 */
@Data
@TableName("t_seat")
public class Seat implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 座位编号 */
    private String seatNo;

    /** 所属阅览室ID */
    private Long roomId;

    /** 行号 */
    private Integer rowNum;

    /** 列号 */
    private Integer colNum;

    /** 是否有电源（0-否，1-是） */
    private Integer hasPower;

    /** 座位状态（0-可用，1-维护中） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
