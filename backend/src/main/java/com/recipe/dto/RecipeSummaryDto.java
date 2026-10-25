package com.recipe.dto;

import com.recipe.entity.Recipe;

public record RecipeSummaryDto(
        Long id,
        String title,
        String imageUrl,
        Integer prepTime,
        Integer cookTime,
        Integer calories,
        Long userId,
        String authorName
) {
    public static RecipeSummaryDto from(Recipe r) {
        if (r == null) return null;
        Long userId = (r.getUser() != null) ? r.getUser().getId() : null;
        String author = (r.getUser() != null) ? r.getUser().getFullName() : "Hệ thống";
        return new RecipeSummaryDto(
                r.getId(),
                r.getTitle(),
                r.getImageUrl(),
                r.getPrepTime(),
                r.getCookTime(),
                r.getCalories(),
                userId,
                author
        );
    }
}
