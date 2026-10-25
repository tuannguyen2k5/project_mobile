package com.recipe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * =========================================================================================
 * [BACKEND] CẤU HÌNH BẢO MẬT & MÃ HÓA (Spring Security 6 / Spring Boot 3)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Cung cấp Bean mã hóa mật khẩu (BCryptPasswordEncoder).
 *   2. Thiết lập quy tắc phân quyền và bảo vệ các endpoint REST API.
 * - Khi thầy cô hỏi:
 *   + "Mật khẩu lưu vào DB có bị lộ không? Dùng thuật toán gì?": Trả lời dùng BCrypt với độ muối (salt) tự sinh.
 *   + "Tại sao các API lại permitAll?": Vì ứng dụng quản lý phiên thông qua userId / header X-User-Id
 *     và kiểm tra quyền sở hữu (owner) trực tiếp trong tầng Service / Controller thay vì dùng Session Cookie.
 * =========================================================================================
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean mã hóa mật khẩu người dùng trước khi lưu xuống MySQL.
     * Thuật toán BCrypt tự động tạo salt và băm mật khẩu 1 chiều, không thể dịch ngược.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Chuỗi bộ lọc bảo mật HTTP (SecurityFilterChain):
     * - Tắt CSRF (Cross-Site Request Forgery) vì ứng dụng mobile kết nối qua REST API không dùng cookie session trình duyệt.
     * - Cho phép truy cập công khai vào các đầu API:
     *   + /api/auth/**    : Đăng ký, đăng nhập.
     *   + /api/recipes/** : Xem danh sách món, xem chi tiết (kiểm tra userId thủ công trong controller).
     *   + /api/users/**   : Thông tin người dùng.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF vì mobile client dùng REST stateless
            .csrf(AbstractHttpConfigurer::disable)
            // Cấu hình quyền truy cập theo từng đường dẫn endpoint
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/recipes/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
