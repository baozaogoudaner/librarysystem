package com.library.reservation.feign;

import com.library.common.domain.SeatDTO;
import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 座位服务 Feign 客户端
 */
@FeignClient(name = "seat-service", fallbackFactory = SeatFeignClientFallback.class)
public interface SeatFeignClient {

    @GetMapping("/seat/{id}")
    Result<SeatDTO> getSeatById(@PathVariable("id") Long id);

    @GetMapping("/seat/check-available")
    Result<Boolean> checkSeatAvailable(
            @RequestParam("seatId") Long seatId,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime);

    @PutMapping("/seat/mark-reserved")
    Result<?> markSeatReserved(
            @RequestParam("seatId") Long seatId,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("reservationId") Long reservationId);

    @PutMapping("/seat/unmark-reserved")
    Result<?> unmarkSeatReserved(
            @RequestParam("seatId") Long seatId,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("reservationId") Long reservationId);
}
