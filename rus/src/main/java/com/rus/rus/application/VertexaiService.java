package com.rus.rus.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionCall;
import com.google.cloud.vertexai.api.FunctionResponse;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto;
import com.rus.rus.controller.dto.req.RoutineUpdateRequestDto;
import com.rus.rus.controller.dto.res.PersonalRoutineResponseDto;
import com.rus.rus.controller.dto.res.RoutinePerformanceFeedbackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VertexaiService {

  private final GenerativeModel generativeModel;
  private final RoutineService routineService;
  private final ObjectMapper objectMapper; // DTO -> JSON 변환에 사용
  private final VertexAI vertexAI;
  private final Tool functionCallingTool;

  @org.springframework.beans.factory.annotation.Value("${gcp.gemini.model.name}")
  private String modelName;

  public String getChatResponse(String uid, List<ChatMessageDto> messages) throws IOException {

    // 루틴 목록 사전 조회
    PersonalRoutineResponseDto routinesDto = routineService.getPersonalRoutines(uid);
    String routinesJson = objectMapper.writeValueAsString(routinesDto);

    // 시스템 프롬프트에 루틴 목록 추가 + 추론/비노출 규칙 보강
    Content systemContent = generativeModel.getSystemInstruction().orElse(Content.newBuilder().build());
    String baseSystemPrompt = systemContent.getPartsList().get(0).getText();
    String dynamicSystemPrompt = baseSystemPrompt +
        "\n\n현재 사용자의 루틴 목록 (JSON 형식):\n" + routinesJson +
        "\n\n지침:" +
        "\n- 위 목록의 각 항목 `id`는 routineId에 해당합니다. 이 값은 도구 호출에만 사용하며, 사용자에게는 절대 노출/요구하지 마세요." +
        "\n- notification 필드는 존재하지 않는 필드로 취급하고 무시하세요." +
        "\n- 대화 맥락과 목록을 활용해 routineId 등 필요한 파라미터를 먼저 추론한 뒤, 바로 도구를 호출하세요. 사용자의 추가 확인을 요구하지 마세요." +
        "\n- 정말로 모호할 때만 콘텐츠 텍스트로 재질문하고, 숫자 ID는 언급하지 마세요.";

    Content dynamicSystemInstruction = Content.newBuilder()
        .addParts(Part.newBuilder().setText(dynamicSystemPrompt).build())
        .build();

    GenerativeModel dynamicModel = new GenerativeModel.Builder()
        .setModelName(modelName)
        .setVertexAi(vertexAI)
        .setTools(Arrays.asList(functionCallingTool))
        .setSystemInstruction(dynamicSystemInstruction)
        .build();

    ChatSession chatSession = dynamicModel.startChat();

    List<Content> history = new ArrayList<>();
    if (messages.size() > 1) {
      history = messages.stream()
          .limit(messages.size() - 1)
          .map(this::convertDtoToContent)
          .collect(Collectors.toList());
      chatSession.setHistory(history);
    }

    String currentUserMessage = messages.get(messages.size() - 1).getText();

    log.info("VertexAI 요청 전송: {}", currentUserMessage);
    GenerateContentResponse response = chatSession.sendMessage(currentUserMessage);
    log.info("VertexAI 응답 수신");

    // AI 응답의 *모든* Part를 확인
    List<Part> responseParts = response.getCandidates(0).getContent().getPartsList();
    List<Part> functionResponseParts = new ArrayList<>(); // Function Response들을 담을 리스트

    boolean hasFunctionCall = false;
    for (Part part : responseParts) {
      if (part.hasFunctionCall()) {
        hasFunctionCall = true;
        FunctionCall functionCall = part.getFunctionCall();
        log.info("Function Call 감지: {}", functionCall.getName());

        if (functionCall.getName().equals("addCustomRoutine")) {
          // 루틴 추가 함수 호출
          // Function Call 인자 파싱
          Map<String, Value> args = functionCall.getArgs().getFieldsMap();
          String content;
          int categoryId;
          try {
            content = args.get("content").getStringValue();
            categoryId = (int) args.get("categoryId").getNumberValue();
          } catch (Exception e) {
            log.error("FunctionCall 인자 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 에러 응답 Part 생성
            Struct.Builder errorStruct = Struct.newBuilder();
            errorStruct.putFields("status", Value.newBuilder().setStringValue("ERROR").build());
            errorStruct.putFields("message",
                Value.newBuilder().setStringValue("AI가 잘못된 함수 인자(content 또는 categoryId)를 전달했습니다.").build());
            functionResponseParts.add(Part.newBuilder()
                .setFunctionResponse(FunctionResponse.newBuilder()
                    .setName(functionCall.getName())
                    .setResponse(errorStruct.build())
                    .build())
                .build());
            continue; // 다음 Part 처리로 넘어감
          }

          // DTO 생성
          RoutineAddCustomRequestDto routineDto = new RoutineAddCustomRequestDto();
          routineDto.setContent(content);
          routineDto.setCategoryId(categoryId);

          // 실제 서비스 로직 호출
          try {
            log.info("RoutineService.addCustomRoutineToUser 호출. uid: {}, content: {}, categoryId: {}",
                uid, routineDto.getContent(), routineDto.getCategoryId());
            routineService.addCustomRoutineToUser(uid, routineDto);

            // 성공 응답 Part - ID 노출 금지
            Struct.Builder responseStruct = Struct.newBuilder();
            responseStruct.putFields("status", Value.newBuilder().setStringValue("SUCCESS").build());
            responseStruct.putFields("message",
                Value.newBuilder().setStringValue("루틴이 성공적으로 추가되었습니다.").build());
            functionResponseParts.add(Part.newBuilder()
                .setFunctionResponse(FunctionResponse.newBuilder()
                    .setName(functionCall.getName())
                    .setResponse(responseStruct.build())
                    .build())
                .build());

          } catch (Exception e) {
            log.error("루틴 추가 중 오류 발생: {}", e.getMessage());
            // 서비스 로직 실패 시 에러 응답 Part 생성 및 리스트에 추가 (민감 정보 제거)
            Struct.Builder errorStruct = Struct.newBuilder();
            errorStruct.putFields("status", Value.newBuilder().setStringValue("ERROR").build());
            errorStruct.putFields("message",
                Value.newBuilder().setStringValue("루틴 추가에 실패했습니다. 잠시 후 다시 시도해 주세요.").build());
            functionResponseParts.add(Part.newBuilder()
                .setFunctionResponse(FunctionResponse.newBuilder()
                    .setName(functionCall.getName())
                    .setResponse(errorStruct.build())
                    .build())
                .build());
          }
        } else if (functionCall.getName().equals("checkRoutineAsDone")) {
          // 루틴 달성 체크
          Map<String, Value> args = functionCall.getArgs().getFieldsMap();
          int routineId = 0; // 초기화
          try {
            routineId = (int) args.get("routineId").getNumberValue();
          } catch (Exception e) {
            log.error("FunctionCall 인자 파싱 실패 (checkRoutineAsDone): {}", e.getMessage());
            functionResponseParts
                .add(createErrorResponsePart(functionCall.getName(), "AI가 잘못된 함수 인자(routineId)를 전달했습니다."));
            continue;
          }
          try {
            log.info("RoutineService.checkRoutineAttainment 호출. uid: {}, routineId: {}", uid, routineId);
            // RoutineService의 checkRoutineAttainment 메서드 호출
            routineService.checkRoutineAttainment(uid, routineId); //
            functionResponseParts
                .add(createSuccessResponsePart(functionCall.getName(), "요청하신 루틴을 완료 처리했습니다."));
          } catch (Exception e) {
            log.error("루틴 달성 체크 중 오류 발생: {}", e.getMessage());
            functionResponseParts.add(createErrorResponsePart(functionCall.getName(),
                "루틴 완료 처리에 실패했습니다. 잠시 후 다시 시도해 주세요."));
          }
        } else if (functionCall.getName().equals("uncheckRoutine")) {
          // 루틴 체크 해제
          Map<String, Value> args = functionCall.getArgs().getFieldsMap();
          int routineId = 0; // 초기화
          try {
            routineId = (int) args.get("routineId").getNumberValue();
          } catch (Exception e) {
            log.error("FunctionCall 인자 파싱 실패 (uncheckRoutine): {}", e.getMessage());
            functionResponseParts
                .add(createErrorResponsePart(functionCall.getName(), "AI가 잘못된 함수 인자(routineId)를 전달했습니다."));
            continue;
          }
          try {
            log.info("RoutineService.uncheckRoutineAttainment 호출. uid: {}, routineId: {}", uid, routineId);
            // RoutineService의 uncheckRoutineAttainment 메서드 호출
            routineService.uncheckRoutineAttainment(uid, routineId); //
            functionResponseParts
                .add(createSuccessResponsePart(functionCall.getName(), "요청하신 루틴의 완료 체크를 해제했습니다."));
          } catch (Exception e) {
            log.error("루틴 체크 해제 중 오류 발생: {}", e.getMessage());
            functionResponseParts.add(createErrorResponsePart(functionCall.getName(),
                "루틴 체크 해제에 실패했습니다. 잠시 후 다시 시도해 주세요."));
          }
        } else if (functionCall.getName().equals("updateRoutine")) {
          // 루틴 수정
          Map<String, Value> args = functionCall.getArgs().getFieldsMap();
          int routineId = 0;
          String content = null;
          int categoryId = 0;
          try {
            routineId = (int) args.get("routineId").getNumberValue();
            content = args.get("content").getStringValue();
            categoryId = (int) args.get("categoryId").getNumberValue();
          } catch (Exception e) {
            log.error("FunctionCall 인자 파싱 실패 (updateRoutine): {}", e.getMessage());
            functionResponseParts
                .add(createErrorResponsePart(functionCall.getName(),
                    "AI가 잘못된 함수 인자(routineId, content, categoryId)를 전달했습니다."));
            continue;
          }
          try {
            log.info("RoutineService.updateRoutine 호출. uid: {}, routineId: {}, content: {}, categoryId: {}", uid,
                routineId, content, categoryId);
            // RoutineService의 updateRoutine 메서드 호출 (DTO 생성 필요)
            RoutineUpdateRequestDto updateDto = new RoutineUpdateRequestDto();
            updateDto.setContent(content);
            updateDto.setCategoryId(categoryId);
            routineService.updateRoutine(routineId, updateDto, uid);
            functionResponseParts
                .add(createSuccessResponsePart(functionCall.getName(), "루틴이 수정되었습니다."));
          } catch (Exception e) {
            log.error("루틴 수정 중 오류 발생: {}", e.getMessage());
            functionResponseParts.add(createErrorResponsePart(functionCall.getName(),
                "루틴 수정에 실패했습니다. 잠시 후 다시 시도해 주세요."));
          }
        } else if (functionCall.getName().equals("deleteRoutine")) {
          // 루틴 삭제
          Map<String, Value> args = functionCall.getArgs().getFieldsMap();
          int routineId = 0;
          try {
            routineId = (int) args.get("routineId").getNumberValue();
          } catch (Exception e) {
            log.error("FunctionCall 인자 파싱 실패 (deleteRoutine): {}", e.getMessage());
            functionResponseParts
                .add(createErrorResponsePart(functionCall.getName(), "AI가 잘못된 함수 인자(routineId)를 전달했습니다."));
            continue;
          }
          try {
            log.info("RoutineService.deleteRoutine 호출. uid: {}, routineId: {}", uid, routineId);
            // RoutineService의 deleteRoutine 메서드 호출
            routineService.deleteRoutine(routineId, uid);
            functionResponseParts
                .add(createSuccessResponsePart(functionCall.getName(), "루틴이 삭제되었습니다."));
          } catch (Exception e) {
            log.error("루틴 삭제 중 오류 발생: {}", e.getMessage());
            functionResponseParts.add(createErrorResponsePart(functionCall.getName(),
                "루틴 삭제에 실패했습니다. 잠시 후 다시 시도해 주세요."));
          }
        } else if (functionCall.getName().equals("getRoutinePerformanceFeedback")) {
          // 루틴 수행 피드백 조회
          try {
            log.info("RoutineService.getRoutinePerformanceFeedback 호출. uid: {}", uid);
            RoutinePerformanceFeedbackDto feedbackDto = routineService.getRoutinePerformanceFeedback(uid);
            String feedbackJson = objectMapper.writeValueAsString(feedbackDto);
            functionResponseParts.add(createSuccessResponsePart(functionCall.getName(), feedbackJson));
            log.info("루틴 수행 피드백 조회 성공");
          } catch (JsonProcessingException e) {
            log.error("루틴 수행 피드백 JSON 변환 중 오류 발생: {}", e.getMessage());
            functionResponseParts.add(createErrorResponsePart(functionCall.getName(), "루틴 수행 피드백을 처리하는 중 오류가 발생했습니다."));
          } catch (Exception e) {
            log.error("루틴 수행 피드백 조회 중 오류 발생: {}", e.getMessage());
            functionResponseParts
                .add(createErrorResponsePart(functionCall.getName(), "루틴 수행 피드백 조회에 실패했습니다. 잠시 후 다시 시도해 주세요."));
          }
        } else {
          // 알 수 없는 함수 호출 처리
          log.warn("알 수 없는 Function Call 요청: {}", functionCall.getName());
          Struct.Builder errorStruct = Struct.newBuilder();
          errorStruct.putFields("status", Value.newBuilder().setStringValue("UNKNOWN_FUNCTION").build());
          errorStruct.putFields("message",
              Value.newBuilder().setStringValue("알 수 없는 함수 '" + functionCall.getName() + "' 호출됨").build());
          functionResponseParts.add(Part.newBuilder()
              .setFunctionResponse(FunctionResponse.newBuilder()
                  .setName(functionCall.getName()) // 받은 이름 그대로 반환
                  .setResponse(errorStruct.build())
                  .build())
              .build());
        }
      }
      // Function Call이 아닌 다른 Part(예: 일반 텍스트)는 무시하고 다음 Part 확인
    }

    // Function Call이 하나라도 있었는지 확인
    if (hasFunctionCall) {
      // 수집된 모든 Function Response Part들을 담아서 AI에게 다시 전송
      Content functionResponsesContent = Content.newBuilder().addAllParts(functionResponseParts).build();
      log.info("{}개의 Function Response 전송", functionResponseParts.size());
      GenerateContentResponse finalResponse = chatSession.sendMessage(functionResponsesContent);
      // AI의 최종 텍스트 응답 반환
      return ResponseHandler.getText(finalResponse);
    } else {
      // Function Call이 없었다면, 원래 받은 응답에서 텍스트를 추출하여 반환
      log.info("일반 텍스트 응답 반환");
      return ResponseHandler.getText(response);
    }
  }

  /**
   * Function Calling 성공 응답 Part를 생성합니다.
   *
   * @param functionName 호출된 함수 이름
   * @param message      AI에게 전달할 결과 메시지 (텍스트 또는 JSON 문자열)
   * @return 생성된 Part 객체
   */
  private Part createSuccessResponsePart(String functionName, String message) {
    Struct.Builder responseStruct = Struct.newBuilder();
    responseStruct.putFields("status", Value.newBuilder().setStringValue("SUCCESS").build());
    responseStruct.putFields("result", Value.newBuilder().setStringValue(message).build());
    return Part.newBuilder()
        .setFunctionResponse(FunctionResponse.newBuilder()
            .setName(functionName)
            .setResponse(responseStruct.build())
            .build())
        .build();
  }

  /**
   * Function Calling 실패 응답 Part를 생성합니다. (기본 상태 코드 "ERROR")
   *
   * @param functionName 호출된 함수 이름
   * @param errorMessage AI에게 전달할 에러 메시지
   * @return 생성된 Part 객체
   */
  private Part createErrorResponsePart(String functionName, String errorMessage) {
    return createErrorResponsePart(functionName, errorMessage, "ERROR");
  }

  /**
   * Function Calling 실패 응답 Part를 생성합니다. (상태 코드 지정 가능)
   *
   * @param functionName 호출된 함수 이름
   * @param errorMessage AI에게 전달할 에러 메시지
   * @param statusCode   상태 코드 (예: "ERROR", "UNKNOWN_FUNCTION")
   * @return 생성된 Part 객체
   */
  private Part createErrorResponsePart(String functionName, String errorMessage, String statusCode) {
    Struct.Builder errorStruct = Struct.newBuilder();
    errorStruct.putFields("status", Value.newBuilder().setStringValue(statusCode).build());
    errorStruct.putFields("message", Value.newBuilder().setStringValue(errorMessage).build());
    return Part.newBuilder()
        .setFunctionResponse(FunctionResponse.newBuilder()
            .setName(functionName)
            .setResponse(errorStruct.build())
            .build())
        .build();
  }

  private Content convertDtoToContent(ChatMessageDto dto) {
    String role;
    if (dto.getRole().equalsIgnoreCase("MODEL")) {
      role = "model";
    } else {
      role = "user";
    }
    return Content.newBuilder()
        .setRole(role)
        .addParts(Part.newBuilder().setText(dto.getText()).build())
        .build();
  }
}
