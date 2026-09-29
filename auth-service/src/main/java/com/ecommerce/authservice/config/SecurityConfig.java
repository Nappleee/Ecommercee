package com.ecommerce.authservice.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String[] PUBLIC_ENDPOINTS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**",
            "/actuator/**",
            "/api/v1/auth/signup",
            "/api/v1/auth/signin",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    };

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                // Tắt CSRF vì project sử dụng JWT (Stateless), không dùng Session + Cookie
                .csrf(AbstractHttpConfigurer::disable)

                // Không tạo và không lưu Session trên server.
                // Mỗi request phải tự mang JWT để xác thực.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Cấu hình quyền truy cập cho các endpoint
                .authorizeHttpRequests(auth -> auth
                        // Các API public (login, signup, swagger...)
                        // được truy cập mà không cần JWT
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Mọi endpoint còn lại đều yêu cầu người dùng đã xác thực
                        .anyRequest().authenticated()
                )

                // Cấu hình Auth Service hoạt động như một OAuth2 Resource Server
                // -> Tự động đọc Bearer Token trong Authorization Header
                // -> Kiểm tra chữ ký JWT
                // -> Kiểm tra thời hạn (exp)
                // -> Kiểm tra issuer
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
                return http.build();
    }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
