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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Api(tags = "01-用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    @ApiOperation("获取图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 80);
        String captchaId = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(
                Constants.REDIS_CAPTCHA_PREFIX + captchaId, captcha.getCode(), 5, TimeUnit.MINUTES);
        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("captchaImg", captcha.getImageBase64Data());
        return Result.success(result);
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功", null);
    }

    @ApiOperation("用户登录")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "username", value = "用户名", required = true),
        @ApiImplicitParam(name = "password", value = "密码", required = true),
        @ApiImplicitParam(name = "captchaId", value = "验证码ID"),
        @ApiImplicitParam(name = "captchaCode", value = "验证码")
    })
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = userService.login(request);
        return Result.success("登录成功", data);
    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/info")
    public Result<UserDTO> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @ApiOperation("根据ID获取用户（Feign内部调用）")
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @ApiOperation("修改用户信息")
    @PutMapping("/update")
    public Result<?> updateUser(@RequestHeader("X-User-Id") Long userId,
                                @RequestBody UserDTO userDTO) {
        userService.updateUser(userId, userDTO);
        return Result.success("修改成功", null);
    }

    @ApiOperation("增加违规记录（Feign内部调用）")
    @PutMapping("/violation/{userId}")
    public Result<?> addViolation(@PathVariable Long userId) {
        userService.addViolation(userId);
        return Result.success("违规记录已添加", null);
    }

    @ApiOperation("检查用户冻结状态（Feign内部调用）")
    @GetMapping("/check-freeze/{userId}")
    public Result<Boolean> checkFreeze(@PathVariable Long userId) {
        return Result.success(userService.isUserFrozen(userId));
    }

    @ApiOperation("获取所有用户列表（管理员）")
    @GetMapping("/list")
    public Result<List<UserDTO>> listAllUsers() {
        return Result.success(userService.listAllUsers());
    }

    @ApiOperation("信用积分变动（Feign内部调用）")
    @PutMapping("/credit/{userId}")
    public Result<?> addCreditScore(@PathVariable Long userId, @RequestParam int delta) {
        userService.addCreditScore(userId, delta);
        return Result.success("信用分已更新", null);
    }

    @ApiOperation("管理员修改用户状态")
    @OperationLog(type = "UPDATE_USER_STATUS", desc = "修改用户状态")
    @PutMapping("/status/{userId}")
    public Result<?> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        userService.updateUserStatus(userId, status);
        return Result.success("状态修改成功", null);
    }

    @ApiOperation("管理员修改读者权限")
    @OperationLog(type = "UPDATE_READER_PERM", desc = "修改读者权限")
    @PutMapping("/permission/{userId}")
    public Result<?> updateReaderPermission(@PathVariable Long userId,
                                            @RequestParam(required = false) Integer readerType,
                                            @RequestParam(required = false) Integer maxBorrowCount) {
        userService.updateReaderPermission(userId, readerType, maxBorrowCount);
        return Result.success("权限修改成功", null);
    }
}
