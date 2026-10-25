package com.recipe.controller;

import com.recipe.dto.AuthResponse;
import com.recipe.dto.LoginRequest;
import com.recipe.dto.RegisterRequest;
import com.recipe.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================================
 * [BACKEND] CONTROLLER XÁC THỰC NGƯỜI DÙNG (Authentication REST API)
 * =========================================================================================
 * - Base URL: http://localhost:8080/api/auth
 * - Cung cấp 2 API xử lý:
 *   1. POST /api/auth/register : Tiếp nhận dữ liệu họ tên, email, password để tạo tài khoản mới.
 *   2. POST /api/auth/login    : Tiếp nhận email và password để kiểm tra và trả về thông tin đăng nhập.
 * =========================================================================================
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * [API ĐĂNG KÝ] POST /api/auth/register
     * - Request Body: JSON gồm { "fullName": "...", "email": "...", "password": "..." }
     * - Logic:
     *   + Nếu thành công: Trả về HTTP 201 CREATED kèm AuthResponse (id, fullName, email).
     *   + Nếu email đã tồn tại: Bắt IllegalArgumentException và trả về HTTP 400 BAD REQUEST.
     *   + Nếu lỗi hệ thống khác: Trả về HTTP 500 INTERNAL SERVER ERROR.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // Lỗi trùng email hoặc dữ liệu không hợp lệ
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            // Lỗi không mong muốn trong quá trình xử lý
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * [API ĐĂNG NHẬP] POST /api/auth/login
     * - Request Body: JSON gồm { "email": "...", "password": "..." }
     * - Logic:
     *   + Nếu đúng email và mật khẩu: Trả về HTTP 200 OK kèm thông tin user.
     *   + Nếu sai thông tin: Bắt IllegalArgumentException và trả về HTTP 401 UNAUTHORIZED.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Sai email hoặc sai mật khẩu
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            // Lỗi kết nối CSDL hoặc lỗi hệ thống
            Map<String, String> error = new HashMap<>();
            error.put("error", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
