package com.rus.rus.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecoveryMissionDto {
    private Integer rid;
    private CategoryDto category;
    private String content;
    private Boolean completed;
}
