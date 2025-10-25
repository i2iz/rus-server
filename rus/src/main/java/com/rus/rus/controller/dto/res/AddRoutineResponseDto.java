package com.rus.rus.controller.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 루틴 추가 결과 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRoutineResponseDto {

    /**
     * 추가 성공 여부
     */
    private boolean success;

    /**
     * 추가된 루틴 ID (성공 시)
     */
    private Long routineId;

    /**
     * 루틴 이름
     */
    private String routineName;

    /**
     * 루틴 카테고리
     */
    private String category;

    /**
     * 루틴 설명
     */
    private String description;

    /**
     * 결과 메시지
     * 예: "루틴이 성공적으로 추가되었습니다", "이미 추가된 루틴입니다"
     */
    private String message;

    /**
     * 생성 시간 (성공 시)
     */
    private LocalDateTime createdAt;
}