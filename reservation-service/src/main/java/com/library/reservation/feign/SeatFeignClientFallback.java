package com.library.reservation.feign;

import com.library.common.domain.SeatDTO;
import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 座位服务 Feign 降级工厂
 */
@Slf4j
@Component
public class SeatFeignClientFallback implements FallbackFactory<SeatFeignClient> {

    @Override
    public SeatFeignClient create(Throwable cause) {
        log.error("座位服务调用失败: {}", cause.getMessage());
        return new SeatFeignClient() {
            @Override
            public Result<SeatDTO> getSeatById(Long id) {
                return Result.error("座位服务不可用");
            }

            @Override
            public Result<Boolean> checkSeatAvailable(Long seatId, String date, String startTime, String endTime) {
                return Result.error("座位服务不可用");
            }

            @Override
            public Result<?> markSeatReserved(Long seatId, String date, String startTime, String endTime, Long reservationId) {
                return Result.error("座位服务不可用");
            }

            @Override
            public Result<?> unmarkSeatReserved(Long seatId, String date, String startTime, String endTime, Long reservationId) {
                return Result.error("座位服务不可用");
            }
        };
    }
}
