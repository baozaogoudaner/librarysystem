package com.library.reservation.mq;

import com.alibaba.fastjson.JSON;
import com.library.common.domain.ReservationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息生产者
 * 用于发送预约相关的异步消息通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationMessageProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /** Topic 常量 */
    public static final String TOPIC_RESERVATION = "reservation-topic";

    /**
     * 发送预约消息
     */
    public void sendReservationMessage(ReservationMessage message) {
        try {
            String jsonMsg = JSON.toJSONString(message);
            rocketMQTemplate.convertAndSend(TOPIC_RESERVATION, jsonMsg);
            log.info("发送 RocketMQ 消息成功: type={}, reservationId={}, userId={}",
                    message.getType(), message.getReservationId(), message.getUserId());
        } catch (Exception e) {
            // 消息发送失败不影响主业务流程，仅记录日志
            log.error("发送 RocketMQ 消息失败: type={}, reservationId={}",
                    message.getType(), message.getReservationId(), e);
        }
    }
}
