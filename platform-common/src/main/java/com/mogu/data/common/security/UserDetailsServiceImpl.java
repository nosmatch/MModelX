package com.mogu.data.common.security;

import com.mogu.data.common.entity.User;
import com.mogu.data.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户详情服务实现
 * 从数据库加载用户信息，用于Spring Security认证
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new UsernameNotFoundException("User is inactive: " + username);
        }

        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new UsernameNotFoundException("User is locked: " + username);
        }

        Collection<GrantedAuthority> authorities = getAuthorities(user);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == User.UserStatus.ACTIVE,
                true, // accountNonExpired
                true, // credentialsNonExpired
                user.getStatus() == User.UserStatus.ACTIVE, // accountNonLocked
                authorities
        );
    }

    /**
     * 获取用户权限列表
     *
     * @param user 用户对象
     * @return 权限列表
     */
    private Collection<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 添加角色权限
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // 根据角色添加额外权限
        switch (user.getRole()) {
            case ADMIN:
                authorities.add(new SimpleGrantedAuthority("PERM_ALL"));
                authorities.add(new SimpleGrantedAuthority("PERM_WRITE"));
                authorities.add(new SimpleGrantedAuthority("PERM_READ"));
                break;
            case MLOPS:
                authorities.add(new SimpleGrantedAuthority("PERM_WRITE"));
                authorities.add(new SimpleGrantedAuthority("PERM_READ"));
                break;
            case ENGINEER:
                authorities.add(new SimpleGrantedAuthority("PERM_READ"));
                break;
        }

        return authorities;
    }
}
