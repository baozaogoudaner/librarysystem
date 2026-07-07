package com.library.borrowing.feign;

import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookFeignClientFallback implements FallbackFactory<BookFeignClient> {
    @Override
    public BookFeignClient create(Throwable cause) {
        log.error("BookFeignClient调用失败: {}", cause.getMessage());
        return new BookFeignClient() {
            @Override
            public Result<?> borrowBook(Long bookId) {
                return Result.error("图书服务不可用，请稍后重试");
            }
            @Override
            public Result<?> returnBook(Long bookId) {
                return Result.error("图书服务不可用，请稍后重试");
            }
        };
    }
}
