package com.rus.rus.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.rus.rus.config.OpenAIConfig.OpenAIProps;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto;
import com.rus.rus.controller.dto.res.PersonalRoutineResponseDto;
import com.rus.rus.controller.dto.res.WeeklyRoutineReportResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * OpenAI Chat Completions + Tool Calling (2턴) 구현
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

        // 2) 과거 히스토리(사용중인 DTO 그대로 매핑: MODEL → assistant, USER → user)
        ArrayNode history = mapper.createArrayNode();
        for (ChatMessageDto m : messages) {
            String role = m.getRole().equalsIgnoreCase("MODEL") ? "assistant" : "user";
            history.add(msg(role, m.getText()));
        }

        // 3) 1턴 요청 (tool 선언 포함)
        ArrayNode reqMessages = mapper.createArrayNode();
        reqMessages.add(system);
        for (JsonNode h : history) reqMessages.add(h);

        ObjectNode body1 = mapper.createObjectNode();
        body1.put("model", props.getModelName());
        body1.set("messages", reqMessages);

        // 로그
        log.info("\n================= 🟢 REQ1 (첫 번째 요청) =================");
        log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body1));
        log.info("========================================================\n");

        body1.set("tools", buildToolsSchema());
        body1.put("tool_choice", "auto");
        body1.put("temperature", 0.7);

        if (log.isDebugEnabled()) {
            log.debug("OpenAI REQ1:\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body1));
        }

        JsonNode resp1 = callOpenAI(body1);
        JsonNode choice1 = resp1.path("choices").get(0);
        JsonNode msg1 = choice1.path("message");

        // 1턴 결과에서 tool_calls 확인
        JsonNode toolCalls = msg1.path("tool_calls");
        boolean hasTools = toolCalls.isArray() && toolCalls.size() > 0;

        if (!hasTools) {
            // 툴 호출 없이 바로 답변
            String content = msg1.path("content").asText("");
            log.info("OpenAI 일반 응답 반환 (no tools)");
            return content;
        }

        // 4) tool_calls 실행 → 실제 서비스 호출 → tool 결과 메시지 준비 (content는 반드시 '문자열')
        List<ObjectNode> toolResultMessages = new ArrayList<>();
        for (JsonNode tc : toolCalls) {
            String toolCallId = tc.path("id").asText();
            String functionName = tc.path("function").path("name").asText();
            String arguments = tc.path("function").path("arguments").asText("{}");

            log.info("Tool Call 감지: {} args={}", functionName, arguments);

            ObjectNode toolOutput = executeTool(uid, functionName, arguments);
            toolResultMessages.add(buildToolMessage(toolCallId, toolOutput)); // content = String
        }

        // 5) 2턴 요청: 직전 assistant 메시지 + tool 결과 메시지들 이어붙여서 재요청
        ArrayNode reqMessages2 = mapper.createArrayNode();
        reqMessages2.add(system);
        for (JsonNode h : history) reqMessages2.add(h);
        reqMessages2.add((ObjectNode) msg1);                 // assistant(with tool_calls)
        toolResultMessages.forEach(reqMessages2::add);       // role=tool (content=String)

        // ▶▶ 중요: OpenAI 규격으로 sanitize → validate 후 호출
        sanitizeMessagesForChat(reqMessages2);
        validateMessages(reqMessages2);

        // 로그
        log.info("\n================= 🟣 REQ2 (두 번째 요청) =================");
        log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqMessages2));
        log.info("========================================================\n");

        ObjectNode body2 = mapper.createObjectNode();
        body2.put("model", props.getModelName());
        body2.set("messages", reqMessages2);
        body2.put("temperature", 0.7);

        if (log.isDebugEnabled()) {
            log.debug("OpenAI REQ2:\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body2));
        }

        JsonNode resp2 = callOpenAI(body2);
        JsonNode choice2 = resp2.path("choices").get(0);
        String finalText = choice2.path("message").path("content").asText("");

        log.info("OpenAI 최종 응답 반환");
        return finalText;
    }

    // --------- 내부 구현 ---------

    private ObjectNode executeTool(String uid, String name, String argJson) {
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

                    // ✅ result에 실제 JSON 객체 삽입 (문자열 이중 포장 제거)
                    ObjectNode r = mapper.createObjectNode();
                    r.put("status", "SUCCESS");
                    r.set("result", mapper.valueToTree(dto));

                    return r;
                }
                case "checkRoutineAsDone": {
                    JsonNode args = safeParse(argJson);
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0) return errorJson("AI가 잘못된 함수 인자(routineId)를 전달했습니다.");
                    routineService.checkRoutineAttainment(uid, routineId);
                    return okJson("루틴(ID: " + routineId + ")이(가) 완료 처리되었습니다.");
                }
                case "uncheckRoutine": {
                    JsonNode args = safeParse(argJson);
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0) return errorJson("AI가 잘못된 함수 인자(routineId)를 전달했습니다.");
                    routineService.uncheckRoutineAttainment(uid, routineId);
                    return okJson("루틴(ID: " + routineId + ")의 완료 체크가 해제되었습니다.");
                }
                case "deleteRoutine": { // ✅ 삭제 기능
                    JsonNode args = safeParse(argJson);
                    int routineId = args.path("routineId").asInt(0);
                    if (routineId == 0) return errorJson("AI가 잘못된 함수 인자(routineId)를 전달했습니다.");
                    routineService.deleteRoutine(routineId, uid);
                    return okJson("루틴(ID: " + routineId + ")이(가) 삭제되었습니다.");
                }
                case "getWeeklyRoutineReport": {   // ✅ 주간 리포트 조회
                    WeeklyRoutineReportResponseDto dto = routineService.getWeeklyRoutineReport(uid);

                    ObjectNode r = mapper.createObjectNode();
                    r.put("status", "SUCCESS");
                    r.set("result", mapper.valueToTree(dto));

                    return r;
                }

                default:
                    return errorJson("알 수 없는 함수 호출: " + name);
            }
        } catch (Exception e) {
            log.error("도구 실행 오류: {}", e.getMessage(), e);
            return errorJson("서버 실행 중 오류: " + e.getMessage());
        }
    }

    private JsonNode callOpenAI(ObjectNode body) throws IOException {
        String json = body.toString();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response res = http.newCall(request).execute()) {
            String raw = res.body() != null ? res.body().string() : "";
            if (!res.isSuccessful()) {
                log.error("OpenAI API 오류: {}", raw);
                throw new IOException("OpenAI API error: " + raw);
            }
            return mapper.readTree(raw);
        }
    }

    /**
     * 공통 message 빌더 (DI 받은 mapper 사용)
     */
    private ObjectNode msg(String role, String content) {
        ObjectNode n = mapper.createObjectNode();
        n.put("role", role);
        n.put("content", content == null ? "" : content);
        return n;
    }

    /**
     * role=tool 메시지를 항상 문자열 content로 만드는 빌더
     */
    private ObjectNode buildToolMessage(String toolCallId, ObjectNode toolOutput) throws IOException {
        ObjectNode toolMsg = mapper.createObjectNode();
        toolMsg.put("role", "tool");
        toolMsg.put("tool_call_id", toolCallId);
        // ✅ 중요: content는 "반드시" 문자열이어야 함
        toolMsg.put("content", mapper.writeValueAsString(toolOutput));
        return toolMsg;
    }

    /**
     * OpenAI 메시지 스펙 강제 정리 (object content → string 등)
     */
    private void sanitizeMessagesForChat(ArrayNode messages) {
        for (int i = 0; i < messages.size(); i++) {
            JsonNode m = messages.get(i);
            if (!(m instanceof ObjectNode)) continue;
            ObjectNode obj = (ObjectNode) m;

            String role = obj.path("role").asText("");
            JsonNode content = obj.get("content");

            // tool: content must be string
            if ("tool".equals(role)) {
                if (content == null || !content.isTextual()) {
                    obj.put("content", content == null ? "" : content.toString());
                }
                continue;
            }

            // user/assistant/system: string | array | null 만 허용
            if (content != null && content.isObject()) {
                obj.put("content", content.toString());
            }
        }
    }

    /**
     * 개발 중 타입 오류 바로 찾도록 검증 (운영 전환 시 완화 가능)
     */
    private void validateMessages(ArrayNode messages) {
        for (int i = 0; i < messages.size(); i++) {
            JsonNode m = messages.get(i);
            String role = m.path("role").asText();
            JsonNode content = m.get("content");

            if ("tool".equals(role)) {
                if (content == null || !content.isTextual()) {
                    throw new IllegalStateException("messages[" + i + "](role=tool).content must be STRING");
                }
            } else {
                boolean ok = (content == null) || content.isTextual() || content.isArray() || content.isNull();
                if (!ok) {
                    throw new IllegalStateException("messages[" + i + "](role=" + role + ").content must be string/array/null");
                }
            }
        }
    }

    private ArrayNode buildToolsSchema() {
        ArrayNode tools = mapper.createArrayNode();

        tools.add(toolFn(
                "addCustomRoutine",
                "사용자가 직접 입력했거나 AI가 제안한 텍스트와 카테고리로 개인 루틴을 1개 추가",
                obj(
                        schemaStr("content", "string", "추가할 루틴 텍스트"),
                        schemaInt("categoryId", "루틴 카테고리의 숫자 ID")
                ),
                arr("content", "categoryId")
        ));

        tools.add(toolFn(
                "getPersonalRoutines",
                "사용자의 개인 루틴 목록 및 오늘 달성 여부 조회",
                obj(), null
        ));

        tools.add(toolFn(
                "checkRoutineAsDone",
                "특정 개인 루틴을 오늘 완료로 체크",
                obj(schemaInt("routineId", "달성 체크할 루틴의 고유 ID")),
                arr("routineId")
        ));

        tools.add(toolFn(
                "uncheckRoutine",
                "특정 개인 루틴의 오늘 달성 체크를 해제",
                obj(schemaInt("routineId", "체크 해제할 루틴의 고유 ID")),
                arr("routineId")
        ));

        tools.add(toolFn(
                "deleteRoutine",
                "사용자의 개인 루틴 하나를 영구 삭제",
                obj(schemaInt("routineId", "삭제할 루틴의 고유 ID")),
                arr("routineId")
        ));

        tools.add(toolFn(
                "getWeeklyRoutineReport",
                "사용자의 최근 7일 루틴 달성 현황을 요약한 주간 리포트를 조회",
                obj(), null
        ));


        return tools;
    }

    // ---- JSON 스키마 유틸 ----
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

    private ObjectNode schemaInt(String name, String desc) {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode spec = mapper.createObjectNode();
        spec.put("type", "integer");
        spec.put("description", desc);
        node.set(name, spec);
        return node;
    }

    private ArrayNode arr(String... keys) {
        ArrayNode a = mapper.createArrayNode();
        for (String k : keys) a.add(k);
        return a;
    }

    private ObjectNode okJson(String message) {
        ObjectNode r = mapper.createObjectNode();
        r.put("status", "SUCCESS");
        r.put("message", message);
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

    // === 시스템 프롬프트 ===
    private static final String SYSTEM_PROMPT =
            "당신은 'RUS' 건강 관리 앱의 친절한 AI 어시스턴트 '세라(Sera)'입니다. " +
                    "역할: 사용자가 5가지 카테고리(수면, 운동, 영양소, 햇빛, 사회적유대감) 루틴을 관리하도록 돕기. " +
                    "항상 공감/지지/동기부여 톤을 사용하고 일상 대화에도 자연스럽게 응답합니다.\n\n" +
                    "루틴 추천 요청 시 목표를 파악해 매일 가능한 구체 루틴 1~3개를 제안하고, 추가 여부를 물어보세요.\n\n" +
                    "사용자가 루틴 추가를 원하거나 동의하면 addCustomRoutine 함수를 호출하세요. " +
                    "대화 맥락을 바탕으로 적절한 카테고리를 스스로 추론해 숫자 ID(categoryId)를 제공합니다. " +
                    "목록 요청 시 getPersonalRoutines, 완료 보고 시 checkRoutineAsDone, 취소 시 uncheckRoutine을 호출하세요.\n\n" +
                    "특정 루틴 ID가 불명확하면 먼저 getPersonalRoutines를 호출하여 JSON 목록을 받고, " +
                    "해당 content와 일치하는 항목에서 id를 찾아 사용하세요. 임의 추측 금지.\n\n" +
                    "사용자가 '요즘 루틴 잘 수행하고 있는지', '최근 일주일 루틴 성과', '주간 리포트', '달성률' 등을 물어보면 " +
                    "반드시 getWeeklyRoutineReport 함수를 호출하여 최근 7일 통계를 조회한 뒤, " +
                    "전체 달성률, 카테고리별 강·약점, 요일별 패턴 등을 간단히 요약하고 따뜻한 피드백과 한두 가지 개선 팁을 제안하세요.\n\n" +
                    "절대 사용자에게 카테고리 ↔ 숫자 ID 매핑을 드러내지 마세요. 내부적으로만 사용합니다. " +
                    "내부 매핑: '수면':1, '운동':2, '영양소':3, '햇빛':4, '사회적유대감':5.\n\n" +
                    "삭제 요청 시 deleteRoutine을 호출하세요. " +
                    "특정 루틴 ID가 불명확하면 먼저 getPersonalRoutines를 호출하여 JSON 목록을 받고, " +
                    "content가 일치하는 항목의 id를 찾아 deleteRoutine에 routineId로 전달하세요. 임의 추측 금지.\n\n" +
                    "의학적 조언은 제공하지 말고 일반 건강 정보와 루틴 관리에 집중하세요. 항상 한국어로 답하세요.";
}

