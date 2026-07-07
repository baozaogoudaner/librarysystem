package com.library.book.feign;

import com.library.common.domain.BorrowDTO;
import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * BorrowingFeignClient 熔断降级处理
 */
@Slf4j
@Component
public class BorrowingFeignClientFallback implements FallbackFactory<BorrowingFeignClient> {

    @Override
    public BorrowingFeignClient create(Throwable cause) {
        log.warn("借阅服务调用失败，触发降级: {}", cause.getMessage());
        return userId -> {
            log.warn("降级返回空借阅历史，userId={}", userId);
            return Result.success("服务暂不可用，返回空数据", Collections.emptyList());
        };
    }
}
