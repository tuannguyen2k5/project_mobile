package com.recipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * =========================================================================================
 * [BACKEND] ĐIỂM KHỞI CHẠY CHÍNH CỦA HỆ THỐNG SPRING BOOT (Application Entry Point)
 * =========================================================================================
 * - @SpringBootApplication: Tự động cấu hình Spring (Auto-Configuration), quét component (Component Scan)
 *   và kích hoạt cấu hình thuộc tính.
 * - Lệnh chạy: Chạy class này hoặc thực thi qua dòng lệnh `./mvnw spring-boot:run`.
 * - Cổng mặc định: http://localhost:8080
 * =========================================================================================
 */
@SpringBootApplication
public class RecipeBackendApp {
    public static void main(String[] args) {
        SpringApplication.run(RecipeBackendApp.class, args);
    }
}
