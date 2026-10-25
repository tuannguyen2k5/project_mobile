package com.recipe.controller;

import com.recipe.dto.UserDto;
import com.recipe.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * =========================================================================================
 * [BACKEND] CONTROLLER QUẢN LÝ THÔNG TIN NGƯỜI DÙNG (User Profile REST API)
 * =========================================================================================
 * - Base URL: http://localhost:8080/api/users
 * - Kiến trúc phân lớp chuẩn: Controller -> UserService -> UserRepository
 * - Cung cấp 2 endpoint:
 *   1. GET /api/users/me : Lấy thông tin cá nhân (họ tên, email, ngày tạo tài khoản).
 *   2. PUT /api/users/me : Cập nhật hồ sơ người dùng (đổi họ tên, đổi email kèm kiểm tra trùng lặp).
 * =========================================================================================
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    // Tiêm (Inject) UserService thay vì tiêm trực tiếp UserRepository
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * [API 1] GET /api/users/me
     * Lấy thông tin tài khoản hiện tại để hiển thị trên phần Header / Hồ sơ cá nhân của app Android.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long targetId = userId != null ? userId : (headerUserId != null ? headerUserId : 1L);
        UserDto dto = userService.getUserProfile(targetId);
        return ResponseEntity.ok(dto);
    }

    /**
     * [API 2] PUT /api/users/me
     * Cập nhật thông tin cá nhân (Họ và tên, Email).
     * - UserService sẽ kiểm tra tài khoản và kiểm tra trùng lặp email nếu đổi email.
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody UserDto dto) {

        Long targetId = userId != null ? userId : (headerUserId != null ? headerUserId : 1L);
        UserDto updatedDto = userService.updateUserProfile(targetId, dto);
        return ResponseEntity.ok(updatedDto);
    }
}
