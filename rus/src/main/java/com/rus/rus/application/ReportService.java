package com.rus.rus.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.rus.rus.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ReportService {

  private final WebClient webClient;
  private final String apiKey;

  private static final String GEMINI_MODEL = "gemini-2.0-flash";

  public ReportService(WebClient.Builder webClientBuilder, @Value("${gemini.api.key}") String apiKey) {
    this.apiKey = apiKey;
    this.webClient = webClientBuilder
        .baseUrl("https://generativelanguage.googleapis.com")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("X-goog-api-key", apiKey)
        .build();
  }

  /**
   * 사용자 데이터 요약을 기반으로 AI 피드백 및 추천 루틴을 생성합니다.
   * 
   * @param userDataSummary AI가 분석할 사용자 데이터 요약 텍스트
   * @return AI가 생성한 피드백 텍스트
   */
  public String getFeedback(String userDataSummary) {
    String prompt = """
            당신은 사용자의 건강 루틴 기록을 분석하고 동기를 부여하는 AI 건강 코치 'Sera'입니다. 당신의 역할은 데이터를 기반으로 통찰력 있는 피드백을 제공하고, 사용자가 더 나은 습관을 형성하도록 돕는 것입니다.

            [분석 가이드라인]
            1. 달성 시간 패턴(규칙적/불규칙) 파악.
            2. 달성률이 낮은 루틴 식별.
            3. 먼저 긍정적 강화(칭찬) 후 개선점.
            4. 실행 가능한 1개 구체 제안. 부담 큰 루틴은 더 쉬운 대안 제시.

            [출력 형식]
            - 정확히 2개의 문단으로 작성.
              1문단: 칭찬 + 간단한 패턴/문제 인식.
              2문단: 구체적 실행 제안(시간/빈도 등).
            - 각 문단은 1~2문장, 총 3~4문장.
            - 두 문단 사이에 반드시 하나의 완전히 빈 줄(연속 개행 2개) 삽입.
            - 라벨(분석:, 제안:) 사용 금지.
            - 마크다운, HTML 태그, 리스트, 이모지 과다 사용 금지(한국어 평문).
            - 문장 끝은 마침표로 명확히 종료.
            - 추가 설명이나 서두/맺음말 장황하게 작성하지 말 것.

            [사용자 기록 데이터]
            %s
        """
        .formatted(userDataSummary.isEmpty() ? "최근 달성 기록이 없습니다." : userDataSummary);

    Map<String, Object> requestBody = Map.of(
        "contents", List.of(
            Map.of("parts", List.of(
                Map.of("text", prompt)))));

    try {
      JsonNode response = webClient.post()
          .uri(uriBuilder -> uriBuilder
              .path("/v1beta/models/" + GEMINI_MODEL + ":generateContent")
              .build())
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono(JsonNode.class)
          .block();

      if (response != null && response.has("candidates")) {
        JsonNode textNode = response.get("candidates").get(0).get("content").get("parts").get(0).get("text");
        return textNode.asText();
      } else {
        String errorResponse = (response != null) ? response.toString() : "빈 응답";
        System.err.println("Gemini API 비정상 응답: " + errorResponse);
        return "피드백을 생성하는 데 실패했습니다. 잠시 후 다시 시도해주세요.";
      }

    } catch (WebClientResponseException e) {
      System.err.println("Gemini API HTTP 오류: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
          "AI 피드백 서비스에 문제가 발생했습니다. (HTTP 상태 코드: " + e.getStatusCode().value() + ")");
    } catch (Exception e) {
      System.err.println("Gemini API 호출 중 알 수 없는 오류: " + e.getMessage());
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 피드백 서비스에 접속할 수 없습니다.");
    }
  }
}