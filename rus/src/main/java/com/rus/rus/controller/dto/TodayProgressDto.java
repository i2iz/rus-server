package com.rus.rus.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TodayProgressDto {
    private Integer totalRoutines;
    private Integer completedRoutines;
    private Double completionRate;
}
