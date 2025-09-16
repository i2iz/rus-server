package com.rus.rus.controller.dto.res;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

import com.rus.rus.controller.dto.RoutineStatisticsDto;

@Getter
@Builder
public class StatisticsResponseDto {
    private LocalDate startDay;
    private LocalDate endDay;
    private List<RoutineStatisticsDto> routines;
}
