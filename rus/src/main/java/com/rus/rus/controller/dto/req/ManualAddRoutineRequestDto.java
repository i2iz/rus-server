package com.rus.rus.controller.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 수동으로 루틴을 추가하는 요청 DTO
 * (Validation 어노테이션 제거 버전)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAddRoutineRequestDto {

    /**
     * 루틴 카테고리 (필수)
     * 수면, 운동, 영양소, 햇빛, 사회적유대감 중 하나
     */
    private String category;

    /**
     * 루틴 이름 (필수)
     * 2-200자 사이
     */
    private String name;

    /**
     * 루틴 설명 (선택사항)
     * 최대 1000자
     */
    private String description;

    /**
     * 권장 시간대 (선택사항)
     * 예: 아침, 점심, 저녁, 밤
     */
    private String timeOfDay;
}