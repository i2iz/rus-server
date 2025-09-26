package com.rus.rus.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class RecoveryStatusDto {
    private Boolean available;
    private LocalDate deadline;
    private Integer originalStreak;
}
