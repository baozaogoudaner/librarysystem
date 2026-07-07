package com.library.reservation.controller;

import com.library.common.result.Result;
import com.library.reservation.domain.CreateReservationRequest;
import com.library.reservation.domain.Reservation;
import com.library.reservation.service.ReservationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "03-预约管理")
@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @ApiOperation("创建预约")
    @PostMapping("/create")
    public Result<Reservation> createReservation(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateReservationRequest request) {
        return Result.success("预约成功", reservationService.createReservation(userId, request));
    }

    @ApiOperation("签到")
    @PutMapping("/check-in/{id}")
    public Result<?> checkIn(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        reservationService.checkIn(userId, id);
        return Result.success("签到成功", null);
    }

    @ApiOperation("签退")
    @PutMapping("/check-out/{id}")
    public Result<?> checkOut(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        reservationService.checkOut(userId, id);
        return Result.success("签退成功", null);
    }

    @ApiOperation("取消预约")
    @PutMapping("/cancel/{id}")
    public Result<?> cancelReservation(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        reservationService.cancelReservation(userId, id);
        return Result.success("取消成功", null);
    }

    @ApiOperation("获取用户的预约列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserReservations(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        List<Reservation> records = reservationService.getMyReservations(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return Result.success(result);
    }

    @ApiOperation("管理员获取全部预约")
    @GetMapping("/admin/list")
    public Result<Map<String, Object>> listAllReservations(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        String today = LocalDate.now().toString();
        List<Reservation> records = reservationService.listAllReservations(today, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return Result.success(result);
    }

    @ApiOperation("获取当前进行中的预约")
    @GetMapping("/current")
    public Result<Reservation> getCurrentReservation(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(reservationService.getCurrentReservation(userId));
    }
}
