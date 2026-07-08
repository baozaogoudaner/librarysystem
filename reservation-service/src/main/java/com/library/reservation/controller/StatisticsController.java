package com.library.reservation.controller;

import com.library.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@Api(tags = "03-预约管理-统计")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final JdbcTemplate jdbcTemplate;

    @ApiOperation("获取首页KPI指标")
    @GetMapping("/kpi")
    public Result<Map<String, Object>> getKpi() {
        String today = LocalDate.now().toString();
        Integer todayTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_reservation WHERE date = ?", Integer.class, today);
        Integer inUse = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_reservation WHERE date = ? AND status = 1", Integer.class, today);
        Integer monthViolations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_reservation WHERE status = 4 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)",
                Integer.class);
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("todayReservations", todayTotal != null ? todayTotal : 0);
        kpi.put("currentInUse", inUse != null ? inUse : 0);
        kpi.put("monthViolations", monthViolations != null ? monthViolations : 0);
        kpi.put("todayDate", today);
        return Result.success(kpi);
    }

    @ApiOperation("每日预约趋势")
    @GetMapping("/daily-trend")
    public Result<List<Map<String, Object>>> getDailyTrend(@RequestParam(defaultValue = "30") int days) {
        String sql = "SELECT date, COUNT(*) AS count FROM t_reservation WHERE date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) GROUP BY date ORDER BY date";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, days);
        return Result.success(data);
    }

    @ApiOperation("各阅览室实时占用率")
    @GetMapping("/room-occupancy")
    public Result<List<Map<String, Object>>> getRoomOccupancy() {
        String today = LocalDate.now().toString();
        String sql = "SELECT r.name AS roomName, r.capacity, IFNULL(sub.in_use, 0) AS inUseCount "
                   + "FROM library_seat.t_room r "
                   + "LEFT JOIN (SELECT t.room_id, COUNT(*) AS in_use "
                   + "FROM library_reservation.t_reservation res "
                   + "JOIN library_seat.t_seat t ON res.seat_id = t.id "
                   + "WHERE res.date = ? AND res.status = 1 "
                   + "GROUP BY t.room_id) sub ON r.id = sub.room_id";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, today);
        return Result.success(data);
    }

    @ApiOperation("预约热度热力图")
    @GetMapping("/hourly-heatmap")
    public Result<List<Map<String, Object>>> getHourlyHeatmap() {
        String startDate = LocalDate.now().minusDays(28).toString();
        String sql = "SELECT sub.dayOfWeek, sub.hour, COUNT(*) AS `count` FROM (SELECT (DAYOFWEEK(date) + 5) % 7 + 1 AS dayOfWeek, HOUR(start_time) AS hour FROM t_reservation WHERE date >= ?) sub GROUP BY sub.dayOfWeek, sub.hour ORDER BY sub.dayOfWeek, sub.hour";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, startDate);
        return Result.success(data);
    }

    @ApiOperation("违规类型分布")
    @GetMapping("/violation-type")
    public Result<List<Map<String, Object>>> getViolationType() {
        String sql = "SELECT CASE WHEN check_in_time IS NULL THEN '超时未签到' ELSE '其他违规' END AS violationType, COUNT(*) AS count FROM t_reservation WHERE status = 4 GROUP BY violationType";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
        return Result.success(data);
    }
}
