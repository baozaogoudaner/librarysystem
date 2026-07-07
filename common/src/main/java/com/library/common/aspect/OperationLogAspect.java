package com.library.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.annotation.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 操作日志 AOP 切面 - 自动拦截 @OperationLog 注解方法并记录操作日志
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 由具体服务注入 OperationLogService 实现，此处仅提供切面框架。
     * 日志数据通过 Spring Event 发布，各服务按需监听。
     * 简化方案：直接在切面中输出结构化日志。
     */
    @Around("@annotation(com.library.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String resultStatus = "SUCCESS";

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            resultStatus = "FAILED: " + e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 获取注解信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperationLog annotation = method.getAnnotation(OperationLog.class);

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String userId = "unknown";
            String ip = "unknown";
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                userId = request.getHeader("X-User-Id") != null ? request.getHeader("X-User-Id") : "system";
                ip = getClientIp(request);
            }

            // 获取方法参数
            Object[] args = joinPoint.getArgs();
            String params = "";
            try {
                ObjectMapper mapper = new ObjectMapper();
                params = mapper.writeValueAsString(args);
                if (params.length() > 2000) {
                    params = params.substring(0, 2000) + "...";
                }
            } catch (Exception ignored) {}

            // 输出操作日志（结构化格式，便于各服务收集）
            log.warn("OP_LOG | userId={} | type={} | desc={} | method={} | params={} | result={} | duration={}ms | ip={}",
                    userId, annotation.type(), annotation.desc(),
                    method.getDeclaringClass().getSimpleName() + "." + method.getName(),
                    params, resultStatus, duration, ip);
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
