package com.mogu.data.common.config;

import com.mogu.data.common.security.JwtAuthenticationFilter;
import com.mogu.data.common.security.RestAccessDeniedHandler;
import com.mogu.data.common.security.RestAuthenticationEntryPoint;
import com.mogu.data.common.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置
 * 配置JWT认证、CORS跨域、权限控制等
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    private static final String[] WHITELIST = {
            "/api/auth/**",
            "/api/health",
            "/api/info",
            "/api/docs",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/error"
    };

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF (使用JWT不需要CSRF保护)
                .csrf().disable()

                // 配置CORS
                .cors().configurationSource(corsConfigurationSource())

                .and()

                // 配置会话管理 (无状态)
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()
                .exceptionHandling()
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler)

                .and()

                // 配置授权规则
                .authorizeRequests()
                        // 白名单路径无需认证
                        .antMatchers(WHITELIST).permitAll()
                        // 管理员接口
                        .antMatchers("/api/admin/**").hasRole("ADMIN")
                        // 部署管理：仅 MLOPS / ADMIN
                        .antMatchers("/api/v1/deployment/**").hasAnyRole("MLOPS", "ADMIN")
                        // 训练、推理：ENGINEER / MLOPS / ADMIN
                        .antMatchers("/api/v1/training/**", "/api/v1/serving/**")
                            .hasAnyRole("ENGINEER", "MLOPS", "ADMIN")
                        // 特征、样本模块写权限：ENGINEER / ADMIN
                        .antMatchers(HttpMethod.POST, "/api/v1/features/**", "/api/v1/samples/**")
                            .hasAnyRole("ENGINEER", "ADMIN")
                        .antMatchers(HttpMethod.PUT, "/api/v1/features/**", "/api/v1/samples/**")
                            .hasAnyRole("ENGINEER", "ADMIN")
                        .antMatchers(HttpMethod.DELETE, "/api/v1/features/**", "/api/v1/samples/**")
                            .hasAnyRole("ENGINEER", "ADMIN")
                        // 特征、样本模块读权限：所有已登录角色
                        .antMatchers(HttpMethod.GET, "/api/v1/features/**", "/api/v1/samples/**")
                            .hasAnyRole("ENGINEER", "MLOPS", "ADMIN")
                        // 数据源管理：MLOPS / ADMIN
                        .antMatchers("/api/v1/datasources/**").hasAnyRole("MLOPS", "ADMIN")
                        // 其余 /api/v1/** 默认要求登录
                        .antMatchers("/api/v1/**").authenticated()
                        // 其他请求默认要求登录
                        .anyRequest().authenticated()

                .and()

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 配置CORS跨域
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许的方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许发送凭证
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间 (秒)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 配置认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }

    /**
     * 配置认证管理器
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * 配置密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
