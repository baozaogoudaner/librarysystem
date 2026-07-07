package com.library.reservation.controller;

import com.library.common.annotation.OperationLog;
import com.library.common.result.Result;
import com.library.reservation.domain.CreateReservationRequest;
import com.library.reservation.domain.Reservation;
import com.library.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 预约控制器
 */
@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 创建预约
     */
    @PostMapping("/create")
    public Result<Reservation> createReservation(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.createReservation(userId, request);
        return Result.success("预约成功", reservation);
    }

    /**
     * 取消预约
     */
    @PutMapping("/cancel/{id}")
    public Result<?> cancelReservation(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        reservationService.cancelReservation(userId, id);
        return Result.success("取消成功", null);
    }

    /**
     * 签到
     */
    @PutMapping("/check-in/{id}")
    public Result<?> checkIn(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        reservationService.checkIn(userId, id);
        return Result.success("签到成功", null);
    }

    /**
     * 签退
     */
    @PutMapping("/check-out/{id}")
    public Result<?> checkOut(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        reservationService.checkOut(userId, id);
        return Result.success("签退成功", null);
    }

    /**
     * 查询我的预约记录
     */
    @GetMapping("/my")
    public Result<List<Reservation>> getMyReservations(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(reservationService.getMyReservations(userId));
    }

    /**
     * 查询当前有效预约
     */
    @GetMapping("/current")
    public Result<Reservation> getCurrentReservation(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(reservationService.getCurrentReservation(userId));
    }

    /**
     * 管理员查询所有预约
     */
    @GetMapping("/list")
    public Result<List<Reservation>> listAllReservations(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer status) {
        return Result.success(reservationService.listAllReservations(date, status));
    }

    /**
     * 管理员取消预约
     */
    @OperationLog(type = "ADMIN_CANCEL_RESERVATION", desc = "管理员取消预约")
    @PutMapping("/admin/cancel/{id}")
    public Result<?> adminCancelReservation(@PathVariable Long id) {
        reservationService.adminCancelReservation(id);
        return Result.success("取消成功", null);
    }
}
