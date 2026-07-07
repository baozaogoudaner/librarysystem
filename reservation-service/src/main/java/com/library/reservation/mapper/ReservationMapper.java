package com.library.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.reservation.domain.Reservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
