package com.rus.rus.controller.dto.res;

import com.rus.rus.controller.dto.RecommendRoutineItemDto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRoutineResponseDto {
    private List<RecommendRoutineItemDto> routines;
}
