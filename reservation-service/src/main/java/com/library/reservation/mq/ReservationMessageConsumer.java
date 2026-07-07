package com.library.reservation.mq;

import com.alibaba.fastjson.JSON;
import com.library.common.domain.ReservationMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息消费者
 * 监听预约相关消息，进行异步处理（如通知、日志记录等）
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "reservation-topic",
        consumerGroup = "reservation-consumer-group"
)
public class ReservationMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        try {
            ReservationMessage msg = JSON.parseObject(message, ReservationMessage.class);
            log.info("收到 RocketMQ 消息: type={}, reservationId={}, userId={}",
                    msg.getType(), msg.getReservationId(), msg.getUserId());

            switch (msg.getType()) {
                case ReservationMessage.TYPE_RESERVATION_CREATED:
                    handleReservationCreated(msg);
                    break;
                case ReservationMessage.TYPE_RESERVATION_CANCELLED:
                    handleReservationCancelled(msg);
                    break;
                case ReservationMessage.TYPE_CHECK_IN:
                    handleCheckIn(msg);
                    break;
                case ReservationMessage.TYPE_CHECK_OUT:
                    handleCheckOut(msg);
                    break;
                case ReservationMessage.TYPE_VIOLATION:
                    handleViolation(msg);
                    break;
                default:
                    log.warn("未知消息类型: {}", msg.getType());
            }
        } catch (Exception e) {
            log.error("处理 RocketMQ 消息失败: {}", message, e);
        }
    }

    /**
     * 处理预约创建消息 —— 发送预约成功通知
     */
    private void handleReservationCreated(ReservationMessage msg) {
        log.info("【通知】用户{}预约成功！座位: {} ({}), 日期: {}, 时间: {}-{}",
                msg.getUserId(), msg.getSeatNo(), msg.getRoomName(),
                msg.getDate(), msg.getStartTime(), msg.getEndTime());
    }

    /**
     * 处理预约取消消息
     */
    private void handleReservationCancelled(ReservationMessage msg) {
        log.info("【通知】用户{}已取消预约，座位: {} ({}), 日期: {}",
                msg.getUserId(), msg.getSeatNo(), msg.getRoomName(), msg.getDate());
    }

    /**
     * 处理签到消息
     */
    private void handleCheckIn(ReservationMessage msg) {
        log.info("【通知】用户{}已签到，座位: {} ({})",
                msg.getUserId(), msg.getSeatNo(), msg.getRoomName());
    }

    /**
     * 处理签退消息
     */
    private void handleCheckOut(ReservationMessage msg) {
        log.info("【通知】用户{}已签退，座位: {} ({})",
                msg.getUserId(), msg.getSeatNo(), msg.getRoomName());
    }

    /**
     * 处理违规消息 —— 发送违规警告通知
     */
    private void handleViolation(ReservationMessage msg) {
        log.warn("【违规通知】用户{}因超时未签到被记录违规！座位: {} ({}), 日期: {}, 详情: {}",
                msg.getUserId(), msg.getSeatNo(), msg.getRoomName(),
                msg.getDate(), msg.getMessage());
    }
}
