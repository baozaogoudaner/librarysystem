package com.library.borrowing.feign;

import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignClientFallback implements FallbackFactory<UserFeignClient> {
    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("UserFeignClient调用失败: {}", cause.getMessage());
        return new UserFeignClient() {
            @Override
            public Result<?> getUserById(Long id) {
                return Result.error("用户服务不可用，请稍后重试");
            }
        };
    }
}
