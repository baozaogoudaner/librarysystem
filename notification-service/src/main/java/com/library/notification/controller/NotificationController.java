package com.library.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.domain.NotificationDTO;
import com.library.common.result.Result;
import com.library.notification.domain.Notification;
import com.library.notification.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "06-通知管理")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @ApiOperation("获取用户通知列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> listNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<Notification> page = notificationService.getUserNotifications(userId, pageNum, pageSize, null);
        List<NotificationDTO> dtos = page.getRecords().stream()
                .map(notificationService::convertToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    @ApiOperation("获取未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @ApiOperation("标记通知为已读")
    @PutMapping("/read/{id}")
    public Result<?> markAsRead(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @ApiOperation("标记全部已读")
    @PutMapping("/read-all")
    public Result<?> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success("已全部标为已读");
    }
}
