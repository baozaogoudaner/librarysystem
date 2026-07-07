package com.library.common.result;

import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    BAD_REQUEST(400, "请求参数错误"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    USER_PASSWORD_ERROR(1003, "密码错误"),
    USER_FROZEN(1004, "账号已被冻结，暂时无法预约"),
    USER_LOGIN_EXPIRED(1005, "登录已过期，请重新登录"),

    // 座位相关 2xxx
    SEAT_NOT_FOUND(2001, "座位不存在"),
    SEAT_NOT_AVAILABLE(2002, "座位当前不可用"),
    SEAT_ALREADY_RESERVED(2003, "该座位在此时段已被预约"),

    // 预约相关 3xxx
    RESERVATION_NOT_FOUND(3001, "预约记录不存在"),
    RESERVATION_TIME_CONFLICT(3002, "您在该时段已有预约"),
    RESERVATION_TIME_EXCEED(3003, "预约时长不能超过4小时"),
    RESERVATION_CHECK_IN_EXPIRED(3004, "签到已超时"),
    RESERVATION_ALREADY_CHECKED_IN(3005, "已经签到过了"),
    RESERVATION_CANNOT_CANCEL(3006, "当前状态无法取消预约"),
    RESERVATION_NOT_CHECKED_IN(3007, "尚未签到，无法签退"),

    // 验证码相关 4xxx
    CAPTCHA_EXPIRED(4001, "验证码已过期"),
    CAPTCHA_ERROR(4002, "验证码错误"),

    // 图书相关 5xxx
    BOOK_NOT_FOUND(5001, "图书不存在"),
    BOOK_ALREADY_EXISTS(5002, "该ISBN图书已存在"),
    BOOK_NOT_AVAILABLE(5003, "图书当前不可借"),
    BOOK_STOCK_INSUFFICIENT(5004, "图书库存不足"),
    BOOK_ALREADY_OFFLINE(5005, "图书已下架"),

    // 借阅相关 6xxx
    BORROW_NOT_FOUND(6001, "借阅记录不存在"),
    BORROW_LIMIT_EXCEEDED(6002, "已达到最大借阅数量"),
    BORROW_OVERDUE_EXISTS(6003, "存在逾期未还图书，无法借阅"),
    BORROW_ALREADY_RENEWED(6004, "已达到最大续借次数"),
    BORROW_CANNOT_RENEW(6005, "当前状态不允许续借"),
    BORROW_ALREADY_RETURNED(6006, "该图书已归还"),

    // 图书预约相关 7xxx
    BOOK_RESERVE_NOT_FOUND(7001, "预约记录不存在"),
    BOOK_RESERVE_ALREADY_EXISTS(7002, "您已预约过该图书"),
    BOOK_RESERVE_LIMIT_EXCEEDED(7003, "预约数量已达上限"),

    // 荐购相关 8xxx
    PURCHASE_RECOMMEND_NOT_FOUND(8001, "荐购记录不存在"),
    PURCHASE_RECOMMEND_DUPLICATE(8002, "该图书已在荐购列表中"),

    // 通知相关 9xxx
    NOTIFY_NOT_FOUND(9001, "通知不存在"),

    // OPAC/检索相关 10xxx
    SEARCH_KEYWORD_EMPTY(10001, "搜索关键词不能为空"),
    SEARCH_RESULT_EMPTY(10002, "未找到匹配的资源");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
