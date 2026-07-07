-- =====================================================
-- 借阅数据库 library_borrowing
-- =====================================================
CREATE DATABASE IF NOT EXISTS library_borrowing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE library_borrowing;

-- 借阅记录表
DROP TABLE IF EXISTS t_borrow;
CREATE TABLE t_borrow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '借阅ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    real_name VARCHAR(50) COMMENT '真实姓名',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    isbn VARCHAR(20) COMMENT 'ISBN',
    book_title VARCHAR(200) COMMENT '书名',
    borrow_time DATETIME NOT NULL COMMENT '借阅时间',
    due_date DATETIME NOT NULL COMMENT '应还日期',
    return_time DATETIME COMMENT '实际归还时间',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-借出中 1-已归还 2-逾期 3-续借中',
    renew_count INT DEFAULT 0 COMMENT '续借次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- 图书预约表
DROP TABLE IF EXISTS t_book_reserve;
CREATE TABLE t_book_reserve (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预约ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    real_name VARCHAR(50) COMMENT '真实姓名',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    isbn VARCHAR(20) COMMENT 'ISBN',
    book_title VARCHAR(200) COMMENT '书名',
    reserve_time DATETIME NOT NULL COMMENT '预约时间',
    expire_time DATETIME COMMENT '过期时间(可取后保留7天)',
    pickup_time DATETIME COMMENT '取书时间',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-等待中 1-可取 2-已取 3-已取消 4-已过期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书预约表';

-- Seata 分布式事务 undo_log 表
DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(100) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预置借阅数据
INSERT INTO t_borrow (user_id, username, book_id, isbn, book_title, borrow_time, due_date, status, renew_count) VALUES
(2, 'zhangsan', 1, '978-7-111-68412-3', 'Java编程思想（第4版）', '2026-06-01 10:00:00', '2026-07-01 10:00:00', 0, 0),
(3, 'lisi', 4, '978-7-111-52998-7', 'Spring实战（第5版）', '2026-06-05 14:30:00', '2026-07-05 14:30:00', 0, 1),
(2, 'zhangsan', 7, '978-7-115-56789-0', 'Python编程：从入门到实践（第3版）', '2026-05-15 09:00:00', '2026-06-14 09:00:00', 1, 0);

-- 预置预约数据
INSERT INTO t_book_reserve (user_id, username, book_id, isbn, book_title, reserve_time, status) VALUES
(3, 'lisi', 8, '978-7-302-61234-7', '人工智能：现代方法（第4版）', '2026-07-01 16:00:00', 0),
(4, 'wangwu', 5, '978-7-121-39455-8', 'MySQL技术内幕（第5版）', '2026-07-03 11:00:00', 1);
