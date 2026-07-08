package com.library.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.constant.Constants;
import com.library.common.domain.ReservationMessage;
import com.library.common.domain.SeatDTO;
import com.library.common.domain.UserDTO;
import com.library.common.exception.BusinessException;
import com.library.common.result.Result;
import com.library.common.result.ResultCode;
import com.library.reservation.domain.CreateReservationRequest;
import com.library.reservation.domain.Reservation;
import com.library.reservation.feign.SeatFeignClient;
import com.library.reservation.feign.UserFeignClient;
import com.library.reservation.mapper.ReservationMapper;
import com.library.reservation.mq.ReservationMessageProducer;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预约服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final UserFeignClient userFeignClient;
    private final SeatFeignClient seatFeignClient;
    private final ReservationMessageProducer messageProducer;
    private final RedissonClient redissonClient;

    /**
     * 创建预约（分布式事务）
     */
    @GlobalTransactional(name = "create-reservation", rollbackFor = Exception.class)
    public Reservation createReservation(Long userId, CreateReservationRequest request) {
        LocalDate date = LocalDate.parse(request.getDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        // 1. 校验预约时长不超过4小时（按分钟计算）
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes > Constants.MAX_RESERVATION_HOURS * 60 || minutes <= 0) {
            throw new BusinessException(ResultCode.RESERVATION_TIME_EXCEED);
        }

        // 2. 检查用户是否被冻结
        Result<Boolean> freezeResult = userFeignClient.checkFreeze(userId);
        if (freezeResult.isSuccess() && Boolean.TRUE.equals(freezeResult.getData())) {
            throw new BusinessException(ResultCode.USER_FROZEN);
        }

        // 3. 检查同一时段是否已有预约
        List<Reservation> existingReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .eq(Reservation::getDate, date)
                        .in(Reservation::getStatus, Constants.RESERVATION_PENDING, Constants.RESERVATION_IN_USE));

        for (Reservation existing : existingReservations) {
            if (startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime())) {
                throw new BusinessException(ResultCode.RESERVATION_TIME_CONFLICT);
            }
        }

        // 4. Redisson 分布式锁：防止同一座位同一时段并发预约
        String lockKey = Constants.REDIS_SEAT_LOCK_PREFIX + request.getSeatId()
                + ":" + request.getDate() + ":" + request.getStartTime() + "-" + request.getEndTime();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        Reservation reservation;
        SeatDTO seatDTO;
        try {
            locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ResultCode.SEAT_ALREADY_RESERVED);
            }

            // 5. 检查座位是否可用（Feign → seat-service）
            Result<Boolean> availableResult = seatFeignClient.checkSeatAvailable(
                    request.getSeatId(), request.getDate(), request.getStartTime(), request.getEndTime());
            if (!availableResult.isSuccess() || !Boolean.TRUE.equals(availableResult.getData())) {
                throw new BusinessException(ResultCode.SEAT_ALREADY_RESERVED);
            }

            // 6. 获取座位信息
            Result<SeatDTO> seatResult = seatFeignClient.getSeatById(request.getSeatId());
            if (!seatResult.isSuccess() || seatResult.getData() == null) {
                throw new BusinessException(ResultCode.SEAT_NOT_FOUND);
            }
            seatDTO = seatResult.getData();

            // 7. 创建预约记录
            reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setSeatId(request.getSeatId());
            reservation.setSeatNo(seatDTO.getSeatNo());
            reservation.setRoomName(seatDTO.getRoomName());
            reservation.setDate(date);
            reservation.setStartTime(startTime);
            reservation.setEndTime(endTime);
            reservation.setStatus(Constants.RESERVATION_PENDING);
            reservation.setCreateTime(LocalDateTime.now());
            reservation.setUpdateTime(LocalDateTime.now());
            reservationMapper.insert(reservation);

            // 8. 在 Redis 中标记座位为已预约（Feign → seat-service）
            seatFeignClient.markSeatReserved(
                    request.getSeatId(), request.getDate(),
                    request.getStartTime(), request.getEndTime(),
                    reservation.getId());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }

        log.info("预约创建成功: userId={}, seatNo={}, date={}, time={}-{}",
                userId, seatDTO.getSeatNo(), date, startTime, endTime);

        // 8. 发送 RocketMQ 预约成功消息
        messageProducer.sendReservationMessage(ReservationMessage.builder()
                .type(ReservationMessage.TYPE_RESERVATION_CREATED)
                .reservationId(reservation.getId())
                .userId(userId)
                .seatNo(seatDTO.getSeatNo())
                .roomName(seatDTO.getRoomName())
                .date(date.toString())
                .startTime(startTime.toString())
                .endTime(endTime.toString())
                .message("预约创建成功")
                .build());

        return reservation;
    }

    /**
     * 取消预约
     */
    @GlobalTransactional(name = "cancel-reservation", rollbackFor = Exception.class)
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_FOUND);
        }

        // 只有本人或管理员可以取消
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 只有待签到状态可以取消
        if (reservation.getStatus() != Constants.RESERVATION_PENDING) {
            throw new BusinessException(ResultCode.RESERVATION_CANNOT_CANCEL);
        }

        reservation.setStatus(Constants.RESERVATION_CANCELLED);
        reservation.setUpdateTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        // 从 Redis 移除座位预约标记
        seatFeignClient.unmarkSeatReserved(
                reservation.getSeatId(),
                reservation.getDate().toString(),
                reservation.getStartTime().toString(),
                reservation.getEndTime().toString(),
                reservation.getId());

        log.info("预约已取消: reservationId={}", reservationId);

        // 发送 RocketMQ 取消预约消息
        messageProducer.sendReservationMessage(ReservationMessage.builder()
                .type(ReservationMessage.TYPE_RESERVATION_CANCELLED)
                .reservationId(reservationId)
                .userId(userId)
                .seatNo(reservation.getSeatNo())
                .roomName(reservation.getRoomName())
                .date(reservation.getDate().toString())
                .startTime(reservation.getStartTime().toString())
                .endTime(reservation.getEndTime().toString())
                .message("预约已取消")
                .build());
    }

    /**
     * 签到
     */
    @Transactional
    public void checkIn(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_FOUND);
        }

        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        if (reservation.getStatus() != Constants.RESERVATION_PENDING) {
            throw new BusinessException(ResultCode.RESERVATION_ALREADY_CHECKED_IN);
        }

        // 检查是否超时（超过预约开始时间30分钟）
        LocalDateTime deadline = LocalDateTime.of(reservation.getDate(), reservation.getStartTime())
                .plusMinutes(Constants.CHECK_IN_TIMEOUT_MINUTES);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(ResultCode.RESERVATION_CHECK_IN_EXPIRED);
        }

        reservation.setStatus(Constants.RESERVATION_IN_USE);
        reservation.setCheckInTime(LocalDateTime.now());
        reservation.setUpdateTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        log.info("签到成功: reservationId={}", reservationId);

        // ★ V2.0 信用积分 +2（准时签到奖励）
        try {
            userFeignClient.addCreditScore(userId, 2);
        } catch (Exception e) {
            log.error("信用积分更新失败: userId={}", userId, e);
        }

        // 发送 RocketMQ 签到消息
        messageProducer.sendReservationMessage(ReservationMessage.builder()
                .type(ReservationMessage.TYPE_CHECK_IN)
                .reservationId(reservationId)
                .userId(userId)
                .seatNo(reservation.getSeatNo())
                .roomName(reservation.getRoomName())
                .date(reservation.getDate().toString())
                .startTime(reservation.getStartTime().toString())
                .endTime(reservation.getEndTime().toString())
                .message("用户已签到")
                .build());
    }

    /**
     * 签退
     */
    @GlobalTransactional(name = "check-out-reservation", rollbackFor = Exception.class)
    public void checkOut(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_FOUND);
        }

        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        if (reservation.getStatus() != Constants.RESERVATION_IN_USE) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_CHECKED_IN);
        }

        reservation.setStatus(Constants.RESERVATION_COMPLETED);
        reservation.setCheckOutTime(LocalDateTime.now());
        reservation.setUpdateTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        // 从 Redis 移除座位预约标记（提前释放）
        seatFeignClient.unmarkSeatReserved(
                reservation.getSeatId(),
                reservation.getDate().toString(),
                reservation.getStartTime().toString(),
                reservation.getEndTime().toString(),
                reservation.getId());

        log.info("签退成功: reservationId={}", reservationId);

        // ★ V2.0 信用积分 +1（提前签退 ≥ 30分钟）
        try {
            long minutesEarly = Duration.between(LocalDateTime.now(),
                    LocalDateTime.of(reservation.getDate(), reservation.getEndTime())).toMinutes();
            if (minutesEarly >= 30) {
                userFeignClient.addCreditScore(userId, 1);
                log.info("提前签退奖励: userId={}, +1信用分", userId);
            }
        } catch (Exception e) {
            log.error("信用积分更新失败: userId={}", userId, e);
        }

        // 发送 RocketMQ 签退消息
        messageProducer.sendReservationMessage(ReservationMessage.builder()
                .type(ReservationMessage.TYPE_CHECK_OUT)
                .reservationId(reservationId)
                .userId(userId)
                .seatNo(reservation.getSeatNo())
                .roomName(reservation.getRoomName())
                .date(reservation.getDate().toString())
                .startTime(reservation.getStartTime().toString())
                .endTime(reservation.getEndTime().toString())
                .message("用户已签退")
                .build());
    }

    /**
     * 查询我的预约记录
     */
    public List<Reservation> getMyReservations(Long userId) {
        return reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .orderByDesc(Reservation::getCreateTime));
    }

    /**
     * 查询当前有效预约
     */
    public Reservation getCurrentReservation(Long userId) {
        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .eq(Reservation::getDate, LocalDate.now())
                        .in(Reservation::getStatus, Constants.RESERVATION_PENDING, Constants.RESERVATION_IN_USE)
                        .orderByAsc(Reservation::getStartTime));
        return reservations.isEmpty() ? null : reservations.get(0);
    }

    /**
     * 管理员查询所有预约
     */
    public List<Reservation> listAllReservations(String date, Integer status) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (date != null && !date.isEmpty()) {
            wrapper.eq(Reservation::getDate, LocalDate.parse(date));
        }
        if (status != null) {
            wrapper.eq(Reservation::getStatus, status);
        }
        wrapper.orderByDesc(Reservation::getCreateTime);
        return reservationMapper.selectList(wrapper);
    }

    /**
     * 管理员取消预约
     */
    @GlobalTransactional(name = "admin-cancel-reservation", rollbackFor = Exception.class)
    public void adminCancelReservation(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.RESERVATION_NOT_FOUND);
        }

        if (reservation.getStatus() == Constants.RESERVATION_PENDING
                || reservation.getStatus() == Constants.RESERVATION_IN_USE) {
            reservation.setStatus(Constants.RESERVATION_CANCELLED);
            reservation.setUpdateTime(LocalDateTime.now());
            reservationMapper.updateById(reservation);

            seatFeignClient.unmarkSeatReserved(
                    reservation.getSeatId(),
                    reservation.getDate().toString(),
                    reservation.getStartTime().toString(),
                    reservation.getEndTime().toString(),
                    reservation.getId());
        }
    }
}
