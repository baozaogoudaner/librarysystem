package com.library.borrowing.feign;

import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallbackFactory = UserFeignClientFallback.class)
public interface UserFeignClient {
    @GetMapping("/user/{id}")
    Result<?> getUserById(@PathVariable("id") Long id);
}
