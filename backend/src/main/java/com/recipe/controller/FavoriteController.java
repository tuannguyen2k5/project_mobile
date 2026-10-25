package com.recipe.controller;

import com.recipe.dto.FavoriteRecipeDto;
import com.recipe.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * =========================================================================================
 * [BACKEND] CONTROLLER QUẢN LÝ DANH SÁCH YÊU THÍCH (Favorites REST API)
 * =========================================================================================
 * - Base URL: http://localhost:8080/api/favorites
 * - Kiến trúc phân lớp chuẩn: Controller -> FavoriteService -> Repository
 *   Controller đóng vai trò "Thin Controller": chỉ tiếp nhận HTTP request từ Android Client,
 *   ủy quyền (delegate) xử lý logic cho FavoriteService và trả về HTTP response tương ứng.
 * - Cung cấp các thao tác:
 *   1. GET    /api/favorites?userId={id}            : Lấy danh sách món mà người dùng đã thả tim.
 *   2. POST   /api/favorites/{recipeId}?userId={id} : Thêm món vào danh sách yêu thích.
 *   3. DELETE /api/favorites/{recipeId}?userId={id} : Xóa món khỏi danh sách yêu thích.
 * =========================================================================================
 */
@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    private final FavoriteService favoriteService;

    // Tiêm (Inject) FavoriteService thay vì tiêm trực tiếp các Repository
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * [API 1] GET /api/favorites?userId={id}
     * Lấy danh sách tất cả các món ăn mà người dùng có userId tương ứng đã lưu yêu thích.
     * Trả về danh sách FavoriteRecipeDto hiển thị trên Tab "Yêu thích" của app Android.
     */
    @GetMapping
    public ResponseEntity<List<FavoriteRecipeDto>> getUserFavorites(@RequestParam Long userId) {
        List<FavoriteRecipeDto> response = favoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * [API 2] POST /api/favorites/{recipeId}?userId={id}
     * Thêm một món ăn vào danh sách yêu thích của người dùng.
     * - FavoriteService sẽ kiểm tra tính hợp lệ của User, Recipe và chống trùng lặp.
     * - Trả về HTTP 201 CREATED nếu thêm mới thành công, hoặc HTTP 200 OK nếu món đã có sẵn.
     */
    @PostMapping("/{recipeId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long recipeId,
                                         @RequestParam Long userId) {
        boolean isNewlyAdded = favoriteService.addFavorite(recipeId, userId);

        if (isNewlyAdded) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Đã thêm vào yêu thích"));
        } else {
            return ResponseEntity.ok(Map.of("message", "Món ăn đã có trong danh sách yêu thích"));
        }
    }

    /**
     * [API 3] DELETE /api/favorites/{recipeId}?userId={id}
     * Xóa một món ăn khỏi danh sách yêu thích của người dùng.
     * - FavoriteService sẽ tìm và xóa bản ghi khỏi bảng favorites trong MySQL.
     * - Trả về HTTP 200 OK kèm thông báo thành công.
     */
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long recipeId,
                                            @RequestParam Long userId) {
        favoriteService.removeFavorite(recipeId, userId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa khỏi yêu thích"));
    }
}
