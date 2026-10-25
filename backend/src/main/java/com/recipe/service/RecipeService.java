package com.recipe.service;

import com.recipe.dto.RecipeDetailDto;
import com.recipe.dto.RecipeRequestDto;
import com.recipe.dto.RecipeSummaryDto;
import com.recipe.entity.Ingredients;
import com.recipe.entity.Instruction;
import com.recipe.entity.Recipe;
import com.recipe.repository.RecipeRepository;
import com.recipe.repository.UserRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lấy danh sách món ăn:
     * - Khách (userId == null): Chỉ xem 10 món fix cứng trong database (id <= 10)
     * - Đã đăng nhập (userId != null): Xem toàn bộ món (10 món gốc + món của các tài khoản khác + tài khoản hiện tại)
     */
    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> getRecipes(String search, Long userId) {
        Sort sort = Sort.by("id");

        List<Recipe> recipes;
        boolean isGuest = (userId == null || userId <= 0);

        if (isGuest) {
            // Chế độ khách: CHỈ hiển thị 10 món fix cứng ban đầu trong database
            if (search == null || search.isBlank()) {
                recipes = recipeRepository.findByIdLessThanEqual(10L, sort);
            } else {
                recipes = recipeRepository.findByIdLessThanEqualAndTitleContainingIgnoreCase(10L, search.trim(), sort);
            }
        } else {
            // Khi đăng nhập: Xem được toàn bộ món của tất cả người dùng
            if (search == null || search.isBlank()) {
                recipes = recipeRepository.findAll(sort);
            } else {
                recipes = recipeRepository.findByTitleContainingIgnoreCase(search.trim(), sort);
            }
        }

        return recipes.stream()
                .map(RecipeSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> getRecipes(String search) {
        return getRecipes(search, null);
    }

    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipe not found with id " + id));

        return RecipeDetailDto.from(recipe);
    }

    /**
     * POST /api/recipes - Thêm món ăn
     */
    @Transactional
    public RecipeDetailDto createRecipe(RecipeRequestDto request, Long userId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe data cannot be null");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        Recipe recipe = new Recipe();
        if (userId != null && userId > 0) {
            userRepository.findById(userId).ifPresent(recipe::setUser);
        }
        recipe.setTitle(request.title().trim());
        recipe.setDescription(request.description());
        recipe.setImageUrl(request.imageUrl());
        recipe.setPrepTime(request.prepTime() != null ? request.prepTime() : 0);
        recipe.setCookTime(request.cookTime() != null ? request.cookTime() : 0);
        recipe.setServings(request.servings() != null ? request.servings() : 1);
        recipe.setCalories(request.calories() != null ? request.calories() : 0);
        recipe.setProteinG(request.proteinG() != null ? request.proteinG() : BigDecimal.ZERO);
        recipe.setCarbsG(request.carbsG() != null ? request.carbsG() : BigDecimal.ZERO);
        recipe.setFatG(request.fatG() != null ? request.fatG() : BigDecimal.ZERO);

        if (request.ingredients() != null) {
            for (var ingDto : request.ingredients()) {
                if (ingDto != null && ingDto.name() != null && !ingDto.name().isBlank()) {
                    BigDecimal qty = ingDto.quantity() != null ? BigDecimal.valueOf(ingDto.quantity()) : null;
                    Ingredients ing = new Ingredients(ingDto.name().trim(), qty, ingDto.unit());
                    recipe.addIngredient(ing);
                }
            }
        }

        if (request.instruction() != null) {
            int autoStep = 1;
            for (var instDto : request.instruction()) {
                if (instDto != null && instDto.description() != null && !instDto.description().isBlank()) {
                    int stepNum = instDto.stepNumber() != null ? instDto.stepNumber() : autoStep;
                    Instruction inst = new Instruction(stepNum, instDto.description().trim());
                    recipe.addInstruction(inst);
                    autoStep = stepNum + 1;
                }
            }
        }

        Recipe saved = recipeRepository.save(recipe);
        return RecipeDetailDto.from(saved);
    }

    @Transactional
    public RecipeDetailDto createRecipe(RecipeRequestDto request) {
        return createRecipe(request, null);
    }

    /**
     * PUT /api/recipes/{id} - Cập nhật món ăn
     * Chỉ chủ sở hữu (user_id của món khớp với currentUserId) mới có quyền chỉnh sửa.
     */
    @Transactional
    public RecipeDetailDto updateRecipe(Long id, RecipeRequestDto request, Long currentUserId) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe id cannot be null");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe data cannot be null");
        }
        if (request.title() != null && request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be empty");
        }

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipe not found with id " + id));

        // 1. Món mặc định cố định của hệ thống (id <= 10 hoặc user == null) không thể chỉnh sửa
        if (recipe.getUser() == null || (recipe.getId() != null && recipe.getId() <= 10)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Không thể chỉnh sửa các món ăn mặc định của hệ thống."
            );
        }

        // 2. Chỉ tài khoản tạo ra món ăn mới có quyền chỉnh sửa
        if (currentUserId == null || !recipe.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn chỉ có quyền chỉnh sửa món ăn do chính tài khoản của mình đăng lên."
            );
        }

        if (request.title() != null) {
            recipe.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            recipe.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            recipe.setImageUrl(request.imageUrl());
        }
        if (request.prepTime() != null) {
            recipe.setPrepTime(request.prepTime());
        }
        if (request.cookTime() != null) {
            recipe.setCookTime(request.cookTime());
        }
        if (request.servings() != null) {
            recipe.setServings(request.servings());
        }
        if (request.calories() != null) {
            recipe.setCalories(request.calories());
        }
        if (request.proteinG() != null) {
            recipe.setProteinG(request.proteinG());
        }
        if (request.carbsG() != null) {
            recipe.setCarbsG(request.carbsG());
        }
        if (request.fatG() != null) {
            recipe.setFatG(request.fatG());
        }

        if (request.ingredients() != null) {
            recipe.getIngredients().clear();
            for (var ingDto : request.ingredients()) {
                if (ingDto != null && ingDto.name() != null && !ingDto.name().isBlank()) {
                    BigDecimal qty = ingDto.quantity() != null ? BigDecimal.valueOf(ingDto.quantity()) : null;
                    Ingredients ing = new Ingredients(ingDto.name().trim(), qty, ingDto.unit());
                    recipe.addIngredient(ing);
                }
            }
        }

        if (request.instruction() != null) {
            recipe.getInstruction().clear();
            int autoStep = 1;
            for (var instDto : request.instruction()) {
                if (instDto != null && instDto.description() != null && !instDto.description().isBlank()) {
                    int stepNum = instDto.stepNumber() != null ? instDto.stepNumber() : autoStep;
                    Instruction inst = new Instruction(stepNum, instDto.description().trim());
                    recipe.addInstruction(inst);
                    autoStep = stepNum + 1;
                }
            }
        }

        Recipe updated = recipeRepository.save(recipe);
        return RecipeDetailDto.from(updated);
    }

    @Transactional
    public RecipeDetailDto updateRecipe(Long id, RecipeRequestDto request) {
        return updateRecipe(id, request, null);
    }
}
