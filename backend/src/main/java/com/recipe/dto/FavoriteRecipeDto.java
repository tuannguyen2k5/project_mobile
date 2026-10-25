package com.recipe.dto;

import com.recipe.entity.Recipe;
import java.math.BigDecimal;

public record FavoriteRecipeDto(
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
        boolean favorite
) {
    public static FavoriteRecipeDto from(Recipe r) {
        if (r == null) return null;
        return new FavoriteRecipeDto(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getImageUrl(),
                r.getPrepTime(),
                r.getCookTime(),
                r.getServings(),
                r.getCalories(),
                r.getProteinG(),
                r.getCarbsG(),
                r.getFatG(),
                true // Luôn là true vì nằm trong danh sách yêu thích
        );
    }
}