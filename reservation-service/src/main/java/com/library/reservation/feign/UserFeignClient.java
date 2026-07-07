package com.library.reservation.feign;

import com.library.common.domain.UserDTO;
import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "user-service", fallbackFactory = UserFeignClientFallback.class)
public interface UserFeignClient {

    @GetMapping("/user/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    @PutMapping("/user/violation/{userId}")
    Result<?> addViolation(@PathVariable("userId") Long userId);

    @GetMapping("/user/check-freeze/{userId}")
    Result<Boolean> checkFreeze(@PathVariable("userId") Long userId);

    /** ★ V2.0 信用积分变动 */
    @PutMapping("/user/credit/{userId}")
    Result<?> addCreditScore(@PathVariable("userId") Long userId, @RequestParam("delta") int delta);
}
