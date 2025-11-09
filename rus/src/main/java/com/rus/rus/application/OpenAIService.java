// src/main/java/com/rus/rus/application/OpenAIService.java
package com.rus.rus.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto;
import com.rus.rus.controller.dto.res.PersonalRoutineResponseDto;
//import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI Chat Completions + Tool (Function) Calling
 * Mirrors VertexaiService.getChatResponse logic.
 */
@Slf4j
@Service
@ConditionalOnProperty(name="openai.enabled", havingValue="true", matchIfMissing=true)
//@RequiredArgsConstructor
public class OpenAIService {

    private final RoutineService routineService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Autowired
    public OpenAIService(
            RoutineService routineService,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl
    ) {
        this.routineService = routineService;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String model;

    @Value("${openai.model:gpt-4o-mini}")
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Equivalent entrypoint to VertexaiService#getChatResponse
     */
    public String getChatResponse(String uid, List<ChatMessageDto> messages) {

        // 1) Build OpenAI messages from DTO history
        List<Map<String, Object>> oaMessages = new ArrayList<>();
        // Optional: Inject a light system prompt
        oaMessages.add(Map.of(
                "role", "system",
                "content", "You are Sera, a helpful routine coach inside the ThreeFit app. " +
                        "Be warm, concise, and use app terms (Lux, routines, weekly goals)."
        ));

        oaMessages.addAll(messages.stream().map(this::mapToOpenAIMessage).collect(Collectors.toList()));

        // 2) Define tools (function signatures)
        List<Map<String, Object>> tools = buildToolsSchema();

        // 3) First call: ask model; it may return tool_calls
        JsonNode first = createChatCompletion(oaMessages, tools);

        JsonNode choice0 = first.path("choices").get(0);
        JsonNode assistantMsg = choice0.path("message");
        JsonNode toolCalls = assistantMsg.path("tool_calls");

        // If no tool calls → return content directly
        if (toolCalls.isMissingNode() || !toolCalls.isArray() || toolCalls.size() == 0) {
            String content = assistantMsg.path("content").asText("");
            log.info("OpenAI: normal text response");
            return content;
        }

        log.info("OpenAI: tool calls detected count={}", toolCalls.size());

        // 4) Execute each tool call and collect tool results
        List<Map<String, Object>> toolResultMessages = new ArrayList<>();
        for (JsonNode tc : toolCalls) {
            String toolCallId = tc.path("id").asText();
            String fnName = tc.path("function").path("name").asText();
            String rawArgs = tc.path("function").path("arguments").asText("{}");

            Map<String, Object> toolResult = handleToolCall(uid, fnName, rawArgs);

            // Each tool result is appended as a `tool` role message
            Map<String, Object> toolMsg = new HashMap<>();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", toolCallId);
            toolMsg.put("content", toJson(toolResult));
            toolMsg.put("name", fnName); // optional but nice to have
            toolResultMessages.add(toolMsg);
        }

        // 5) Second call: add assistant tool-call message + tool results → final answer
        List<Map<String, Object>> followup = new ArrayList<>(oaMessages);
        // append the assistant message (with tool_calls) we just got
        followup.add(objectMapper.convertValue(assistantMsg, Map.class));
        // append each tool result
        followup.addAll(toolResultMessages);

        JsonNode second = createChatCompletion(followup, tools);
        String finalText = second.path("choices").get(0).path("message").path("content").asText("");

        return finalText == null ? "" : finalText.trim();
    }

    // ============ Helpers ============

    private Map<String, Object> mapToOpenAIMessage(ChatMessageDto dto) {
        String role = dto.getRole().equalsIgnoreCase("MODEL") ? "assistant" : "user";
        return Map.of("role", role, "content", dto.getText());
    }

    private String toJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "{\"status\":\"ERROR\",\"message\":\"serialization failed\"}"; }
    }

    private Map<String, Object> createSuccess(String messageOrJson) {
        // If it looks like JSON, pass through; otherwise wrap as string
        try {
            JsonNode node = objectMapper.readTree(messageOrJson);
            return Map.of("status", "SUCCESS", "result", node);
        } catch (Exception ignore) {
            return Map.of("status", "SUCCESS", "result", messageOrJson);
        }
    }

    private Map<String, Object> createError(String msg, String code) {
        return Map.of("status", code == null ? "ERROR" : code, "message", msg);
    }

    private Map<String, Object> handleToolCall(String uid, String fnName, String rawArgs) {
        try {
            JsonNode args = objectMapper.readTree(rawArgs);

            switch (fnName) {
                case "addCustomRoutine": {
                    String content = args.path("content").asText(null);
                    int categoryId = args.path("categoryId").asInt(0);
                    if (content == null || categoryId == 0) {
                        return createError("AI가 잘못된 함수 인자(content/categoryId)를 전달했습니다.", "ERROR");
                    }

                    RoutineAddCustomRequestDto dto = new RoutineAddCustomRequestDto();
                    dto.setContent(content);
                    dto.setCategoryId(categoryId);

                    routineService.addCustomRoutineToUser(uid, dto);

                    return createSuccess("루틴 '" + content + "'이(가) 성공적으로 추가되었습니다.");
                }
                case "getPersonalRoutines": {
                    PersonalRoutineResponseDto routines = routineService.getPersonalRoutines(uid);
                    String asJson = objectMapper.writeValueAsString(routines);
                    return createSuccess(asJson);
                }
                case "checkRoutineAsDone": {
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0) return createError("AI가 잘못된 함수 인자(routineId)를 전달했습니다.", "ERROR");
                    routineService.checkRoutineAttainment(uid, routineId);
                    return createSuccess("루틴(ID: " + routineId + ")이(가) 완료 처리되었습니다.");
                }
                case "uncheckRoutine": {
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0) return createError("AI가 잘못된 함수 인자(routineId)를 전달했습니다.", "ERROR");
                    routineService.uncheckRoutineAttainment(uid, routineId);
                    return createSuccess("루틴(ID: " + routineId + ")의 완료 체크가 해제되었습니다.");
                }
                default:
                    return createError("알 수 없는 함수 '" + fnName + "' 호출됨", "UNKNOWN_FUNCTION");
            }
        } catch (Exception e) {
            log.error("Tool handler error (fn={}): {}", fnName, e.getMessage());
            return createError("도구 실행 중 오류: " + e.getMessage(), "ERROR");
        }
    }

    private JsonNode createChatCompletion(List<Map<String, Object>> messages,
                                          List<Map<String, Object>> tools) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        body.put("temperature", 0.3);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    private List<Map<String, Object>> buildToolsSchema() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "addCustomRoutine",
                        "description", "사용자에게 커스텀 루틴을 추가한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "content", Map.of("type", "string", "description", "루틴 내용"),
                                        "categoryId", Map.of("type", "integer", "description", "카테고리 ID")
                                ),
                                "required", List.of("content", "categoryId")
                        )
                )
        ));

        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "getPersonalRoutines",
                        "description", "사용자의 개인 루틴 목록을 조회한다.",
                        "parameters", Map.of("type", "object", "properties", Map.of())
                )
        ));

        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "checkRoutineAsDone",
                        "description", "특정 루틴을 완료 처리한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "routineId", Map.of("type", "integer", "description", "루틴 ID")
                                ),
                                "required", List.of("routineId")
                        )
                )
        ));

        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "uncheckRoutine",
                        "description", "특정 루틴의 완료 체크를 해제한다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "routineId", Map.of("type", "integer", "description", "루틴 ID")
                                ),
                                "required", List.of("routineId")
                        )
                )
        ));

        return tools;
    }
}
