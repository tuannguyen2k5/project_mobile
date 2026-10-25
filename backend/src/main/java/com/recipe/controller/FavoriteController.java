package com.recipe.controller;

import com.recipe.dto.FavoriteRecipeDto;
import com.recipe.entity.Favorite;
import com.recipe.entity.Recipe;
import com.recipe.entity.User;
import com.recipe.repository.FavoriteRepository;
import com.recipe.repository.RecipeRepository;
import com.recipe.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public FavoriteController(FavoriteRepository favoriteRepository,
                              UserRepository userRepository,
                              RecipeRepository recipeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    /**
     * API 7: Lấy danh sách món ăn yêu thích của người dùng
     * GET /api/favorites?userId=1
     */
    @GetMapping
    public ResponseEntity<List<FavoriteRecipeDto>> getUserFavorites(@RequestParam Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        List<FavoriteRecipeDto> response = favorites.stream()
                .map(fav -> FavoriteRecipeDto.from(fav.getRecipe()))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * API 9: Thêm món vào yêu thích
     * POST /api/favorites/{recipeId}?userId=1
     */
    @PostMapping("/{recipeId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long recipeId,
                                         @RequestParam Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found with id: " + userId));
        }

        Optional<Recipe> recipe = recipeRepository.findById(recipeId);
        if (recipe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Recipe not found with id: " + recipeId));
        }

        // Đã yêu thích rồi thì không thêm trùng
        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            return ResponseEntity.ok(Map.of("message", "Món ăn đã có trong danh sách yêu thích"));
        }

        favoriteRepository.save(new Favorite(user.get(), recipe.get()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đã thêm vào yêu thích"));
    }

    /**
     * API 10: Xóa món khỏi yêu thích
     * DELETE /api/favorites/{recipeId}?userId=1
     */
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long recipeId,
                                            @RequestParam Long userId) {
        Optional<Favorite> favorite = favoriteRepository.findByUserIdAndRecipeId(userId, recipeId);
        if (favorite.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Món ăn này chưa có trong danh sách yêu thích"));
        }

        favoriteRepository.delete(favorite.get());

        return ResponseEntity.ok(Map.of("message", "Đã xóa khỏi yêu thích"));
    }
}