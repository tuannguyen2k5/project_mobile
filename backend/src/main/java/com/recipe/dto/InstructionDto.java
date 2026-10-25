package com.recipe.dto;

import com.recipe.entity.Instruction;

public record InstructionDto(
        Integer stepNumber,
        String description
) {
    public static InstructionDto from(Instruction i) {
        if (i == null) return null;
        String desc = (i.getDescription() != null && !i.getDescription().trim().equalsIgnoreCase("null"))
                ? i.getDescription().trim()
                : "";
        return new InstructionDto(
                i.getStepNumber(),
                desc
        );
    }
}
