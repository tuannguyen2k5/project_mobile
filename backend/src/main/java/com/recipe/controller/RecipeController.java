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
     * GET /api/recipes - Xem danh sách món ăn:
     * - Chế độ khách (không có userId): Chỉ xem 10 món fix cứng trong database (id <= 10)
     * - Đã đăng nhập (có userId): Xem toàn bộ món (10 món gốc + món của các tài khoản khác + tài khoản hiện tại)
     */
    @GetMapping
    public List<RecipeSummaryDto> getRecipes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long currentUserId = userId != null ? userId : headerUserId;
        return recipeService.getRecipes(search, currentUserId);
    }

    /**
     * GET /api/recipes/{id} - Xem chi tiết món ăn (Bắt buộc phải đăng nhập/đăng ký)
     */
    @GetMapping("/{id}")
    public RecipeDetailDto getRecipeById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long currentUserId = userId != null ? userId : headerUserId;
        if (currentUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng ký hoặc đăng nhập tài khoản để xem chi tiết công thức và thông tin dinh dưỡng."
            );
        }

        if (!userRepository.existsById(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản không tồn tại hoặc phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại."
            );
        }

        return recipeService.getRecipeById(id);
    }

    /**
     * POST /api/recipes - Thêm món ăn (Yêu cầu đăng nhập)
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
     * PUT /api/recipes/{id} - Cập nhật món ăn
     * Yêu cầu người dùng đăng nhập và chỉ chủ sở hữu của món mới được phép sửa.
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
