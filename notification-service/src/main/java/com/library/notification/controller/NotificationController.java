package com.library.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.domain.NotificationDTO;
import com.library.common.result.Result;
import com.library.notification.domain.Notification;
import com.library.notification.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取用户通知列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer type) {
        IPage<Notification> page = notificationService.getUserNotifications(userId, pageNum, pageSize, type);
        List<NotificationDTO> dtos = page.getRecords().stream()
                .map(notificationService::convertToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    /**
     * 获取未读通知数
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/read/{notificationId}")
    public Result<?> markAsRead(@PathVariable Long notificationId,
                                @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAsRead(notificationId, userId);
        return Result.success("已标记为已读", null);
    }

    /**
     * 全部标记为已读
     */
    @PutMapping("/read-all")
    public Result<?> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success("已全部标记为已读", null);
    }

    /**
     * 发送通知（内部调用/测试用）
     */
    @PostMapping("/send")
    public Result<?> sendNotification(@RequestBody @Valid SendNotificationRequest request) {
        notificationService.createNotification(request.getUserId(), request.getType(),
                request.getTitle(), request.getContent(),
                request.getReferenceId(), request.getReferenceType());
        return Result.success("通知已发送", null);
    }
}

@Data
class SendNotificationRequest {
    @NotBlank private Long userId;
    @NotBlank private Integer type;
    @NotBlank private String title;
    private String content;
    private Long referenceId;
    private String referenceType;
}
