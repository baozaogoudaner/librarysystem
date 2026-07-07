package com.library.reservation.controller;

import com.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 数据统计控制器 - 为管理后台 ECharts 大屏提供数据
 */
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * KPI 指标卡片数据
     */
    @GetMapping("/kpi")
    public Result<Map<String, Object>> getKpi() {
        Map<String, Object> kpi = new LinkedHashMap<>();
        String today = LocalDate.now().toString();

        // 今日预约总数
        Integer todayTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_reservation WHERE date = ?", Integer.class, today);
        kpi.put("todayReservations", todayTotal != null ? todayTotal : 0);

        // 当前在座人数（使用中）
        Integer inUse = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_reservation WHERE date = ? AND status = 1", Integer.class, today);
        kpi.put("currentInUse", inUse != null ? inUse : 0);

        // 本月违规总数
        String monthStart = today.substring(0, 7) + "-01";
        Integer monthViolations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_reservation WHERE date >= ? AND status = 4", Integer.class, monthStart);
        kpi.put("monthViolations", monthViolations != null ? monthViolations : 0);

        // 总用户数（跨库查询，通过 user-service 更合适，这里简化）
        kpi.put("todayDate", today);

        return Result.success(kpi);
    }

    /**
     * 近N天每日预约量趋势（折线图）
     */
    @GetMapping("/daily-trend")
    public Result<List<Map<String, Object>>> getDailyTrend(@RequestParam(defaultValue = "30") int days) {
        String sql = "SELECT date, COUNT(*) AS count FROM t_reservation " +
                     "WHERE date >= ? GROUP BY date ORDER BY date";
        String startDate = LocalDate.now().minusDays(days).toString();
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, startDate);
        return Result.success(data);
    }

    /**
     * 各阅览室占用率（柱状图）
     */
    @GetMapping("/room-occupancy")
    public Result<List<Map<String, Object>>> getRoomOccupancy() {
        String today = LocalDate.now().toString();
        // 查询各阅览室当前使用中的预约数
        String sql = "SELECT r.name AS roomName, r.capacity AS capacity, " +
                     "COALESCE(res.in_use_count, 0) AS inUseCount " +
                     "FROM library_seat.t_room r " +
                     "LEFT JOIN (SELECT room_name, COUNT(*) AS in_use_count " +
                     "           FROM t_reservation WHERE date = ? AND status = 1 " +
                     "           GROUP BY room_name) res ON r.name = res.room_name " +
                     "ORDER BY r.floor";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, today);
        return Result.success(data);
    }

    /**
     * 预约热度热力图数据（小时 × 星期）
     */
    @GetMapping("/hourly-heatmap")
    public Result<List<Map<String, Object>>> getHourlyHeatmap() {
        String startDate = LocalDate.now().minusDays(28).toString();
        // 子查询避免 MySQL only_full_group_by
        String sql = "SELECT sub.dayOfWeek, sub.hour, COUNT(*) AS `count` "
                   + "FROM (SELECT (DAYOFWEEK(date) + 5) % 7 + 1 AS dayOfWeek, "
                   + "HOUR(start_time) AS hour "
                   + "FROM t_reservation WHERE date >= ?) sub "
                   + "GROUP BY sub.dayOfWeek, sub.hour ORDER BY sub.dayOfWeek, sub.hour";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, startDate);
        return Result.success(data);
    }

    /**
     * 违规类型分布（饼图）
     */
    @GetMapping("/violation-type")
    public Result<List<Map<String, Object>>> getViolationType() {
        // status=4 为违规，细分类型通过备注字段简单分类
        String sql = "SELECT " +
                     "CASE " +
                     "  WHEN check_in_time IS NULL THEN '超时未签到' " +
                     "  ELSE '其他违规' " +
                     "END AS violationType, " +
                     "COUNT(*) AS count " +
                     "FROM t_reservation WHERE status = 4 " +
                     "GROUP BY CASE WHEN check_in_time IS NULL THEN '超时未签到' ELSE '其他违规' END";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
        return Result.success(data);
    }
}
