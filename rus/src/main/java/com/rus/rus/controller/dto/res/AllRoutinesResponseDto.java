package com.rus.rus.controller.dto.res;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

import com.rus.rus.controller.dto.RoutineDto;

@Getter
@Builder
public class AllRoutinesResponseDto {
    private List<RoutineDto> routines;
}
