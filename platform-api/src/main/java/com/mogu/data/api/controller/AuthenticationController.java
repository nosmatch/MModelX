package com.mogu.data.api.controller;

import com.mogu.data.common.dto.RegisterRequest;
import com.mogu.data.common.entity.User;
import com.mogu.data.common.result.Result;
import com.mogu.data.common.security.UserDetailsServiceImpl;
import com.mogu.data.common.service.AuthenticationService;
import com.mogu.data.common.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、登出、注册等认证相关操作
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、token管理等认证相关接口")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationService authenticationService;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return JWT token和用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT token")
    public Result<Map<String, Object>> login(@Valid @RequestBody com.mogu.data.common.dto.LoginRequest request) {
        log.info("User login attempt: {}", request.getUsername());

        try {
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 获取用户详情
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            // 获取用户实体信息
            User user = authenticationService.findByUsername(request.getUsername());

            // 构建响应
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("type", "Bearer");
            data.put("expiresIn", 86400); // 24小时
            data.put("user", com.mogu.data.common.dto.UserInfo.from(user));

            log.info("User {} logged in successfully", request.getUsername());
            return Result.success(data);

        } catch (Exception e) {
            log.error("User {} login failed: {}", request.getUsername(), e.getMessage());
            return Result.error(401, "用户名或密码错误");
        }
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，默认角色为ENGINEER")
    public Result<Map<String, Object>> register(@Valid @RequestBody com.mogu.data.common.dto.RegisterRequest request) {
        log.info("User registration attempt: {}", request.getUsername());

        try {
            // 检查用户名是否已存在
            if (authenticationService.existsByUsername(request.getUsername())) {
                return Result.error(400, "用户名已存在");
            }

            // 检查邮箱是否已存在
            if (authenticationService.existsByEmail(request.getEmail())) {
                return Result.error(400, "邮箱已被使用");
            }

            // 创建用户
            User user = authenticationService.createUser(request);

            // 自动登录生成token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            // 构建响应
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("type", "Bearer");
            data.put("user", com.mogu.data.common.dto.UserInfo.from(user));

            log.info("User {} registered successfully", request.getUsername());
            return Result.success(data, "注册成功");

        } catch (Exception e) {
            log.error("User registration failed for {}: {}", request.getUsername(), e.getMessage());
            return Result.error(500, "注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出（客户端需删除token）")
    public Result<Void> logout() {
        log.info("User logout");
        // JWT是无状态的，登出操作主要在客户端删除token
        // 这里可以添加日志记录、清理缓存等操作
        return Result.success(null, "登出成功");
    }

    /**
     * 刷新token
     *
     * @param request HTTP请求
     * @return 新的token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新token", description = "使用旧token刷新获取新token")
    public Result<Map<String, Object>> refreshToken(HttpServletRequest request) {
        String token = jwtUtil.extractToken(request.getHeader("Authorization"));

        if (token == null || jwtUtil.isTokenExpired(token)) {
            return Result.error(401, "Token无效或已过期");
        }

        try {
            String newToken = jwtUtil.refreshToken(token);

            if (newToken == null) {
                return Result.error(500, "Token刷新失败");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("type", "Bearer");

            return Result.success(data, "Token刷新成功");

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return Result.error(500, "Token刷新失败");
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param authentication 认证信息
     * @return 用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "根据JWT token获取当前登录用户的信息")
    public Result<com.mogu.data.common.dto.UserInfo> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error(401, "未登录");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = authenticationService.findByUsername(userDetails.getUsername());

        return Result.success(com.mogu.data.common.dto.UserInfo.from(user));
    }
}
