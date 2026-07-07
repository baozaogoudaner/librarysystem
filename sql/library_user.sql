-- =============================================
-- 图书馆预约系统 - 用户数据库
-- =============================================

CREATE DATABASE IF NOT EXISTS `library_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `library_user`;

-- 用户表
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色（0-普通用户，1-管理员）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0-正常，1-冻结）',
    `violation_count` INT NOT NULL DEFAULT 0 COMMENT '违规次数',
    `freeze_until` DATETIME DEFAULT NULL COMMENT '冻结截止时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入管理员账号 (密码: admin123, BCrypt加密)
-- 注意：首次启动后建议通过注册接口创建用户，或使用以下预设密码
-- 以下 BCrypt 哈希对应密码 '123456'
INSERT INTO `t_user` (`username`, `password`, `real_name`, `role`) VALUES
('admin', '$2a$10$GnEdXYdKwwXzdMHSYS0uxOWKoPhPVkfmnFbjvz.iZXQUo4qELyc9a', '系统管理员', 1);

-- 插入测试用户 (密码: 123456)
INSERT INTO `t_user` (`username`, `password`, `real_name`, `phone`, `email`) VALUES
('zhangsan', '$2a$10$GnEdXYdKwwXzdMHSYS0uxOWKoPhPVkfmnFbjvz.iZXQUo4qELyc9a', '张三', '13800138001', 'zhangsan@example.com'),
('lisi', '$2a$10$GnEdXYdKwwXzdMHSYS0uxOWKoPhPVkfmnFbjvz.iZXQUo4qELyc9a', '李四', '13800138002', 'lisi@example.com'),
('wangwu', '$2a$10$GnEdXYdKwwXzdMHSYS0uxOWKoPhPVkfmnFbjvz.iZXQUo4qELyc9a', '王五', '13800138003', 'wangwu@example.com');

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
