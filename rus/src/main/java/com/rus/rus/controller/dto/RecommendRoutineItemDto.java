package com.rus.rus.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRoutineItemDto {
    private Integer id;
    private CategoryDto category;
    private String content;
    private Boolean complete;
}
