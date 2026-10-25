package com.recipe.dto;

import com.recipe.entity.Ingredients;

public record IngredientDto(
        String name,
        Double quantity,
        String unit
) {
    public static IngredientDto from(Ingredients i) {
        if (i == null) return null;
        String u = (i.getUnit() != null && !i.getUnit().trim().isEmpty() && !i.getUnit().trim().equalsIgnoreCase("null"))
                ? i.getUnit().trim()
                : "";
        return new IngredientDto(
                i.getName(),
                i.getQuantity() == null ? null : i.getQuantity().doubleValue(),
                u
        );
    }
}
