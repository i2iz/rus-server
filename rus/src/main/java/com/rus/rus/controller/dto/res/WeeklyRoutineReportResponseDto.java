package com.rus.rus.controller.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyRoutineReportResponseDto {

    /**
     * 최근 7일 전체 루틴 달성률 (0~100, 소수점 1자리)
     */
    private double weekAchievementRate;

    /**
     * 최근 7일 동안 완료한 루틴 수
     */
    private int totalCompleted;

    /**
     * 최근 7일 동안 예정되었던 전체 루틴 수 (루틴 개수 * 7일)
     */
    private int totalPlanned;

    /**
     * 카테고리별 달성률 (key 예: "수면", "운동", "영양소", "햇빛", "사회적유대감")
     */
    private Map<String, Double> categoryAchievementRates;

    /**
     * 일자별 통계
     */
    private List<DailyStat> dailyStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private LocalDate date;          // 날짜
        private int completed;           // 완료 루틴 수
        private int total;               // 전체 루틴 수
        private double achievementRate;  // 해당 일자의 달성률 (0~100, 소수점 1자리)
    }
}
