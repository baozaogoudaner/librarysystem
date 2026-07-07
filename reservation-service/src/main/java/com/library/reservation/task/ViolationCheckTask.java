package com.library.reservation.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.constant.Constants;
import com.library.common.domain.ReservationMessage;
import com.library.reservation.domain.Reservation;
import com.library.reservation.feign.SeatFeignClient;
import com.library.reservation.feign.UserFeignClient;
import com.library.reservation.mapper.ReservationMapper;
import com.library.reservation.mq.ReservationMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 违规检测定时任务
 * 每分钟执行一次，检查超时未签到的预约
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViolationCheckTask {

    private final ReservationMapper reservationMapper;
    private final UserFeignClient userFeignClient;
    private final SeatFeignClient seatFeignClient;
    private final ReservationMessageProducer messageProducer;

    /**
     * 每分钟检查一次超时未签到的预约
     */
    @Scheduled(fixedRate = 60000)
    public void checkViolations() {
        log.debug("开始违规检测...");

        // 查询今天所有待签到状态的预约
        List<Reservation> pendingReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, LocalDate.now())
                        .eq(Reservation::getStatus, Constants.RESERVATION_PENDING));

        for (Reservation reservation : pendingReservations) {
            // 计算签到截止时间：预约开始时间 + 30分钟
            LocalDateTime deadline = LocalDateTime.of(reservation.getDate(), reservation.getStartTime())
                    .plusMinutes(Constants.CHECK_IN_TIMEOUT_MINUTES);

            if (LocalDateTime.now().isAfter(deadline)) {
                // 超时未签到，标记为违规
                reservation.setStatus(Constants.RESERVATION_VIOLATED);
                reservation.setUpdateTime(LocalDateTime.now());
                reservationMapper.updateById(reservation);

                // 释放座位（从 Redis 移除标记）
                try {
                    seatFeignClient.unmarkSeatReserved(
                            reservation.getSeatId(),
                            reservation.getDate().toString(),
                            reservation.getStartTime().toString(),
                            reservation.getEndTime().toString(),
                            reservation.getId());
                } catch (Exception e) {
                    log.error("释放座位失败: reservationId={}", reservation.getId(), e);
                }

                // 增加用户违规记录
                try {
                    userFeignClient.addViolation(reservation.getUserId());
                } catch (Exception e) {
                    log.error("增加违规记录失败: userId={}", reservation.getUserId(), e);
                }

                log.warn("预约超时未签到，已标记违规: reservationId={}, userId={}, seatNo={}",
                        reservation.getId(), reservation.getUserId(), reservation.getSeatNo());

                // 发送 RocketMQ 违规通知消息
                messageProducer.sendReservationMessage(ReservationMessage.builder()
                        .type(ReservationMessage.TYPE_VIOLATION)
                        .reservationId(reservation.getId())
                        .userId(reservation.getUserId())
                        .seatNo(reservation.getSeatNo())
                        .roomName(reservation.getRoomName())
                        .date(reservation.getDate().toString())
                        .startTime(reservation.getStartTime().toString())
                        .endTime(reservation.getEndTime().toString())
                        .message("超时未签到，系统自动标记为违规")
                        .build());
            }
        }

        // 检查已到结束时间但仍在使用中的预约，自动签退
        List<Reservation> inUseReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, LocalDate.now())
                        .eq(Reservation::getStatus, Constants.RESERVATION_IN_USE));

        for (Reservation reservation : inUseReservations) {
            LocalDateTime endDateTime = LocalDateTime.of(reservation.getDate(), reservation.getEndTime());
            if (LocalDateTime.now().isAfter(endDateTime)) {
                // 自动签退
                reservation.setStatus(Constants.RESERVATION_COMPLETED);
                reservation.setCheckOutTime(LocalDateTime.now());
                reservation.setUpdateTime(LocalDateTime.now());
                reservationMapper.updateById(reservation);

                try {
                    seatFeignClient.unmarkSeatReserved(
                            reservation.getSeatId(),
                            reservation.getDate().toString(),
                            reservation.getStartTime().toString(),
                            reservation.getEndTime().toString(),
                            reservation.getId());
                } catch (Exception e) {
                    log.error("自动签退释放座位失败: reservationId={}", reservation.getId(), e);
                }

                log.info("预约已自动签退: reservationId={}", reservation.getId());
            }
        }
    }
}
