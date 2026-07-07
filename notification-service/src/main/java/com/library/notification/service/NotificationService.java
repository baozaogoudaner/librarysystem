package com.library.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.constant.Constants;
import com.library.common.domain.NotificationDTO;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.notification.domain.Notification;
import com.library.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final JavaMailSender javaMailSender;

    /**
     * 创建通知
     */
    @Transactional
    public Notification createNotification(Long userId, Integer type, String title, String content,
                                            Long referenceId, String referenceType) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(Constants.NOTIFY_UNREAD);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notificationMapper.insert(notification);
        log.info("通知已创建: 用户={} 类型={} 标题={}", userId, type, title);
        return notification;
    }

    /**
     * 发送邮件通知
     */
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            javaMailSender.send(message);
            log.info("邮件已发送: 收件人={} 主题={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: 收件人={} 错误={}", to, e.getMessage());
        }
    }

    /**
     * 获取用户通知列表
     */
    public IPage<Notification> getUserNotifications(Long userId, Integer pageNum, Integer pageSize, Integer type) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(page, wrapper);
    }

    /**
     * 获取未读通知数
     */
    public Long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getStatus, Constants.NOTIFY_UNREAD));
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOTIFY_NOT_FOUND);
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        notification.setStatus(Constants.NOTIFY_READ);
        notificationMapper.updateById(notification);
    }

    /**
     * 全部标记为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getStatus, Constants.NOTIFY_UNREAD));
        unread.forEach(n -> n.setStatus(Constants.NOTIFY_READ));
        for (Notification n : unread) {
            notificationMapper.updateById(n);
        }
    }

    /**
     * 到期提醒（由定时任务触发）
     */
    public void sendDueReminder(Long userId, String email, String username, String bookTitle, Long borrowId) {
        String title = "还书到期提醒";
        String content = String.format("亲爱的 %s，您借阅的《%s》即将到期，请及时归还或办理续借。", username, bookTitle);
        createNotification(userId, Constants.NOTIFY_DUE, title, content, borrowId, "borrow");
        if (email != null && !email.isEmpty()) {
            sendEmail(email, title, content);
        }
    }

    /**
     * 逾期警告
     */
    public void sendOverdueAlert(Long userId, String email, String username, String bookTitle, Long borrowId) {
        String title = "图书逾期警告";
        String content = String.format("亲爱的 %s，您借阅的《%s》已逾期，请尽快归还图书，以免影响借阅权限。", username, bookTitle);
        createNotification(userId, Constants.NOTIFY_OVERDUE, title, content, borrowId, "borrow");
        if (email != null && !email.isEmpty()) {
            sendEmail(email, title, content);
        }
    }

    /**
     * 预约可取通知
     */
    public void sendReserveReady(Long userId, String email, String username, String bookTitle, Long reserveId) {
        String title = "预约图书可取通知";
        String content = String.format("亲爱的 %s，您预约的《%s》现已可取，请在7日内到馆取书，逾期将自动取消。", username, bookTitle);
        createNotification(userId, Constants.NOTIFY_RESERVE_READY, title, content, reserveId, "reserve");
        if (email != null && !email.isEmpty()) {
            sendEmail(email, title, content);
        }
    }

    /**
     * 荐购进度通知
     */
    public void sendPurchaseProgress(Long userId, String email, String username, String title, String progress, Long recommendId) {
        String notifyTitle = "荐购进度更新";
        String content = String.format("亲爱的 %s，您荐购的《%s》状态已更新为：%s", username, title, progress);
        createNotification(userId, Constants.NOTIFY_PURCHASE_PROGRESS, notifyTitle, content, recommendId, "purchase_recommend");
        if (email != null && !email.isEmpty()) {
            sendEmail(email, notifyTitle, content);
        }
    }

    /**
     * 库存预警通知（管理员）
     */
    public void sendStockAlert(Long adminId, String email, String bookTitle, int availableStock) {
        String title = "库存预警通知";
        String content = String.format("《%s》当前库存仅剩 %d 册，请及时补充采购。", bookTitle, availableStock);
        createNotification(adminId, Constants.NOTIFY_STOCK_ALERT, title, content, null, "book");
        if (email != null && !email.isEmpty()) {
            sendEmail(email, title, content);
        }
    }

    /**
     * 转换为DTO
     */
    public NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(notification, dto);
        return dto;
    }
}
