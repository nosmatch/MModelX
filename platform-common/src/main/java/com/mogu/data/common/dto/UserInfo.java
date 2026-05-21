package com.mogu.data.common.dto;

import com.mogu.data.common.entity.User;
import lombok.Data;

/**
 * 用户信息DTO
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Data
public class UserInfo {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;

    public static UserInfo from(User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setRole(user.getRole().name());
        info.setStatus(user.getStatus().name());
        return info;
    }
}
