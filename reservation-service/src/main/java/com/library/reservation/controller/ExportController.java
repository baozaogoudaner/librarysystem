package com.library.reservation.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@Api(tags = "03-预约管理-导出")
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    @ApiOperation("导出预约记录（Excel）")
    @GetMapping("/reservations")
    public void exportReservations(HttpServletResponse response) {
        // 导出实现（预留）
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
}
