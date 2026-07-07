-- =====================================================
-- 图书数据库 library_book
-- =====================================================
CREATE DATABASE IF NOT EXISTS library_book DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE library_book;

-- 图书表
DROP TABLE IF EXISTS t_book;
CREATE TABLE t_book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图书ID',
    isbn VARCHAR(20) NOT NULL COMMENT 'ISBN',
    title VARCHAR(200) NOT NULL COMMENT '书名',
    author VARCHAR(100) COMMENT '作者',
    publisher VARCHAR(100) COMMENT '出版社',
    publish_date DATETIME COMMENT '出版日期',
    category VARCHAR(50) COMMENT '分类',
    description TEXT COMMENT '简介',
    cover_url VARCHAR(500) COMMENT '封面URL',
    price DECIMAL(10,2) DEFAULT 0 COMMENT '价格',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-在库 1-借出 2-下架 3-遗失',
    total_stock INT DEFAULT 1 COMMENT '总库存',
    available_stock INT DEFAULT 1 COMMENT '可用库存',
    location VARCHAR(100) COMMENT '馆藏位置',
    call_number VARCHAR(50) COMMENT '索书号',
    has_ebook TINYINT(1) DEFAULT 0 COMMENT '是否有电子版',
    ebook_url VARCHAR(500) COMMENT '电子版URL',
    borrow_count INT DEFAULT 0 COMMENT '累计借阅次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_isbn (isbn),
    INDEX idx_title (title),
    INDEX idx_author (author),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 荐购表
DROP TABLE IF EXISTS t_purchase_recommend;
CREATE TABLE t_purchase_recommend (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '荐购ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    real_name VARCHAR(50) COMMENT '真实姓名',
    isbn VARCHAR(20) COMMENT 'ISBN',
    title VARCHAR(200) NOT NULL COMMENT '书名',
    author VARCHAR(100) COMMENT '作者',
    publisher VARCHAR(100) COMMENT '出版社',
    reason TEXT COMMENT '推荐理由',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待审核 1-已采纳 2-已拒绝 3-已采购',
    review_comment VARCHAR(500) COMMENT '审核意见',
    reviewer_name VARCHAR(50) COMMENT '审核人',
    review_time DATETIME COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_isbn (isbn),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='荐购表';

-- 预置图书数据
INSERT INTO t_book (isbn, title, author, publisher, category, description, price, total_stock, available_stock, location, call_number, borrow_count) VALUES
('978-7-111-68412-3', 'Java编程思想（第4版）', 'Bruce Eckel', '机械工业出版社', '计算机科学', 'Java经典入门书籍，全面讲解Java语言特性与编程思想。', 108.00, 10, 8, '3F-A区-12架', 'TP312/EC12', 156),
('978-7-115-54489-6', '深入理解计算机系统（第3版）', 'Randal E. Bryant', '人民邮电出版社', '计算机科学', '从程序员视角深入剖析计算机系统核心概念。', 139.00, 6, 3, '3F-A区-08架', 'TP303/BR3', 203),
('978-7-302-58123-5', '算法导论（第4版）', 'Thomas H. Cormen', '清华大学出版社', '计算机科学', '算法领域的经典教材，涵盖广泛的算法设计与分析方法。', 128.00, 8, 5, '3F-A区-05架', 'TP301/CO4', 189),
('978-7-111-52998-7', 'Spring实战（第5版）', 'Craig Walls', '人民邮电出版社', '计算机科学', '全面讲解Spring框架核心特性与实战应用。', 99.00, 12, 12, '3F-A区-15架', 'TP312/WA5', 134),
('978-7-121-39455-8', 'MySQL技术内幕（第5版）', '姜承尧', '电子工业出版社', '数据库', '深入MySQL内核，详解InnoDB存储引擎。', 118.00, 5, 2, '3F-B区-03架', 'TP311/MY5', 78),
('978-7-111-67890-1', '设计模式：可复用面向对象软件的基础', 'Erich Gamma', '机械工业出版社', '软件工程', 'GoF经典设计模式著作，23种设计模式详解。', 79.00, 7, 6, '3F-A区-10架', 'TP311/GA1', 267),
('978-7-115-56789-0', 'Python编程：从入门到实践（第3版）', 'Eric Matthes', '人民邮电出版社', '计算机科学', 'Python入门经典，项目驱动式学习。', 89.00, 15, 12, '3F-A区-01架', 'TP312/MA3', 312),
('978-7-302-61234-7', '人工智能：现代方法（第4版）', 'Stuart Russell', '清华大学出版社', '人工智能', 'AI领域权威教材，全面覆盖人工智能理论与应用。', 198.00, 4, 1, '3F-C区-06架', 'TP18/RU4', 145),
('978-7-111-70123-4', '数据结构与算法分析（C语言描述）', 'Mark Allen Weiss', '机械工业出版社', '计算机科学', '经典数据结构教材，理论与实践并重。', 69.00, 9, 7, '3F-A区-06架', 'TP311/WE3', 178),
('978-7-121-44567-8', 'Linux命令行与Shell脚本编程大全（第4版）', 'Richard Blum', '电子工业出版社', '操作系统', 'Linux Shell编程权威指南。', 109.00, 6, 4, '3F-B区-01架', 'TP316/BL4', 92),
('978-7-115-59876-5', 'React设计原理', '卡松', '人民邮电出版社', '前端开发', '深入React实现原理，剖析Fiber架构与Hooks。', 88.00, 8, 8, '3F-A区-20架', 'TP393/KA1', 56),
('978-7-111-71234-1', '深度学习', 'Ian Goodfellow', '人民邮电出版社', '人工智能', '深度学习领域奠基性经典著作。', 168.00, 5, 3, '3F-C区-08架', 'TP181/GO1', 234);

-- 预置荐购数据
INSERT INTO t_purchase_recommend (user_id, username, real_name, isbn, title, author, publisher, reason, status) VALUES
(2, 'zhangsan', '张三', '978-7-302-61235-4', '大规模分布式存储系统', '杨传辉', '清华大学出版社', '课程需要分布式系统相关参考书', 0),
(3, 'lisi', '李四', '978-7-115-59877-2', '微服务架构设计模式', 'Chris Richardson', '机械工业出版社', '公司项目需要，建议图书馆采购', 1);
