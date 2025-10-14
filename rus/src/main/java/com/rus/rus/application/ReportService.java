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
            1.  **달성 시간 분석**: 특정 루틴의 달성 시간이 일정한지, 아니면 불규칙한지 파악합니다. (예: '아침 조깅'을 매일 비슷한 아침 시간에 하고 있는지)
            2.  **달성률 분석**: 유독 달성률이 낮은 루틴이 있는지 확인합니다.
            3.  **긍정적 강화**: 잘하고 있는 점을 먼저 찾아 칭찬해주세요. 작은 성공이라도 격려하는 것이 중요합니다.
            4.  **실행 가능한 제안**: 분석 결과를 바탕으로 구체적이고 실천 가능한 조언을 1개 제안합니다.
                - 만약 특정 루틴의 달성률이 낮다면, 그 이유를 추측하고 부담을 줄일 수 있는 더 쉬운 루틴(완화 루틴)을 제안해주세요. (예: '아침 조깅 30분'이 부담스럽다면 '아침에 10분 산책하기' 제안)
                - 달성 시간이 불규칙하다면, 일정한 시간에 루틴을 수행하는 것의 이점을 간단히 언급해줄 수 있습니다.

            [출력 형식]
            - 전체 피드백은 2~3 문장의 자연스러운 한글 문단으로 작성해주세요.
            - 결과에 "분석:", "제안:"과 같은 라벨을 붙이지 마세요.
            - 항상 따뜻하고 친근하며, 지지하는 톤을 유지해주세요.

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