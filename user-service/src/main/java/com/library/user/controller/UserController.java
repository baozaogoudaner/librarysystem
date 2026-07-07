package com.library.user.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.library.common.annotation.OperationLog;
import com.library.common.constant.Constants;
import com.library.common.domain.UserDTO;
import com.library.common.result.Result;
import com.library.user.domain.LoginRequest;
import com.library.user.domain.RegisterRequest;
import com.library.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        // 使用 Hutool 生成线段干扰验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 80);
        String captchaId = UUID.randomUUID().toString();
        String code = captcha.getCode();

        // 存入 Redis，5 分钟过期
        stringRedisTemplate.opsForValue().set(
                Constants.REDIS_CAPTCHA_PREFIX + captchaId,
                code,
                5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("captchaImg", captcha.getImageBase64Data());
        return Result.success(result);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功", null);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = userService.login(request);
        return Result.success("登录成功", data);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserDTO> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        UserDTO user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    /**
     * 根据ID获取用户（供 Feign 内部调用）
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/update")
    public Result<?> updateUser(@RequestHeader("X-User-Id") Long userId,
                                @RequestBody UserDTO userDTO) {
        userService.updateUser(userId, userDTO);
        return Result.success("修改成功", null);
    }

    /**
     * 增加违规记录（供 Feign 内部调用）
     */
    @PutMapping("/violation/{userId}")
    public Result<?> addViolation(@PathVariable Long userId) {
        userService.addViolation(userId);
        return Result.success("违规记录已添加", null);
    }

    /**
     * 检查用户是否被冻结（供 Feign 内部调用）
     */
    @GetMapping("/check-freeze/{userId}")
    public Result<Boolean> checkFreeze(@PathVariable Long userId) {
        boolean frozen = userService.isUserFrozen(userId);
        return Result.success(frozen);
    }

    /**
     * 获取所有用户列表（管理员）
     */
    @GetMapping("/list")
    public Result<List<UserDTO>> listAllUsers() {
        List<UserDTO> users = userService.listAllUsers();
        return Result.success(users);
    }

    /**
     * ★ V2.0 信用积分变动（供Feign内部调用）
     */
    @PutMapping("/credit/{userId}")
    public Result<?> addCreditScore(@PathVariable Long userId, @RequestParam int delta) {
        userService.addCreditScore(userId, delta);
        return Result.success("信用分已更新", null);
    }

    /**
     * 管理员修改用户状态
     */
    @OperationLog(type = "UPDATE_USER_STATUS", desc = "修改用户状态")
    @PutMapping("/status/{userId}")
    public Result<?> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        userService.updateUserStatus(userId, status);
        return Result.success("状态修改成功", null);
    }

    /**
     * 管理员修改读者权限（读者类型、最大借阅数）
     */
    @OperationLog(type = "UPDATE_READER_PERM", desc = "修改读者权限")
    @PutMapping("/permission/{userId}")
    public Result<?> updateReaderPermission(@PathVariable Long userId,
                                            @RequestParam(required = false) Integer readerType,
                                            @RequestParam(required = false) Integer maxBorrowCount) {
        userService.updateReaderPermission(userId, readerType, maxBorrowCount);
        return Result.success("权限修改成功", null);
    }
}
