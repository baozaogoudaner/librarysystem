package com.library.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 预约消息事件 DTO（用于 RocketMQ 消息传递）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息类型 */
    private String type;

    /** 预约ID */
    private Long reservationId;

    /** 用户ID */
    private Long userId;

    /** 座位编号 */
    private String seatNo;

    /** 阅览室名称 */
    private String roomName;

    /** 预约日期 */
    private String date;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 附加信息 */
    private String message;

    // 消息类型常量
    public static final String TYPE_RESERVATION_CREATED = "RESERVATION_CREATED";
    public static final String TYPE_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
    public static final String TYPE_CHECK_IN = "CHECK_IN";
    public static final String TYPE_CHECK_OUT = "CHECK_OUT";
    public static final String TYPE_VIOLATION = "VIOLATION";
}
