package com.library.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 - 标记需要记录操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作类型，如：ADD_SEAT, UPDATE_USER_STATUS, CANCEL_RESERVATION */
    String type();
    /** 操作描述 */
    String desc() default "";
}
