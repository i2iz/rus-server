package com.rus.rus.controller.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 루틴을 한번에 추가하는 요청 DTO
 * (Validation 어노테이션 제거 버전)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddRoutineRequestDto {

    /**
     * 추가할 루틴 목록 (필수)
     * 최소 1개 이상
     */
    private List<ManualAddRoutineRequestDto> routines;
}