package com.rus.rus.controller.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutinePerformanceItemDto {

  private Integer routineId;
  private String content;
  private String category;
  private Long attainmentCount;
  private Double completionRate;
}
