package com.recipe.controller;

import com.recipe.dto.UserDto;
import com.recipe.entity.User;
import com.recipe.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long targetId = userId != null ? userId : (headerUserId != null ? headerUserId : 1L);

        User user = userRepository.findById(targetId).orElse(null);
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found with id: " + targetId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        UserDto dto = new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody UserDto dto) {

        Long targetId = userId != null ? userId : (headerUserId != null ? headerUserId : 1L);

        User user = userRepository.findById(targetId).orElse(null);
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found with id: " + targetId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            user.setFullName(dto.getFullName().trim());
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            String newEmail = dto.getEmail().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email đã được sử dụng bởi tài khoản khác.");
                return ResponseEntity.badRequest().body(error);
            }
            user.setEmail(newEmail);
        }

        User saved = userRepository.save(user);
        UserDto updatedDto = new UserDto(saved.getId(), saved.getFullName(), saved.getEmail(), null);
        return ResponseEntity.ok(updatedDto);
    }
}
