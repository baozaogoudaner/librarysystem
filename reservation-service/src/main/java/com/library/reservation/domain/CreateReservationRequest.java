package com.library.reservation.domain;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 创建预约请求
 */
@Data
public class CreateReservationRequest {
    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    @NotNull(message = "预约日期不能为空")
    private String date;

    @NotNull(message = "开始时间不能为空")
    private String startTime;

    @NotNull(message = "结束时间不能为空")
    private String endTime;
}
