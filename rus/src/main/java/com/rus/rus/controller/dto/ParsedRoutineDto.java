package com.rus.rus.controller.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 응답에서 파싱된 루틴 정보 DTO
 * Service 내부에서만 사용 (기존 RecommendedRoutineDto와 구분)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedRoutineDto {

    /**
     * 루틴 카테고리
     * 수면, 운동, 영양소, 햇빛, 사회적유대감
     */
    private String category;

    /**
     * 루틴 이름
     */
    private String name;

    /**
     * 루틴 설명
     */
    private String description;

    /**
     * 우선순위
     * AI가 추천한 순서 (1부터 시작)
     */
    private Integer priority;

    /**
     * 추천 이유
     * 예: "사회적유대감 카테고리가 비어있어서"
     */
    private String reason;

    /**
     * 권장 시간대
     * 예: 아침, 점심, 저녁, 밤
     */
    private String timeOfDay;
}