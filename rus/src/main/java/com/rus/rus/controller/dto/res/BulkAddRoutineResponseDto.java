package com.rus.rus.controller.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 루틴 추가 결과 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddRoutineResponseDto {

    /**
     * 요청된 총 루틴 개수
     */
    private int totalRequested;

    /**
     * 성공한 루틴 개수
     */
    private int successCount;

    /**
     * 실패한 루틴 개수
     */
    private int failureCount;

    /**
     * 각 루틴의 추가 결과 목록
     */
    private List<AddRoutineResponseDto> results;

    /**
     * AddRoutineResponseDto 리스트로부터 BulkAddRoutineResponseDto 생성
     */
    public static BulkAddRoutineResponseDto from(List<AddRoutineResponseDto> results) {
        long successCount = results.stream()
                .filter(AddRoutineResponseDto::isSuccess)
                .count();

        return BulkAddRoutineResponseDto.builder()
                .totalRequested(results.size())
                .successCount((int) successCount)
                .failureCount((int) (results.size() - successCount))
                .results(results)
                .build();
    }
}