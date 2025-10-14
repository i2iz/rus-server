package com.rus.rus.controller.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RoutineUpdateRequestDto {
    @JsonProperty("category_id")
    private Integer categoryId;
    private String content;
}
