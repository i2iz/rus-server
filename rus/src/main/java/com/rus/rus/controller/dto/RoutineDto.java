package com.rus.rus.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoutineDto {
    private Integer rid;
    private CategoryDto category;
    private String content;
}
