package com.rus.rus.controller.dto.res;

import com.rus.rus.controller.dto.RecoveryMissionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor

public class RecoveryStatusResponseDto {
    private Boolean recoveryAvailable;
    private LocalDate deadline;
    private Integer originalStreak;
    private List<RecoveryMissionDto> missions;
    private Boolean allCompleted;
}
