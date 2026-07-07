-- =============================================
-- 图书馆预约系统 - 座位数据库
-- =============================================

CREATE DATABASE IF NOT EXISTS `library_seat` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `library_seat`;

-- 阅览室表
DROP TABLE IF EXISTS `t_room`;
CREATE TABLE `t_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '阅览室ID',
    `name` VARCHAR(100) NOT NULL COMMENT '阅览室名称',
    `floor` INT NOT NULL COMMENT '楼层',
    `capacity` INT NOT NULL COMMENT '容量',
    `open_time` TIME NOT NULL DEFAULT '08:00:00' COMMENT '开放时间',
    `close_time` TIME NOT NULL DEFAULT '22:00:00' COMMENT '关闭时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0-正常，1-关闭）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅览室表';

-- 座位表
DROP TABLE IF EXISTS `t_seat`;
CREATE TABLE `t_seat` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '座位ID',
    `seat_no` VARCHAR(20) NOT NULL COMMENT '座位编号',
    `room_id` BIGINT NOT NULL COMMENT '所属阅览室ID',
    `row_num` INT NOT NULL COMMENT '行号',
    `col_num` INT NOT NULL COMMENT '列号',
    `has_power` TINYINT NOT NULL DEFAULT 0 COMMENT '是否有电源（0-否，1-是）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '座位状态（0-可用，1-维护中）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seat_no` (`seat_no`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- 插入阅览室数据
INSERT INTO `t_room` (`name`, `floor`, `capacity`, `open_time`, `close_time`) VALUES
('一楼自习室A', 1, 30, '08:00:00', '22:00:00'),
('一楼自习室B', 1, 20, '08:00:00', '22:00:00'),
('二楼电子阅览室', 2, 24, '08:00:00', '21:00:00'),
('三楼考研自习室', 3, 36, '07:00:00', '23:00:00');

-- 一楼自习室A：5行6列 = 30个座位
INSERT INTO `t_seat` (`seat_no`, `room_id`, `row_num`, `col_num`, `has_power`) VALUES
('A-01', 1, 1, 1, 1), ('A-02', 1, 1, 2, 0), ('A-03', 1, 1, 3, 0), ('A-04', 1, 1, 4, 0), ('A-05', 1, 1, 5, 0), ('A-06', 1, 1, 6, 1),
('A-07', 1, 2, 1, 1), ('A-08', 1, 2, 2, 0), ('A-09', 1, 2, 3, 0), ('A-10', 1, 2, 4, 0), ('A-11', 1, 2, 5, 0), ('A-12', 1, 2, 6, 1),
('A-13', 1, 3, 1, 1), ('A-14', 1, 3, 2, 0), ('A-15', 1, 3, 3, 0), ('A-16', 1, 3, 4, 0), ('A-17', 1, 3, 5, 0), ('A-18', 1, 3, 6, 1),
('A-19', 1, 4, 1, 1), ('A-20', 1, 4, 2, 0), ('A-21', 1, 4, 3, 0), ('A-22', 1, 4, 4, 0), ('A-23', 1, 4, 5, 0), ('A-24', 1, 4, 6, 1),
('A-25', 1, 5, 1, 1), ('A-26', 1, 5, 2, 0), ('A-27', 1, 5, 3, 0), ('A-28', 1, 5, 4, 0), ('A-29', 1, 5, 5, 0), ('A-30', 1, 5, 6, 1);

-- 一楼自习室B：4行5列 = 20个座位
INSERT INTO `t_seat` (`seat_no`, `room_id`, `row_num`, `col_num`, `has_power`) VALUES
('B-01', 2, 1, 1, 1), ('B-02', 2, 1, 2, 0), ('B-03', 2, 1, 3, 0), ('B-04', 2, 1, 4, 0), ('B-05', 2, 1, 5, 1),
('B-06', 2, 2, 1, 1), ('B-07', 2, 2, 2, 0), ('B-08', 2, 2, 3, 0), ('B-09', 2, 2, 4, 0), ('B-10', 2, 2, 5, 1),
('B-11', 2, 3, 1, 1), ('B-12', 2, 3, 2, 0), ('B-13', 2, 3, 3, 0), ('B-14', 2, 3, 4, 0), ('B-15', 2, 3, 5, 1),
('B-16', 2, 4, 1, 1), ('B-17', 2, 4, 2, 0), ('B-18', 2, 4, 3, 0), ('B-19', 2, 4, 4, 0), ('B-20', 2, 4, 5, 1);

-- 二楼电子阅览室：4行6列 = 24个座位（全部有电源）
INSERT INTO `t_seat` (`seat_no`, `room_id`, `row_num`, `col_num`, `has_power`) VALUES
('C-01', 3, 1, 1, 1), ('C-02', 3, 1, 2, 1), ('C-03', 3, 1, 3, 1), ('C-04', 3, 1, 4, 1), ('C-05', 3, 1, 5, 1), ('C-06', 3, 1, 6, 1),
('C-07', 3, 2, 1, 1), ('C-08', 3, 2, 2, 1), ('C-09', 3, 2, 3, 1), ('C-10', 3, 2, 4, 1), ('C-11', 3, 2, 5, 1), ('C-12', 3, 2, 6, 1),
('C-13', 3, 3, 1, 1), ('C-14', 3, 3, 2, 1), ('C-15', 3, 3, 3, 1), ('C-16', 3, 3, 4, 1), ('C-17', 3, 3, 5, 1), ('C-18', 3, 3, 6, 1),
('C-19', 3, 4, 1, 1), ('C-20', 3, 4, 2, 1), ('C-21', 3, 4, 3, 1), ('C-22', 3, 4, 4, 1), ('C-23', 3, 4, 5, 1), ('C-24', 3, 4, 6, 1);

-- 三楼考研自习室：6行6列 = 36个座位
INSERT INTO `t_seat` (`seat_no`, `room_id`, `row_num`, `col_num`, `has_power`) VALUES
('D-01', 4, 1, 1, 1), ('D-02', 4, 1, 2, 0), ('D-03', 4, 1, 3, 0), ('D-04', 4, 1, 4, 0), ('D-05', 4, 1, 5, 0), ('D-06', 4, 1, 6, 1),
('D-07', 4, 2, 1, 1), ('D-08', 4, 2, 2, 0), ('D-09', 4, 2, 3, 0), ('D-10', 4, 2, 4, 0), ('D-11', 4, 2, 5, 0), ('D-12', 4, 2, 6, 1),
('D-13', 4, 3, 1, 1), ('D-14', 4, 3, 2, 0), ('D-15', 4, 3, 3, 0), ('D-16', 4, 3, 4, 0), ('D-17', 4, 3, 5, 0), ('D-18', 4, 3, 6, 1),
('D-19', 4, 4, 1, 1), ('D-20', 4, 4, 2, 0), ('D-21', 4, 4, 3, 0), ('D-22', 4, 4, 4, 0), ('D-23', 4, 4, 5, 0), ('D-24', 4, 4, 6, 1),
('D-25', 4, 5, 1, 1), ('D-26', 4, 5, 2, 0), ('D-27', 4, 5, 3, 0), ('D-28', 4, 5, 4, 0), ('D-29', 4, 5, 5, 0), ('D-30', 4, 5, 6, 1),
('D-31', 4, 6, 1, 1), ('D-32', 4, 6, 2, 0), ('D-33', 4, 6, 3, 0), ('D-34', 4, 6, 4, 0), ('D-35', 4, 6, 5, 0), ('D-36', 4, 6, 6, 1);

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
