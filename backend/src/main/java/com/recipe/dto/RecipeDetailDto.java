package com.recipe.dto;

import com.recipe.entity.Recipe;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public record RecipeDetailDto(
        Long id,
        String title,
        String description,
        String imageUrl,
        Integer prepTime,
        Integer cookTime,
        Integer servings,
        Integer calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        Long userId,
        String authorName,
        List<IngredientDto> ingredients,
        List<InstructionDto> instruction
) {
    public static RecipeDetailDto from(Recipe r) {
        if (r == null) return null;

        List<IngredientDto> ingList = r.getIngredients() != null
                ? r.getIngredients().stream().map(IngredientDto::from).toList()
                : Collections.emptyList();

        List<InstructionDto> instList = r.getInstruction() != null
                ? r.getInstruction().stream().map(InstructionDto::from).toList()
                : Collections.emptyList();

        String desc = (r.getDescription() != null && !r.getDescription().trim().equalsIgnoreCase("null"))
                ? r.getDescription().trim()
                : "";
        String img = (r.getImageUrl() != null && !r.getImageUrl().trim().equalsIgnoreCase("null"))
                ? r.getImageUrl().trim()
                : null;
        Long userId = (r.getUser() != null) ? r.getUser().getId() : null;
        String author = (r.getUser() != null) ? r.getUser().getFullName() : "Hệ thống";

        return new RecipeDetailDto(
                r.getId(),
                r.getTitle(),
                desc,
                img,
                r.getPrepTime(),
                r.getCookTime(),
                r.getServings(),
                r.getCalories(),
                r.getProteinG(),
                r.getCarbsG(),
                r.getFatG(),
                userId,
                author,
                ingList,
                instList
        );
    }
}
