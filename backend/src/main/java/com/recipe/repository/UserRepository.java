package com.recipe.repository;

import com.recipe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * =========================================================================================
 * [BACKEND] REPOSITORY TRUY VẤN TÀI KHOẢN NGƯỜI DÙNG (User Data Access)
 * =========================================================================================
 * - Tương tác với bảng `users` trong MySQL.
 * =========================================================================================
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm kiếm tài khoản theo email (dùng cho luồng đăng nhập)
    Optional<User> findByEmail(String email);
    
    // Kiểm tra nhanh xem email đã được đăng ký chưa (dùng cho luồng đăng ký)
    boolean existsByEmail(String email);
}
