package com.library.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.notification.domain.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
