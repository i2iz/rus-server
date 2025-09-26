package com.rus.rus.controller.dto.res;

import com.rus.rus.controller.dto.RecoveryStatusDto;
import com.rus.rus.controller.dto.TodayProgressDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class StreakResponseDto {
    private Integer currentStreak;
    private LocalDate lastSuccessDate;
    private RecoveryStatusDto recoveryStatus;
    private TodayProgressDto todayProgress;

}
