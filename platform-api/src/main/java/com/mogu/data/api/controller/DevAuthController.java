package com.mogu.data.api.controller;

import com.mogu.data.common.entity.User;
import com.mogu.data.common.result.Result;
import com.mogu.data.common.security.UserDetailsServiceImpl;
import com.mogu.data.common.service.AuthenticationService;
import com.mogu.data.common.util.JwtUtil;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 开发环境认证控制器
 *
 * 仅在 dev profile 下生效，提供自动获取测试 token 的接口，
 * 解决开发环境频繁遇到 401 Unauthorized 的问题。
 *
 * @author MModelX Team
 * @since 2026-05-26
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile("dev")
@Hidden
public class DevAuthController {

    private final AuthenticationService authenticationService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;

    /**
     * 获取开发环境测试 token
     *
     * 自动创建或使用已有的 dev 测试账号，返回 JWT token。
     * 前端开发环境在首次请求时自动调用此接口获取 token。
     *
     * @return token 和用户信息
     */
    @GetMapping("/dev-token")
    public Result<Map<String, Object>> getDevToken() {
        String devUsername = "dev";
        String devPassword = "dev123";

        // 查找或创建 dev 测试用户（角色为 ADMIN，拥有全部权限）
        User user = authenticationService.findOrCreateDevUser(devUsername, devPassword);

        // 加载用户详情用于生成 token
        UserDetails userDetails = userDetailsService.loadUserByUsername(devUsername);
        String token = jwtUtil.generateToken(userDetails);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("type", "Bearer");
        data.put("expiresIn", 86400);
        data.put("username", user.getUsername());
        data.put("role", user.getRole().name());

        log.info("Dev token issued for user: {}", devUsername);
        return Result.success(data);
    }
}
