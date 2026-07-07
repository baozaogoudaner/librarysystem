package com.library.seat.controller;

import com.library.common.annotation.OperationLog;
import com.library.common.domain.SeatDTO;
import com.library.common.result.Result;
import com.library.seat.domain.Room;
import com.library.seat.domain.Seat;
import com.library.seat.service.SeatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "02-座位管理")
@RestController
@RequestMapping("/seat")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @ApiOperation("获取所有座位")
    @GetMapping("/list")
    public Result<List<SeatDTO>> listAllSeats() {
        return Result.success(seatService.listAllSeats());
    }

    @ApiOperation("获取指定阅览室的座位列表")
    @GetMapping("/room/{roomId}")
    public Result<List<SeatDTO>> listSeatsByRoom(@PathVariable Long roomId) {
        return Result.success(seatService.listSeatsByRoom(roomId));
    }

    @ApiOperation("查询单个座位详情")
    @GetMapping("/{id}")
    public Result<SeatDTO> getSeatById(@PathVariable Long id) {
        return Result.success(seatService.getSeatById(id));
    }

    @ApiOperation("查询可用座位")
    @GetMapping("/available")
    public Result<List<SeatDTO>> getAvailableSeats(
            @RequestParam Long roomId,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return Result.success(seatService.getAvailableSeats(roomId, date, startTime, endTime));
    }

    @ApiOperation("检查座位是否可用（Feign内部调用）")
    @GetMapping("/check-available")
    public Result<Boolean> checkSeatAvailable(
            @RequestParam Long seatId,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return Result.success(seatService.isSeatAvailable(seatId, date, startTime, endTime));
    }

    @ApiOperation("标记座位为已预约（Feign内部调用）")
    @PutMapping("/mark-reserved")
    public Result<?> markSeatReserved(
            @RequestParam Long seatId, @RequestParam String date,
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam Long reservationId) {
        seatService.markSeatReserved(seatId, date, startTime, endTime, reservationId);
        return Result.success();
    }

    @ApiOperation("取消座位预约标记（Feign内部调用）")
    @PutMapping("/unmark-reserved")
    public Result<?> unmarkSeatReserved(
            @RequestParam Long seatId, @RequestParam String date,
            @RequestParam String startTime, @RequestParam String endTime,
            @RequestParam Long reservationId) {
        seatService.unmarkSeatReserved(seatId, date, startTime, endTime, reservationId);
        return Result.success();
    }

    @ApiOperation("管理员更新座位状态")
    @PutMapping("/status/{id}")
    public Result<?> updateSeatStatus(@PathVariable Long id, @RequestParam Integer status) {
        seatService.updateSeatStatus(id, status);
        return Result.success();
    }

    @ApiOperation("获取座位某天的预约时段")
    @GetMapping("/reservations/{seatId}")
    public Result<List<Map<String, String>>> getSeatReservations(
            @PathVariable Long seatId, @RequestParam String date) {
        return Result.success(seatService.getSeatReservations(seatId, date));
    }

    @ApiOperation("获取所有阅览室")
    @GetMapping("/room/list")
    public Result<List<Room>> listAllRooms() {
        return Result.success(seatService.listAllRooms());
    }

    @ApiOperation("管理员新增座位")
    @OperationLog(type = "ADD_SEAT", desc = "新增座位")
    @PostMapping("/add")
    public Result<?> addSeat(@RequestBody Seat seat) {
        seatService.addSeat(seat);
        return Result.success();
    }

    @ApiOperation("管理员新增阅览室")
    @PostMapping("/room/add")
    public Result<?> addRoom(@RequestBody Room room) {
        seatService.addRoom(room);
        return Result.success();
    }
}
