package com.rus.rus.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.rus.rus.config.OpenAIConfig.OpenAIProps;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto;
import com.rus.rus.controller.dto.res.PersonalRoutineResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI Chat Completions + Tool Calling 구현
 * - 1턴: 사용자 메시지 보내기 → tool_calls 감지
 * - 서버 함수 실행 → tool outputs 메시지로 다시 전송
 * - 2턴: 최종 assistant 텍스트 받기
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAIProps props;
    private final OkHttpClient http;
    private final RoutineService routineService;
    private final ObjectMapper mapper;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public String getChatResponse(String uid, List<ChatMessageDto> messages) throws IOException {
        // 1) 시스템 프롬프트
        ObjectNode system = msg("system", SYSTEM_PROMPT);

        // 2) 과거 히스토리(사용중인 DTO 그대로 매핑)
        List<ObjectNode> history = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            ChatMessageDto m = messages.get(i);
            String role = m.getRole().equalsIgnoreCase("MODEL") ? "assistant" : "user";
            history.add(msg(role, m.getText()));
        }

        // 3) 1턴 요청 (tool 선언 포함)
        ArrayNode reqMessages = mapper.createArrayNode();
        reqMessages.add(system);
        history.forEach(reqMessages::add);

        ObjectNode body1 = mapper.createObjectNode();
        body1.put("model", props.getModelName());
        body1.set("messages", reqMessages);
        body1.set("tools", buildToolsSchema());
        body1.put("tool_choice", "auto");
        body1.put("temperature", 0.7);

        JsonNode resp1 = callOpenAI(body1);
        JsonNode choice1 = resp1.path("choices").get(0);
        JsonNode msg1 = choice1.path("message");

        // tool_calls 유무 확인
        JsonNode toolCalls = msg1.path("tool_calls");
        boolean hasTools = toolCalls.isArray() && toolCalls.size() > 0;

        if (!hasTools) {
            // 툴 호출 없이 바로 답변
            String content = msg1.path("content").asText("");
            log.info("OpenAI 일반 응답 반환");
            return content;
        }

        // 4) tool_calls 실행 → 실제 서비스 호출 → tool 결과 메시지 준비
        List<ObjectNode> toolResultMessages = new ArrayList<>();
        for (JsonNode tc : toolCalls) {
            String toolCallId = tc.path("id").asText();
            String functionName = tc.path("function").path("name").asText();
            String arguments = tc.path("function").path("arguments").asText("{}");

            log.info("Tool Call 감지: {} args={}", functionName, arguments);

            ObjectNode toolOutput = executeTool(uid, functionName, arguments);
            // role=tool, tool_call_id 동일하게 세팅
            ObjectNode toolMsg = mapper.createObjectNode();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", toolCallId);
            toolMsg.set("content", toolOutput);
            toolResultMessages.add(toolMsg);
        }

        // 5) 2턴 요청: 직전 assistant 메시지 + tool 결과 메시지들 이어붙여서 재요청
        ArrayNode reqMessages2 = mapper.createArrayNode();
        reqMessages2.add(system);
        history.forEach(reqMessages2::add);
        reqMessages2.add((ObjectNode) msg1); // assistant(도구 호출 포함된) 메시지
        toolResultMessages.forEach(reqMessages2::add);

        ObjectNode body2 = mapper.createObjectNode();
        body2.put("model", props.getModelName());
        body2.set("messages", reqMessages2);
        body2.put("temperature", 0.7);

        JsonNode resp2 = callOpenAI(body2);
        JsonNode choice2 = resp2.path("choices").get(0);
        String finalText = choice2.path("message").path("content").asText("");

        log.info("OpenAI 최종 응답 반환");
        return finalText;
    }

    // --------- 내부 구현 ---------

    private ObjectNode executeTool(String uid, String name, String argJson) {
        ObjectNode result = mapper.createObjectNode();
        try {
            switch (name) {
                case "addCustomRoutine": {
                    JsonNode args = safeParse(argJson);
                    String content = args.path("content").asText(null);
                    int categoryId = args.path("categoryId").asInt(0);

                    if (content == null || categoryId == 0) {
                        return errorJson("AI가 잘못된 함수 인자(content/categoryId)를 전달했습니다.");
                    }

                    RoutineAddCustomRequestDto dto = new RoutineAddCustomRequestDto();
                    dto.setContent(content);
                    dto.setCategoryId(categoryId);

                    routineService.addCustomRoutineToUser(uid, dto);

                    return okJson("루틴 '" + content + "'이(가) 성공적으로 추가되었습니다.");
                }
                case "getPersonalRoutines": {
                    PersonalRoutineResponseDto dto = routineService.getPersonalRoutines(uid);
                    String json = mapper.writeValueAsString(dto);
                    return okJson(json, true);
                }
                case "checkRoutineAsDone": {
                    JsonNode args = safeParse(argJson);
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0)
                        return errorJson("AI가 잘못된 함수 인자(routineId)를 전달했습니다.");
                    routineService.checkRoutineAttainment(uid, routineId);
                    return okJson("루틴(ID: " + routineId + ")이(가) 완료 처리되었습니다.");
                }
                case "uncheckRoutine": {
                    JsonNode args = safeParse(argJson);
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0)
                        return errorJson("AI가 잘못된 함수 인자(routineId)를 전달했습니다.");
                    routineService.uncheckRoutineAttainment(uid, routineId);
                    return okJson("루틴(ID: " + routineId + ")의 완료 체크가 해제되었습니다.");
                }
                // ✅ 새로 추가된 루틴 삭제 기능
                case "deleteRoutine": {
                    JsonNode args = safeParse(argJson);
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0)
                        return errorJson("AI가 잘못된 함수 인자(routineId)를 전달했습니다.");

                    routineService.deleteRoutine(routineId, uid);
                    return okJson("루틴(ID: " + routineId + ")이(가) 삭제되었습니다.");
                }

                default:
                    return errorJson("알 수 없는 함수 호출: " + name);
            }
        } catch (Exception e) {
            log.error("도구 실행 오류: {}", e.getMessage());
            return errorJson("서버 실행 중 오류: " + e.getMessage());
        }
    }

    private JsonNode callOpenAI(ObjectNode body) throws IOException {
        String json = body.toString();

        // OkHttp 3.x와 4.x 모두와 호환되는 방식
        MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response res = http.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                String msg = res.body() != null ? res.body().string() : ("HTTP " + res.code());
                log.error("OpenAI API 오류: {}", msg);
                throw new IOException("OpenAI API error: " + msg);
            }
            String raw = res.body().string();
            return mapper.readTree(raw);
        }
    }

    private static ObjectNode msg(String role, String content) {
        ObjectMapper m = new ObjectMapper();
        ObjectNode n = m.createObjectNode();
        n.put("role", role);
        n.put("content", content);
        return n;
    }

    private ArrayNode buildToolsSchema() {
        ArrayNode tools = mapper.createArrayNode();

        tools.add(toolFn(
                "addCustomRoutine",
                "사용자가 직접 입력했거나 AI가 제안한 텍스트와 카테고리로 개인 루틴을 1개 추가",
                obj(schemaStr("content", "string", "추가할 루틴 텍스트"),
                        schemaNum("categoryId", "루틴 카테고리의 숫자 ID")),
                arr("content", "categoryId")));

        tools.add(toolFn(
                "getPersonalRoutines",
                "사용자의 개인 루틴 목록 및 오늘 달성 여부 조회",
                obj(), null));

        tools.add(toolFn(
                "checkRoutineAsDone",
                "특정 개인 루틴을 오늘 완료로 체크",
                obj(schemaNum("routineId", "달성 체크할 루틴의 고유 ID")),
                arr("routineId")));

        tools.add(toolFn(
                "uncheckRoutine",
                "특정 개인 루틴의 오늘 달성 체크를 해제",
                obj(schemaNum("routineId", "체크 해제할 루틴의 고유 ID")),
                arr("routineId")));

        // ✅ 추가: 루틴 삭제 툴
        tools.add(toolFn(
                "deleteRoutine",
                "특정 개인 루틴을 삭제",
                obj(schemaNum("routineId", "삭제할 루틴의 고유 ID")),
                arr("routineId")));

        return tools;
    }

    // ---- JSON 빌더 유틸 ----
    private ObjectNode toolFn(String name, String desc, ObjectNode parameters, ArrayNode required) {
        ObjectNode fn = mapper.createObjectNode();
        fn.put("type", "function");
        ObjectNode f = mapper.createObjectNode();
        f.put("name", name);
        f.put("description", desc);
        f.set("parameters", parameters);
        if (required != null) {
            f.with("parameters").set("required", required);
        }
        fn.set("function", f);
        return fn;
    }

    private ObjectNode obj(ObjectNode... props) {
        ObjectNode o = mapper.createObjectNode();
        o.put("type", "object");
        ObjectNode properties = mapper.createObjectNode();
        for (ObjectNode p : props) {
            // key는 p에만 들어있으니 fieldName으로 추출
            Iterator<String> it = p.fieldNames();
            String key = it.next();
            properties.set(key, p.get(key));
        }
        o.set("properties", properties);
        return o;
    }

    private ObjectNode schemaStr(String name, String type, String desc) {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode spec = mapper.createObjectNode();
        spec.put("type", type);
        spec.put("description", desc);
        node.set(name, spec);
        return node;
    }

    private ObjectNode schemaNum(String name, String desc) {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode spec = mapper.createObjectNode();
        spec.put("type", "number");
        spec.put("description", desc);
        node.set(name, spec);
        return node;
    }

    private ArrayNode arr(String... keys) {
        ArrayNode a = mapper.createArrayNode();
        for (String k : keys)
            a.add(k);
        return a;
    }

    private ObjectNode okJson(String message) {
        return okJson(message, false);
    }

    private ObjectNode okJson(String message, boolean rawStringIsJson) {
        ObjectNode r = mapper.createObjectNode();
        r.put("status", "SUCCESS");
        if (rawStringIsJson) {
            r.put("result", message); // 문자열 형태 JSON을 그대로 넣음(모델이 텍스트로 해석)
        } else {
            r.put("message", message);
        }
        return r;
    }

    private ObjectNode errorJson(String message) {
        ObjectNode r = mapper.createObjectNode();
        r.put("status", "ERROR");
        r.put("message", message);
        return r;
    }

    private JsonNode safeParse(String json) {
        try {
            return mapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    // === 시스템 프롬프트 (Vertex 버전과 의미 동일) ===
    private static final String SYSTEM_PROMPT = "당신은 'RUS' 건강 관리 앱의 친절한 AI 어시스턴트 '세라(Sera)'입니다. " +
            "역할: 사용자가 5가지 카테고리(수면, 운동, 영양소, 햇빛, 사회적유대감) 루틴을 관리하도록 돕기. " +
            "항상 공감/지지/동기부여 톤을 사용하고 일상 대화에도 자연스럽게 응답합니다.\n\n" +
            "루틴 추천 요청 시 목표를 파악해 매일 가능한 구체 루틴 1~3개를 제안하고, 추가 여부를 물어보세요.\n\n" +
            "사용자가 루틴 추가를 원하거나 동의하면 addCustomRoutine 함수를 호출하세요. " +
            "대화 맥락을 바탕으로 적절한 카테고리를 스스로 추론해 숫자 ID(categoryId)를 제공합니다. " +
            "목록 요청 시 getPersonalRoutines, 완료 보고 시 checkRoutineAsDone, 취소 시 uncheckRoutine을 호출하세요.\n\n" +
            "특정 루틴 ID가 불명확하면 먼저 getPersonalRoutines를 호출하여 JSON 목록을 받고, " +
            "해당 content와 일치하는 항목에서 id를 찾아 사용하세요. 임의 추측 금지.\n\n" +
            "절대 사용자에게 카테고리 ↔ 숫자 ID 매핑을 드러내지 마세요. 내부적으로만 사용합니다. " +
            "내부 매핑: '수면':1, '운동':2, '영양소':3, '햇빛':4, '사회적유대감':5.\n\n" +
            "의학적 조언은 제공하지 말고 일반 건강 정보와 루틴 관리에 집중하세요. 항상 한국어로 답하세요.";
}
