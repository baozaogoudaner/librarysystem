package com.library.reservation.feign;

import com.library.common.domain.UserDTO;
import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务 Feign 降级工厂
 */
@Slf4j
@Component
public class UserFeignClientFallback implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("用户服务调用失败: {}", cause.getMessage());
        return new UserFeignClient() {
            @Override
            public Result<UserDTO> getUserById(Long id) {
                return Result.error("用户服务不可用");
            }

            @Override
            public Result<?> addViolation(Long userId) {
                return Result.error("用户服务不可用，无法记录违规");
            }

            @Override
            public Result<Boolean> checkFreeze(Long userId) {
                return Result.error("用户服务不可用");
            }

            @Override
            public Result<?> addCreditScore(Long userId, int delta) {
                return Result.error("用户服务不可用，无法更新信用分");
            }
        };
    }
}
