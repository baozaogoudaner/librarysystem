package com.library.notification.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 通知定时任务 - 每日检查到期/逾期/库存情况
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduledTask {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 每日上午9点检查到期提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkDueReminders() {
        log.info("开始执行每日到期提醒检查...");
        try {
            restTemplate.getForObject("http://localhost:8085/borrow/overdue", String.class);
            log.info("到期提醒检查完成");
        } catch (Exception e) {
            log.error("到期提醒检查失败: {}", e.getMessage());
        }
    }

    /**
     * 每小时检查库存预警
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkStockAlerts() {
        log.debug("开始执行库存预警检查...");
        try {
            restTemplate.getForObject("http://localhost:8084/book/low-stock", String.class);
        } catch (Exception e) {
            log.debug("库存预警检查: {}", e.getMessage());
        }
    }
}
