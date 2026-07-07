-- =============================================
-- 图书馆预约系统 - 预约数据库
-- =============================================

CREATE DATABASE IF NOT EXISTS `library_reservation` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `library_reservation`;

-- 预约表
DROP TABLE IF EXISTS `t_reservation`;
CREATE TABLE `t_reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `seat_id` BIGINT NOT NULL COMMENT '座位ID',
    `seat_no` VARCHAR(20) NOT NULL COMMENT '座位编号',
    `room_name` VARCHAR(100) DEFAULT NULL COMMENT '阅览室名称',
    `date` DATE NOT NULL COMMENT '预约日期',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0-待签到，1-使用中，2-已完成，3-已取消，4-违规）',
    `check_in_time` DATETIME DEFAULT NULL COMMENT '签到时间',
    `check_out_time` DATETIME DEFAULT NULL COMMENT '签退时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_seat_id` (`seat_id`),
    KEY `idx_date` (`date`),
    KEY `idx_status` (`status`),
    KEY `idx_seat_date` (`seat_id`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- Seata AT 模式 undo_log 表
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
    `branch_id` BIGINT NOT NULL COMMENT 'branch transaction id',
    `xid` VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context` VARCHAR(128) NOT NULL COMMENT 'undo_log context, such as serialization',
    `rollback_info` LONGBLOB NOT NULL COMMENT 'rollback info',
    `log_status` INT NOT NULL COMMENT '0:normal status, 1:defense status',
    `log_created` DATETIME(6) NOT NULL COMMENT 'create datetime',
    `log_modified` DATETIME(6) NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT transaction mode undo table';
