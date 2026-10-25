package com.recipe.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record RecipeRequestDto(
        @NotBlank(message = "Title is required")
        String title,
        String description,
        String imageUrl,
        Integer prepTime,
        Integer cookTime,
        @JsonAlias("rangeServings")
        Integer servings,
        Integer calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        @Valid List<IngredientDto> ingredients,
        @Valid @JsonAlias("instructions") List<InstructionDto> instruction
) {}
