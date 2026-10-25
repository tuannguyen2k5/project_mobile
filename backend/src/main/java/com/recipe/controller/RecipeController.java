package com.recipe.controller;

import com.recipe.dto.RecipeDetailDto;
import com.recipe.dto.RecipeRequestDto;
import com.recipe.dto.RecipeSummaryDto;
import com.recipe.repository.UserRepository;
import com.recipe.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * =========================================================================================
 * [BACKEND] CONTROLLER ĐIỀU HƯỚNG CÔNG THỨC MÓN ĂN (Recipe REST API)
 * =========================================================================================
 * - Base URL: http://localhost:8080/api/recipes
 * - Định nghĩa các endpoint RESTful kết nối với ứng dụng Android:
 *   1. GET  /api/recipes         : Xem danh sách tóm tắt (Khách xem 10 món, User xem tất cả).
 *   2. GET  /api/recipes/{id}    : Xem chi tiết món ăn (Bắt buộc phải đăng nhập/đăng ký).
 *   3. POST /api/recipes         : Thêm món ăn mới kèm nguyên liệu, các bước và dinh dưỡng.
 *   4. PUT  /api/recipes/{id}    : Cập nhật món ăn (Chỉ tài khoản chính chủ tạo ra món mới được sửa).
 * - Câu hỏi bảo vệ:
 *   + "Tại sao có cả @RequestParam và @RequestHeader?": Hỗ trợ cả 2 cách truyền thông tin người dùng
 *     từ Android (qua Query URL ?userId= hoặc Header X-User-Id), tăng độ tương thích.
 * =========================================================================================
 */
@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;

    public RecipeController(RecipeService recipeService, UserRepository userRepository) {
        this.recipeService = recipeService;
        this.userRepository = userRepository;
    }

    /**
     * [API 1] GET /api/recipes
     * Lấy danh sách món ăn hiển thị ở màn hình chính (Trang chủ Android).
     * - Tham số:
     *   + search: Từ khóa tìm kiếm (tùy chọn)
     *   + userId / X-User-Id: ID người dùng hiện tại (nếu là null/0 coi như khách).
     * - Trả về: Danh sách tóm tắt (RecipeSummaryDto) gồm ID, tiêu đề, ảnh, thời gian, calo, tên tác giả.
     */
    @GetMapping
    public List<RecipeSummaryDto> getRecipes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        // Lấy ID người dùng từ Param hoặc Header
        Long currentUserId = userId != null ? userId : headerUserId;
        return recipeService.getRecipes(search, currentUserId);
    }

    /**
     * [API 2] GET /api/recipes/{id}
     * Lấy thông tin chi tiết đầy đủ của một món ăn.
     * - Bắt buộc người dùng phải đăng nhập/đăng ký tài khoản:
     *   + Nếu chưa đăng nhập (currentUserId == null): Trả về HTTP 401 UNAUTHORIZED.
     *   + Nếu ID người dùng không tồn tại trong DB: Trả về HTTP 401 UNAUTHORIZED.
     * - Trả về: RecipeDetailDto (Dinh dưỡng, tác giả, danh sách nguyên liệu, các bước nấu).
     */
    @GetMapping("/{id}")
    public RecipeDetailDto getRecipeById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long currentUserId = userId != null ? userId : headerUserId;
        
        // [CHẶN KHÁCH] Không cho xem chi tiết nếu chưa đăng nhập
        if (currentUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng ký hoặc đăng nhập tài khoản để xem chi tiết công thức và thông tin dinh dưỡng."
            );
        }

        // Kiểm tra tính hợp lệ của tài khoản trong CSDL
        if (!userRepository.existsById(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản không tồn tại hoặc phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại."
            );
        }

        return recipeService.getRecipeById(id);
    }

    /**
     * [API 3] POST /api/recipes
     * Đăng công thức món ăn mới.
     * - Trạng thái HTTP thành công: 201 CREATED.
     * - Body: JSON chứa thông tin món, dinh dưỡng, mảng nguyên liệu và mảng các bước.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDetailDto createRecipe(
            @Valid @RequestBody RecipeRequestDto request,
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long currentUserId = userId != null ? userId : headerUserId;
        if (currentUserId != null && !userRepository.existsById(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ");
        }

        return recipeService.createRecipe(request, currentUserId);
    }

    /**
     * [API 4] PUT /api/recipes/{id}
     * Cập nhật chỉnh sửa công thức món ăn.
     * - Yêu cầu đăng nhập và chỉ cho phép chủ bài viết chỉnh sửa.
     * - Nếu vi phạm (món của hệ thống hoặc sửa món của người khác), RecipeService sẽ ném 403 FORBIDDEN.
     */
    @PutMapping("/{id}")
    public RecipeDetailDto updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeRequestDto request,
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long currentUserId = userId != null ? userId : headerUserId;
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để chỉnh sửa món ăn.");
        }
        if (!userRepository.existsById(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ hoặc không tồn tại.");
        }

        return recipeService.updateRecipe(id, request, currentUserId);
    }
}
