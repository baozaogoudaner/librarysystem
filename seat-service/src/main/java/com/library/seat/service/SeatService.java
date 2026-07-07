package com.library.seat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.constant.Constants;
import com.library.common.domain.SeatDTO;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.seat.domain.Room;
import com.library.seat.domain.Seat;
import com.library.seat.mapper.RoomMapper;
import com.library.seat.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 座位服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 获取所有座位列表
     */
    public List<SeatDTO> listAllSeats() {
        List<Seat> seats = seatMapper.selectList(null);
        return seats.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 获取某阅览室的座位
     */
    public List<SeatDTO> listSeatsByRoom(Long roomId) {
        List<Seat> seats = seatMapper.selectList(
                new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, roomId).orderByAsc(Seat::getRowNum).orderByAsc(Seat::getColNum));
        return seats.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 查询单个座位
     */
    public SeatDTO getSeatById(Long seatId) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException(ResultCode.SEAT_NOT_FOUND);
        }
        return convertToDTO(seat);
    }

    /**
     * 查询某日期某时段可用的座位
     */
    public List<SeatDTO> getAvailableSeats(Long roomId, String date, String startTime, String endTime) {
        // 获取该阅览室所有可用座位
        List<Seat> seats = seatMapper.selectList(
                new LambdaQueryWrapper<Seat>()
                        .eq(Seat::getRoomId, roomId)
                        .eq(Seat::getStatus, Constants.SEAT_STATUS_AVAILABLE));

        List<SeatDTO> result = new ArrayList<>();
        for (Seat seat : seats) {
            // 从 Redis 检查该座位在该时段是否已被预约
            String redisKey = Constants.REDIS_SEAT_STATUS_PREFIX + date + ":" + seat.getId();
            Boolean isReserved = checkTimeConflictInRedis(redisKey, startTime, endTime);
            if (!isReserved) {
                SeatDTO dto = convertToDTO(seat);
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * 检查座位在某时段是否可用（基于 Redis 缓存）
     */
    public boolean isSeatAvailable(Long seatId, String date, String startTime, String endTime) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null || seat.getStatus() != Constants.SEAT_STATUS_AVAILABLE) {
            return false;
        }

        String redisKey = Constants.REDIS_SEAT_STATUS_PREFIX + date + ":" + seatId;
        return !checkTimeConflictInRedis(redisKey, startTime, endTime);
    }

    /**
     * 在 Redis 中标记座位为已预约
     */
    public void markSeatReserved(Long seatId, String date, String startTime, String endTime, Long reservationId) {
        String redisKey = Constants.REDIS_SEAT_STATUS_PREFIX + date + ":" + seatId;
        String value = startTime + "-" + endTime + ":" + reservationId;
        redisTemplate.opsForSet().add(redisKey, value);
        // 设置过期时间为第二天凌晨
        redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
        log.info("Redis 标记座位已预约: key={}, value={}", redisKey, value);
    }

    /**
     * 从 Redis 中移除座位预约标记
     */
    public void unmarkSeatReserved(Long seatId, String date, String startTime, String endTime, Long reservationId) {
        String redisKey = Constants.REDIS_SEAT_STATUS_PREFIX + date + ":" + seatId;
        String value = startTime + "-" + endTime + ":" + reservationId;
        redisTemplate.opsForSet().remove(redisKey, value);
        log.info("Redis 移除座位预约标记: key={}, value={}", redisKey, value);
    }

    /**
     * 更新座位基本状态（管理员）
     */
    @Transactional
    public void updateSeatStatus(Long seatId, Integer status) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException(ResultCode.SEAT_NOT_FOUND);
        }
        seat.setStatus(status);
        seatMapper.updateById(seat);
    }

    /**
     * 获取所有阅览室
     */
    public List<Room> listAllRooms() {
        return roomMapper.selectList(new LambdaQueryWrapper<Room>().orderByAsc(Room::getFloor));
    }

    /**
     * 管理员新增座位
     */
    @Transactional
    public void addSeat(Seat seat) {
        seatMapper.insert(seat);
    }

    /**
     * 管理员新增阅览室
     */
    @Transactional
    public void addRoom(Room room) {
        roomMapper.insert(room);
    }

    /**
     * 检查 Redis 中是否有时间冲突
     */
    private Boolean checkTimeConflictInRedis(String redisKey, String startTime, String endTime) {
        Set<String> reservations = redisTemplate.opsForSet().members(redisKey);
        if (reservations == null || reservations.isEmpty()) {
            return false;
        }

        LocalTime newStart = LocalTime.parse(startTime);
        LocalTime newEnd = LocalTime.parse(endTime);

        for (String reservation : reservations) {
            String timeRange = reservation.split(":")[0];
            // 格式修正：因为 LocalTime 也有冒号，需要更精确的解析
            int lastColon = reservation.lastIndexOf(":");
            // 格式是 "HH:mm-HH:mm:reservationId"
            // 需要找到时间范围和预约ID的分隔
            String[] parts = reservation.split(":");
            // parts: [HH, mm-HH, mm, reservationId]
            if (parts.length >= 4) {
                String existStart = parts[0] + ":" + parts[1].split("-")[0];
                String existEnd = parts[1].split("-")[1] + ":" + parts[2];
                LocalTime existingStart = LocalTime.parse(existStart);
                LocalTime existingEnd = LocalTime.parse(existEnd);

                // 检查时间是否重叠
                if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取座位在某天的所有预约时段（用于前端展示）
     */
    public List<Map<String, String>> getSeatReservations(Long seatId, String date) {
        String redisKey = Constants.REDIS_SEAT_STATUS_PREFIX + date + ":" + seatId;
        Set<String> reservations = redisTemplate.opsForSet().members(redisKey);
        List<Map<String, String>> result = new ArrayList<>();

        if (reservations != null) {
            for (String reservation : reservations) {
                String[] parts = reservation.split(":");
                if (parts.length >= 4) {
                    Map<String, String> map = new HashMap<>();
                    map.put("startTime", parts[0] + ":" + parts[1].split("-")[0]);
                    map.put("endTime", parts[1].split("-")[1] + ":" + parts[2]);
                    map.put("reservationId", parts[3]);
                    result.add(map);
                }
            }
        }
        return result;
    }

    private SeatDTO convertToDTO(Seat seat) {
        SeatDTO dto = new SeatDTO();
        BeanUtils.copyProperties(seat, dto);
        // 查询阅览室名称
        Room room = roomMapper.selectById(seat.getRoomId());
        if (room != null) {
            dto.setRoomName(room.getName());
        }
        return dto;
    }
}
