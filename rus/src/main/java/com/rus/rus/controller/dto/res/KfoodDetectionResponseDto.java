package com.rus.rus.controller.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class KfoodDetectionResponseDto {
  /** 바운딩 박스가 그려진 이미지의 Base64 인코딩 문자열 */
  @JsonProperty("image_with_boxes_base64")
  private String imageWithBoxesBase64;

  /** 감지된 음식 라벨 목록 */
  @JsonProperty("detected_food_labels")
  private List<String> detectedFoodLabels;

  /** 각 감지된 객체의 상세 분석 결과 (라벨, 신뢰도, 바운딩 박스 좌표 등) */
  @JsonProperty("analysis_results")
  private List<Map<String, Object>> analysisResults;

  /** 전체 감지된 객체들의 평균 신뢰도 */
  @JsonProperty("overall_average_confidence")
  private double overallAverageConfidence;
}
