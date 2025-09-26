package com.rus.rus.controller.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecoveryAttainmentResponseDto {
    private String message;
    private Boolean allCompleted;
    private Boolean streakRestored;
    private Integer newStreak;
}
