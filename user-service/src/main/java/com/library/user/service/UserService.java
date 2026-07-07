package com.library.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.constant.Constants;
import com.library.common.domain.UserDTO;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.common.utils.JwtUtils;
import com.library.user.domain.LoginRequest;
import com.library.user.domain.RegisterRequest;
import com.library.user.domain.User;
import com.library.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 校验图形验证码
     */
    private void validateCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }
        String key = Constants.REDIS_CAPTCHA_PREFIX + captchaId;
        String storedCode = stringRedisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
        }
        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }
        // 校验后立即删除，一次性使用
        stringRedisTemplate.delete(key);
    }

    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 校验图形验证码
        validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());

        // 检查用户名是否已存在
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.USER_STATUS_NORMAL);
        user.setViolationCount(0);
        user.setCreditScore(100);  // ★ V2.0 初始信用分100
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        log.info("用户注册成功: {}, 初始信用分: 100", request.getUsername());
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginRequest request) {
        // 校验图形验证码
        validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());

        // 检查登录失败次数（防暴力破解）
        String failKey = Constants.REDIS_LOGIN_FAIL_PREFIX + request.getUsername();
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= 5) {
            throw new BusinessException(ResultCode.USER_FROZEN.getCode(), "登录失败次数过多，请30分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 记录登录失败次数
            stringRedisTemplate.opsForValue().increment(failKey);
            stringRedisTemplate.expire(failKey, Constants.LOGIN_FAIL_LOCK_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 登录成功，清除失败计数
        stringRedisTemplate.delete(failKey);

        // 检查是否冻结（如果冻结时间已过，自动解冻）
        if (user.getStatus() == Constants.USER_STATUS_FROZEN) {
            if (user.getFreezeUntil() != null && user.getFreezeUntil().isBefore(LocalDateTime.now())) {
                // 自动解冻
                user.setStatus(Constants.USER_STATUS_NORMAL);
                user.setViolationCount(0);
                user.setFreezeUntil(null);
                user.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(user);
            }
        }

        // 生成 Token
        String token = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("status", user.getStatus());
        result.put("violationCount", user.getViolationCount());

        log.info("用户登录成功: {}", request.getUsername());
        return result;
    }

    /**
     * 获取用户信息
     */
    public UserDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToDTO(user);
    }

    /**
     * 根据ID获取用户（供 Feign 调用）
     */
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return convertToDTO(user);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public void updateUser(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (userDTO.getRealName() != null) user.setRealName(userDTO.getRealName());
        if (userDTO.getPhone() != null) user.setPhone(userDTO.getPhone());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 增加违规记录（供 Feign 调用）
     */
    @Transactional
    public void addViolation(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        int newCount = user.getViolationCount() + 1;
        user.setViolationCount(newCount);

        // ★ V2.0 信用积分 -10
        int newScore = Math.max(0, (user.getCreditScore() != null ? user.getCreditScore() : 100) - 10);
        user.setCreditScore(newScore);

        // 违规3次 或 信用分<30，冻结7天
        if (newCount >= Constants.MAX_VIOLATION_COUNT || newScore < 30) {
            user.setStatus(Constants.USER_STATUS_FROZEN);
            user.setFreezeUntil(LocalDateTime.now().plusDays(Constants.FREEZE_DAYS));
            log.warn("用户 {} 违规累计{}次/信用分{}，已冻结至 {}", user.getUsername(), newCount, newScore, user.getFreezeUntil());
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户 {} 违规+1，当前违规次数: {}, 信用分: {}", user.getUsername(), newCount, newScore);
    }

    /**
     * 检查用户是否被冻结
     */
    public boolean isUserFrozen(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == Constants.USER_STATUS_FROZEN) {
            // 检查冻结是否已过期
            if (user.getFreezeUntil() != null && user.getFreezeUntil().isBefore(LocalDateTime.now())) {
                // 自动解冻
                user.setStatus(Constants.USER_STATUS_NORMAL);
                user.setViolationCount(0);
                user.setFreezeUntil(null);
                user.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(user);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 获取所有用户列表（管理员）
     */
    public List<UserDTO> listAllUsers() {
        List<User> users = userMapper.selectList(null);
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * ★ V2.0 信用积分变动（加分/扣分）
     */
    @Transactional
    public void addCreditScore(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        int current = user.getCreditScore() != null ? user.getCreditScore() : 100;
        int newScore = Math.max(0, Math.min(100, current + delta));
        user.setCreditScore(newScore);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户 {} 信用分变动: {} {:+d} = {}", user.getUsername(), current, delta, newScore);
    }

    /**
     * 管理员修改用户状态
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        if (status == Constants.USER_STATUS_NORMAL) {
            user.setFreezeUntil(null);
            user.setViolationCount(0);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 管理员修改读者权限
     */
    @Transactional
    public void updateReaderPermission(Long userId, Integer readerType, Integer maxBorrowCount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (readerType != null) {
            user.setReaderType(readerType);
            // 根据读者类型自动设置默认借阅数
            if (maxBorrowCount == null) {
                switch (readerType) {
                    case 1: user.setMaxBorrowCount(20); break; // 教师
                    case 2: user.setMaxBorrowCount(5); break;  // 社会读者
                    default: user.setMaxBorrowCount(10); break; // 学生
                }
            }
        }
        if (maxBorrowCount != null) {
            user.setMaxBorrowCount(maxBorrowCount);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("读者权限更新: 用户={} 类型={} 最大借阅={}", user.getUsername(), readerType, maxBorrowCount);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
