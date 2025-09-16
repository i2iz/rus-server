package com.rus.rus.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.rus.rus.common.ApiException;

@Service
public class SupabaseAuthService {

  private final WebClient webClient;
  private final String supabaseUrl;
  private final String serviceRoleKey;

  public SupabaseAuthService(
    WebClient.Builder webClientBuilder,
    @Value("${supabase.url}") String supabaseUrl,
    @Value("${supabase.service-role-key}") String serviceRoleKey) {
      this.supabaseUrl = supabaseUrl;
      this.serviceRoleKey = serviceRoleKey;
      this.webClient = webClientBuilder.baseUrl(supabaseUrl + "/auth/v1")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
    }


    /**
     * Supabase Authentication에 새로운 사용자를 생성하고,
     * 생성된 사용자의 UID를 반환합니다. (동기 방식)
     * @param email 이메일
     * @param password 비밀번호(평문)
     * @return UID
     */
    public String createUser(String email, String password) {
      String requestBody = String.format("{\"email\": \"%s\", \"password\": \"%s\", \"email_confirm\": true}", email, password);

      try {
        JsonNode responseNode = webClient.post()
                    .uri("/admin/users")
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(); // block()을 호출하여 동기적으로 결과를 기다립니다.

        if (responseNode != null && responseNode.has("id")) {
            return responseNode.get("id").asText();
        } else {
            // 응답이 비어있거나 'id' 필드가 없는 경우
            String responseBody = (responseNode != null) ? responseNode.toString() : "null";
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Supabase 사용자 생성 실패: 응답에 'id' 필드가 없습니다. 응답: " + responseBody);
        }
      } catch (WebClientResponseException e) {
        // WebClientResponseException은 HTTP 응답 상태 코드 정보를 포함합니다.
        HttpStatus statusCode = HttpStatus.valueOf(e.getStatusCode().value());
        String errorMessageFromSupabase = e.getResponseBodyAsString(); // Supabase API의 에러 메시지

        // Supabase의 특정 오류 코드에 따라 커스텀 예외를 던집니다.
        if (statusCode == HttpStatus.CONFLICT) { // 409 Conflict: 이미 존재하는 이메일
            throw new ApiException(HttpStatus.CONFLICT, "이미 해당 이메일로 등록된 사용자가 존재합니다.");
        } else if (statusCode == HttpStatus.UNPROCESSABLE_ENTITY) { // 이메일 중복이면 409를 뱉을줄 알았는데 supabase api가 422를 뱉는다.
            // Supabase의 에러 메시지에 'email_exists'가 포함되어 있는지 확인하여 명확히 처리
            if (errorMessageFromSupabase.contains("\"error_code\":\"email_exists\"")) {
                throw new ApiException(HttpStatus.CONFLICT, "이미 해당 이메일로 등록된 사용자가 존재합니다.");
            }
            // 다른 422 오류
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Supabase 인증 오류: " + errorMessageFromSupabase);
        } else if (statusCode == HttpStatus.BAD_REQUEST) { // 400 Bad Request: 유효하지 않은 비밀번호 등
            // Supabase API의 상세 에러 메시지를 포함할 수 있습니다.
            // 예: {"code":400,"msg":"AuthApiError: Password should be at least 6 characters"}
            throw new ApiException(HttpStatus.BAD_REQUEST, "Supabase 인증 오류: " + errorMessageFromSupabase);
        } else {
            // 그 외 Supabase API 오류 (5xx 등)
            System.err.println("Supabase API 오류 (" + statusCode.value() + "): " + errorMessageFromSupabase);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Supabase 서비스에 문제가 발생했습니다. (코드: " + statusCode.value() + ")");
        }
      } catch (Exception e) {
        // 네트워크 문제 또는 기타 예상치 못한 오류
        System.err.println("예상치 못한 사용자 생성 오류: " + e.getMessage());
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 생성 중 알 수 없는 오류가 발생했습니다.");
      }
    }
}
