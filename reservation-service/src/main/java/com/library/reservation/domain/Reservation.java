package com.library.reservation.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约实体
 */
@Data
@TableName("t_reservation")
public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long seatId;
    private String seatNo;
    private String roomName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    /** 状态（0-待签到，1-使用中，2-已完成，3-已取消，4-违规） */
    private Integer status;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
