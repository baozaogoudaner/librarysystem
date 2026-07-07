package com.library.common.constant;

/**
 * 系统常量
 */
public class Constants {

    /** 用户角色：普通用户 */
    public static final int ROLE_USER = 0;
    /** 用户角色：管理员 */
    public static final int ROLE_ADMIN = 1;

    /** 用户状态：正常 */
    public static final int USER_STATUS_NORMAL = 0;
    /** 用户状态：冻结 */
    public static final int USER_STATUS_FROZEN = 1;

    /** 座位状态：可用 */
    public static final int SEAT_STATUS_AVAILABLE = 0;
    /** 座位状态：维护中 */
    public static final int SEAT_STATUS_MAINTENANCE = 1;

    /** 预约状态：待签到 */
    public static final int RESERVATION_PENDING = 0;
    /** 预约状态：使用中 */
    public static final int RESERVATION_IN_USE = 1;
    /** 预约状态：已完成 */
    public static final int RESERVATION_COMPLETED = 2;
    /** 预约状态：已取消 */
    public static final int RESERVATION_CANCELLED = 3;
    /** 预约状态：违规 */
    public static final int RESERVATION_VIOLATED = 4;

    /** 违规次数上限 */
    public static final int MAX_VIOLATION_COUNT = 3;
    /** 冻结天数 */
    public static final int FREEZE_DAYS = 7;
    /** 签到超时时间（分钟） */
    public static final int CHECK_IN_TIMEOUT_MINUTES = 30;
    /** 最大预约时长（小时） */
    public static final int MAX_RESERVATION_HOURS = 4;

    /** Redis Key 前缀 */
    public static final String REDIS_SEAT_STATUS_PREFIX = "seat:status:";
    public static final String REDIS_SEAT_LOCK_PREFIX = "seat:lock:";
    public static final String REDIS_CAPTCHA_PREFIX = "captcha:";
    public static final String REDIS_LOGIN_FAIL_PREFIX = "login:fail:";

    /** 登录失败最大次数 */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;
    /** 登录失败锁定时间（分钟） */
    public static final int LOGIN_FAIL_LOCK_MINUTES = 30;

    // ==================== 图书相关 ====================
    /** 图书状态：在库 */
    public static final int BOOK_STATUS_IN = 0;
    /** 图书状态：借出 */
    public static final int BOOK_STATUS_OUT = 1;
    /** 图书状态：下架 */
    public static final int BOOK_STATUS_OFFLINE = 2;
    /** 图书状态：遗失 */
    public static final int BOOK_STATUS_LOST = 3;

    // ==================== 借阅相关 ====================
    /** 借阅状态：借出中 */
    public static final int BORROW_STATUS_ACTIVE = 0;
    /** 借阅状态：已归还 */
    public static final int BORROW_STATUS_RETURNED = 1;
    /** 借阅状态：逾期 */
    public static final int BORROW_STATUS_OVERDUE = 2;
    /** 借阅状态：续借中 */
    public static final int BORROW_STATUS_RENEWED = 3;
    /** 最大借阅数量 */
    public static final int MAX_BORROW_COUNT = 10;
    /** 默认借阅天数 */
    public static final int DEFAULT_BORROW_DAYS = 30;
    /** 续借天数 */
    public static final int RENEW_DAYS = 15;
    /** 最大续借次数 */
    public static final int MAX_RENEW_COUNT = 2;

    // ==================== 预约(图书)相关 ====================
    /** 预约状态：等待中 */
    public static final int BOOK_RESERVE_WAITING = 0;
    /** 预约状态：可取 */
    public static final int BOOK_RESERVE_READY = 1;
    /** 预约状态：已取 */
    public static final int BOOK_RESERVE_FULFILLED = 2;
    /** 预约状态：已取消 */
    public static final int BOOK_RESERVE_CANCELLED = 3;
    /** 预约状态：已过期 */
    public static final int BOOK_RESERVE_EXPIRED = 4;

    // ==================== 荐购相关 ====================
    /** 荐购状态：待审核 */
    public static final int PURCHASE_RECOMMEND_PENDING = 0;
    /** 荐购状态：已采纳 */
    public static final int PURCHASE_RECOMMEND_APPROVED = 1;
    /** 荐购状态：已拒绝 */
    public static final int PURCHASE_RECOMMEND_REJECTED = 2;
    /** 荐购状态：已采购 */
    public static final int PURCHASE_RECOMMEND_PURCHASED = 3;

    // ==================== 通知相关 ====================
    /** 通知类型：到期提醒 */
    public static final int NOTIFY_DUE = 0;
    /** 通知类型：逾期警告 */
    public static final int NOTIFY_OVERDUE = 1;
    /** 通知类型：预约可取 */
    public static final int NOTIFY_RESERVE_READY = 2;
    /** 通知类型：荐购进度 */
    public static final int NOTIFY_PURCHASE_PROGRESS = 3;
    /** 通知类型：库存预警 */
    public static final int NOTIFY_STOCK_ALERT = 4;
    /** 通知状态：未读 */
    public static final int NOTIFY_UNREAD = 0;
    /** 通知状态：已读 */
    public static final int NOTIFY_READ = 1;

    // ==================== 库存预警 ====================
    /** 库存预警阈值 */
    public static final int STOCK_ALERT_THRESHOLD = 3;

    // ==================== Redis Key 扩展 ====================
    public static final String REDIS_BOOK_STOCK_PREFIX = "book:stock:";
    public static final String REDIS_BOOK_BORROW_LOCK = "book:borrow:lock:";
    public static final String REDIS_HOT_BOOKS_KEY = "book:hot:rank";
    public static final String REDIS_NOTIFY_PREFIX = "notify:";
}
