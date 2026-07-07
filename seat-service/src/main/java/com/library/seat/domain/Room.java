package com.library.seat.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 阅览室实体
 */
@Data
@TableName("t_room")
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 阅览室名称 */
    private String name;

    /** 楼层 */
    private Integer floor;

    /** 容量 */
    private Integer capacity;

    /** 开放时间 */
    private LocalTime openTime;

    /** 关闭时间 */
    private LocalTime closeTime;

    /** 状态（0-正常，1-关闭） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
