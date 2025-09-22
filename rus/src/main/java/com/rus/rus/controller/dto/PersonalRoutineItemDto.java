package com.rus.rus.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRoutineItemDto {
    private Integer id;
    private CategoryDto category;
    private String content;
    private String notification; // 현재는 null
    private Boolean complete; // 달성 여부
}
