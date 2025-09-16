package com.rus.rus.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryDto {
    private Integer categoryId;
    private String value;
}
