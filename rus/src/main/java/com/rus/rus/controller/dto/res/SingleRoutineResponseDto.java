package com.rus.rus.controller.dto.res;

import com.rus.rus.controller.dto.PersonalRoutineItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleRoutineResponseDto {
    private PersonalRoutineItemDto routine;
}
