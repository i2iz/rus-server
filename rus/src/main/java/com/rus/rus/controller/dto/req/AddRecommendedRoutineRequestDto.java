package com.rus.rus.controller.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 챗봇 메시지에서 추천된 루틴을 추가하는 요청 DTO
 * (Validation 어노테이션 제거 버전)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRecommendedRoutineRequestDto {

    /**
     * 챗봇 메시지 ID (AI가 추천한 메시지)
     * 필수값
     */
    private Long messageId;

    /**
     * 선택한 루틴 인덱스 목록
     * null이면 AI 메시지에서 파싱된 모든 루틴 추가
     * 예: [0, 2, 4] -> 0번째, 2번째, 4번째 루틴만 추가
     */
    private List<Integer> selectedIndexes;
}