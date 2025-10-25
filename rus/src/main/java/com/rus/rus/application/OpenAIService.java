package com.rus.rus.application;

import com.rus.rus.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 일반 메시지 생성 (비스트리밍)
     */
    public String generateResponse(String userMessage, String context) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", buildMessages(userMessage, context));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    openaiApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스 응답 실패");

        } catch (Exception e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스 응답 실패: " + e.getMessage());
        }
    }

    /**
     * 스트리밍 메시지 생성
     */
    public void streamResponse(String userMessage, String context, SseEmitter emitter,
                               BiConsumer<String, Integer> onComplete) {
        new Thread(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(openaiApiKey);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("messages", buildMessages(userMessage, context));
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 1000);
                requestBody.put("stream", true);

                // 실제 구현에서는 StreamingHttpOutputMessage나 WebClient를 사용해야 합니다
                // 여기서는 간단한 시뮬레이션
                String fullResponse = generateResponse(userMessage, context);
                StringBuilder accumulated = new StringBuilder();

                // 단어 단위로 스트리밍 시뮬레이션
                String[] words = fullResponse.split("(?<=\\s)|(?=\\s)");
                for (String word : words) {
                    emitter.send(SseEmitter.event().data(word));
                    accumulated.append(word);
                    Thread.sleep(50); // 자연스러운 타이핑 효과
                }

                int tokenCount = estimateTokenCount(fullResponse);
                onComplete.accept(fullResponse, tokenCount);
                emitter.complete();

            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        }).start();
    }

    /**
     * 메시지 구성
     */
    private List<Map<String, String>> buildMessages(String userMessage, String context) {
        String systemPrompt = buildSystemPrompt(context);

        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        );
    }

    /**
     * 시스템 프롬프트 생성
     */
    private String buildSystemPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 RUS(Routine, Unity, Sera) 루틴 관리 앱의 AI 어시스턴트 'Sera'입니다.\n\n");
        prompt.append("주요 기능:\n");
        prompt.append("1. 5가지 카테고리의 루틴 추천: 수면, 운동, 영양소, 햇빛, 사회적유대감\n");
        prompt.append("2. 사용자의 연속 달성 일수(Streak) 관리 지원\n");
        prompt.append("3. 리커버리 미션 안내\n");
        prompt.append("4. 개인화된 루틴 조언\n\n");
        prompt.append("응답 스타일:\n");
        prompt.append("- 친근하고 격려하는 톤\n");
        prompt.append("- 구체적이고 실행 가능한 조언\n");
        prompt.append("- 한국어로 응답\n");
        prompt.append("- 필요시 이모지 사용 가능\n\n");

        if (context != null && !context.isEmpty()) {
            prompt.append("추가 컨텍스트:\n");
            prompt.append(context);
            prompt.append("\n\n");
        }

        return prompt.toString();
    }

    /**
     * 토큰 수 추정
     */
    private int estimateTokenCount(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }
}