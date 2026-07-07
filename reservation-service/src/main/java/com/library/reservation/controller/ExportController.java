package com.library.reservation.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.library.common.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;

/**
 * 报表导出控制器 - Excel 流式导出
 */
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 导出预约记录报表
     */
    @OperationLog(type = "EXPORT_RESERVATION", desc = "导出预约记录报表")
    @GetMapping("/reservations")
    public void exportReservations(@RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   HttpServletResponse response) throws IOException {
        if (startDate == null) startDate = LocalDate.now().minusDays(30).toString();
        if (endDate == null) endDate = LocalDate.now().toString();

        String sql = "SELECT id, user_id, seat_no, room_name, date, start_time, end_time, " +
                     "CASE status WHEN 0 THEN '待签到' WHEN 1 THEN '使用中' WHEN 2 THEN '已完成' " +
                     "WHEN 3 THEN '已取消' WHEN 4 THEN '违规' END AS status_name, " +
                     "check_in_time, check_out_time, create_time " +
                     "FROM t_reservation WHERE date BETWEEN ? AND ? ORDER BY create_time DESC";

        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, startDate, endDate);

        setExcelResponse(response, "预约记录报表");
        List<List<String>> head = Arrays.asList(
                Arrays.asList("预约ID"), Arrays.asList("用户ID"), Arrays.asList("座位号"),
                Arrays.asList("阅览室"), Arrays.asList("日期"), Arrays.asList("开始时间"),
                Arrays.asList("结束时间"), Arrays.asList("状态"), Arrays.asList("签到时间"),
                Arrays.asList("签退时间"), Arrays.asList("创建时间"));

        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : data) {
            List<Object> r = new ArrayList<>();
            for (String col : Arrays.asList("id","user_id","seat_no","room_name","date",
                    "start_time","end_time","status_name","check_in_time","check_out_time","create_time")) {
                r.add(row.getOrDefault(col, ""));
            }
            rows.add(r);
        }

        EasyExcel.write(response.getOutputStream()).head(head).sheet("预约记录").doWrite(rows);
    }

    private void setExcelResponse(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition",
                "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
    }
}
