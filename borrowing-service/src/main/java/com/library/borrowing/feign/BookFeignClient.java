package com.library.borrowing.feign;

import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "book-service", fallbackFactory = BookFeignClientFallback.class)
public interface BookFeignClient {
    @PutMapping("/book/borrow/{bookId}")
    Result<?> borrowBook(@PathVariable("bookId") Long bookId);

    @PutMapping("/book/return/{bookId}")
    Result<?> returnBook(@PathVariable("bookId") Long bookId);
}
