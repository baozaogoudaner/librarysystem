-- =====================================================
-- 通知数据库 library_notification
-- =====================================================
CREATE DATABASE IF NOT EXISTS library_notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE library_notification;

-- 通知表
DROP TABLE IF EXISTS t_notification;
CREATE TABLE t_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '类型: 0-到期提醒 1-逾期警告 2-预约可取 3-荐购进度 4-库存预警',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-未读 1-已读',
    reference_id BIGINT COMMENT '关联记录ID',
    reference_type VARCHAR(50) COMMENT '关联类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 预置通知数据
INSERT INTO t_notification (user_id, type, title, content, status, reference_id, reference_type) VALUES
(2, 0, '还书到期提醒', '亲爱的张三，您借阅的《Java编程思想（第4版）》将于2026-07-01到期，请及时归还或办理续借。', 0, 1, 'borrow'),
(3, 0, '还书到期提醒', '亲爱的李四，您借阅的《Spring实战（第5版）》即将到期，请及时归还。', 0, 2, 'borrow'),
(3, 2, '预约图书可取通知', '亲爱的李四，您预约的《人工智能：现代方法（第4版）》现已可取，请在7日内到馆取书。', 0, 1, 'reserve'),
(1, 4, '库存预警通知', '《MySQL技术内幕（第5版）》当前库存仅剩2册，请及时补充采购。', 0, 5, 'book');
