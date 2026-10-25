package com.recipe.service;

import com.recipe.dto.AuthResponse;
import com.recipe.dto.LoginRequest;
import com.recipe.dto.RegisterRequest;
import com.recipe.entity.User;
import com.recipe.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================================================
 * [BACKEND] SERVICE XỬ LÝ XÁC THỰC NGƯỜI DÙNG (Authentication Service)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Đăng ký tài khoản mới: Kiểm tra trùng email, mã hóa mật khẩu bằng BCrypt, lưu vào MySQL.
 *   2. Đăng nhập tài khoản: Tìm user theo email, đối chiếu mật khẩu dạng băm (matches).
 * - Câu hỏi bảo vệ thường gặp:
 *   + "Kiểm tra trùng email ở đâu?": Phương thức userRepository.existsByEmail() trong register().
 *   + "Làm sao biết mật khẩu nhập vào đúng khi đã bị băm?": Dùng passwordEncoder.matches(raw, hash).
 *   + "Tại sao có @Transactional?": Đảm bảo tính toàn vẹn dữ liệu (ACID), nếu lỗi thì tự động rollback.
 * =========================================================================================
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Dependency Injection thông qua Constructor (khuyên dùng trong Spring Boot)
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Logic đăng ký tài khoản mới:
     * 1. Chuẩn hóa email (xóa khoảng trắng thừa, đưa về chữ thường).
     * 2. Kiểm tra xem email đã tồn tại trong DB chưa -> Nếu có ném lỗi.
     * 3. Băm mật khẩu người dùng gửi lên.
     * 4. Tạo entity User và lưu xuống bảng `users` trong MySQL.
     * 5. Trả về AuthResponse chứa id, tên, email để client lưu phiên.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Kiểm tra tính duy nhất của email (tránh đăng ký trùng lặp)
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email already exists. Please choose another email.");
        }

        // Băm mật khẩu bằng thuật toán BCrypt trước khi ghi vào database
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        // Khởi tạo đối tượng Entity User với dữ liệu đã chuẩn hóa
        User user = new User(
                request.getFullName().trim(),
                request.getEmail().trim().toLowerCase(),
                hashedPassword
        );

        // Lưu vào MySQL và nhận lại entity đã có ID tự sinh (AUTO_INCREMENT)
        User savedUser = userRepository.save(user);
        
        return new AuthResponse(savedUser.getId(), savedUser.getFullName(), savedUser.getEmail(), "Registration successful");
    }

    /**
     * Logic đăng nhập:
     * 1. Tìm tài khoản trong DB theo email. Nếu không thấy -> Báo lỗi sai tài khoản/mật khẩu.
     * 2. Dùng BCrypt đối chiếu mật khẩu người dùng nhập với chuỗi băm trong DB.
     *    -> Nếu không khớp ném ngoại lệ IllegalArgumentException.
     * 3. Trả về thông tin phiên người dùng (id, tên, email).
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Tìm user theo email, nếu không có ném ngoại lệ
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Đối chiếu mật khẩu nhập vào với mật khẩu đã băm trong database
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Đăng nhập thành công, trả về thông tin user
        return new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), "Login successful");
    }
}
