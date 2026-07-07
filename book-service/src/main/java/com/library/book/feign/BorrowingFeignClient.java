package com.library.book.feign;

import com.library.common.domain.BorrowDTO;
import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 借阅服务 Feign 客户端（book-service 调用 borrowing-service）
 */
@FeignClient(name = "borrowing-service", path = "/borrow",
        fallbackFactory = BorrowingFeignClientFallback.class)
public interface BorrowingFeignClient {

    /**
     * 获取用户借阅历史（供推荐模块使用）
     */
    @GetMapping("/internal/history/{userId}")
    Result<List<BorrowDTO>> getBorrowHistory(@PathVariable("userId") Long userId);
}
